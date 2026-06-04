package com.ruima.ai.center.service.impl;

import com.ruima.ai.center.model.dto.CodeReviewIssue;
import com.ruima.ai.center.model.dto.CodeReviewReport;
import com.ruima.ai.center.model.dto.CodeReviewRequest;
import com.ruima.ai.center.model.enums.IssueSeverity;
import com.ruima.ai.center.model.enums.ReviewDimension;
import com.ruima.ai.center.service.AiCodeReviewService;
import com.ruima.ai.center.util.PromptTemplate;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiCodeReviewServiceImpl implements AiCodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiCodeReviewServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern SCORE_PATTERN = Pattern.compile("整体评分[:：]\\s*([\\d.]+)");
    private static final Pattern INTENT_PATTERN = Pattern.compile("变更意图[:：]\\s*(.+?)(?:\\n|$)");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("影响范围[:：]\\s*(.+?)(?:\\n|$)");
    private static final Pattern TYPE_PATTERN = Pattern.compile("问题类型[:：]\\s*(Critical|Warning|Info)");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("位置[:：]\\s*(.+?):(\\d+)");
    private static final Pattern DESC_PATTERN = Pattern.compile("问题描述[:：]\\s*(.+?)(?:\\n|$)");
    private static final Pattern IMPACT_PATTERN = Pattern.compile("影响[:：]\\s*(.+?)(?:\\n|$)");
    private static final Pattern SUGGEST_PATTERN = Pattern.compile("建议[:：]\\s*(.+?)(?:\\n|$)");

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Override
    public CodeReviewReport review(CodeReviewRequest request) {
        log.info("开始AI代码评审: {}", request.getTitle());

        String prompt = PromptTemplate.buildCodeReviewPrompt(request);
        String aiResponse;
        try {
            aiResponse = chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }

        CodeReviewReport report = parseAiResponse(aiResponse);
        report.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        report.setTitle(request.getTitle());
        report.setReviewTime(LocalDateTime.now().format(DATE_FMT));

        log.info("AI代码评审完成, 报告ID: {}, Critical: {}, Warning: {}, Info: {}",
                report.getId(),
                report.getCriticalIssues().size(),
                report.getWarningIssues().size(),
                report.getInfoIssues().size());

        return report;
    }

    private CodeReviewReport parseAiResponse(String aiResponse) {
        CodeReviewReport report = new CodeReviewReport();
        List<CodeReviewIssue> criticalIssues = new ArrayList<>();
        List<CodeReviewIssue> warningIssues = new ArrayList<>();
        List<CodeReviewIssue> infoIssues = new ArrayList<>();

        // 解析概览信息
        report.setOverallScore(extractDouble(aiResponse, SCORE_PATTERN, 3.0));
        report.setChangeIntent(extractField(aiResponse, INTENT_PATTERN));
        report.setImpactScope(extractField(aiResponse, SCOPE_PATTERN));

        // 按"问题类型:"分割，每个 block 是一个独立问题
        String[] blocks = aiResponse.split("问题类型[:：]\\s*");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            CodeReviewIssue issue = new CodeReviewIssue();
            issue.setSeverity(extractSeverity(block));
            issue.setFilePath(extractField(block, LOCATION_PATTERN, 1));
            issue.setLineNumber(extractInt(block, LOCATION_PATTERN, 2));
            issue.setDescription(extractField(block, DESC_PATTERN));
            issue.setImpact(extractField(block, IMPACT_PATTERN));
            issue.setSuggestion(extractField(block, SUGGEST_PATTERN));
            issue.setDimension(inferDimension(issue.getDescription()));

            if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
                // 根据关键词自动升级严重级别（兜底模型分类不准）
                issue.setSeverity(upgradeSeverity(issue));
                switch (issue.getSeverity()) {
                    case CRITICAL: criticalIssues.add(issue); break;
                    case WARNING:  warningIssues.add(issue);  break;
                    default:       infoIssues.add(issue);     break;
                }
            }
        }

        // 解析总结
        Pattern summaryPattern = Pattern.compile("总结[:：]\\s*(.+)");
        Matcher sm = summaryPattern.matcher(aiResponse);
        if (sm.find()) {
            report.setSummary(sm.group(1).trim());
        } else {
            report.setSummary(String.format("共发现 %d 个Critical问题、%d 个Warning问题、%d 个Info建议",
                    criticalIssues.size(), warningIssues.size(), infoIssues.size()));
        }

        report.setCriticalIssues(criticalIssues);
        report.setWarningIssues(warningIssues);
        report.setInfoIssues(infoIssues);
        return report;
    }

    private static String extractField(String text, Pattern pattern) {
        return extractField(text, pattern, 1);
    }

    private static String extractField(String text, Pattern pattern, int group) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String value = m.group(group);
            return value != null ? value.trim() : "";
        }
        return "";
    }

    private static double extractDouble(String text, Pattern pattern, double defaultValue) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static int extractInt(String text, Pattern pattern, int group) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(group));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static IssueSeverity extractSeverity(String text) {
        String trimmed = text.trim();
        for (IssueSeverity s : IssueSeverity.values()) {
            if (trimmed.startsWith(s.name())) return s;
        }
        return IssueSeverity.INFO;
    }

    /**
     * 根据问题描述关键词自动升级严重级别，兜底 LLM 分类不准的问题。
     */
    private static IssueSeverity upgradeSeverity(CodeReviewIssue issue) {
        String desc = (issue.getDescription() != null ? issue.getDescription() : "").toLowerCase();
        String impact = (issue.getImpact() != null ? issue.getImpact() : "").toLowerCase();

        // 已经是 Critical 则无需升级
        if (issue.getSeverity() == IssueSeverity.CRITICAL) return IssueSeverity.CRITICAL;

        // 关键词 → 强制升级到 Critical
        boolean isCritical = desc.contains("空指针") || desc.contains("null") && (desc.contains("调用") || desc.contains("访问") || desc.contains("方法") || desc.contains("属性"))
                || impact.contains("nullpointer") || impact.contains("npe")
                || desc.contains("sql注入") || desc.contains("sql 注入")
                || desc.contains("线程安全") || desc.contains("死锁") || desc.contains("数据不一致")
                || desc.contains("资源泄漏") || desc.contains("资源泄露") || desc.contains("未关闭")
                || desc.contains("安全漏洞") || desc.contains("任意文件")
                || desc.contains("oom") || desc.contains("内存溢出");

        if (isCritical) return IssueSeverity.CRITICAL;

        // 已 Warning 的保持不变，Info 的看是否够 Warning
        if (issue.getSeverity() == IssueSeverity.WARNING) return IssueSeverity.WARNING;

        boolean isWarning = desc.contains("异常") || desc.contains("catch") || desc.contains("exception")
                || desc.contains("n+1") || desc.contains("索引")
                || desc.contains("硬编码") || desc.contains("魔法数字")
                || desc.contains("重复") || desc.contains("过长")
                || desc.contains("命名不") || desc.contains("不规范");

        if (isWarning) return IssueSeverity.WARNING;

        return issue.getSeverity();
    }

    private ReviewDimension inferDimension(String description) {
        if (description == null) return ReviewDimension.CODE_QUALITY;

        String lower = description.toLowerCase();
        if (lower.contains("安全") || lower.contains("注入") || lower.contains("xss") || lower.contains("csrf")
                || lower.contains("权限") || lower.contains("脱敏") || lower.contains("加密")) {
            return ReviewDimension.SECURITY;
        }
        if (lower.contains("n+1") || lower.contains("sql") || lower.contains("索引") || lower.contains("事务") || lower.contains("连接池")) {
            return ReviewDimension.DATABASE;
        }
        if (lower.contains("复杂度") || lower.contains("缓存") || lower.contains("异步") || lower.contains("批量") || lower.contains("oom")) {
            return ReviewDimension.PERFORMANCE;
        }
        if (lower.contains("空指针") || lower.contains("npe") || lower.contains("线程") || lower.contains("并发")
                || lower.contains("spring") || lower.contains("注解")) {
            return ReviewDimension.JAVA_SPECIFIC;
        }
        if (lower.contains("架构") || lower.contains("分层") || lower.contains("接口") || lower.contains("模块")) {
            return ReviewDimension.ARCHITECTURE;
        }
        if (lower.contains("测试") || lower.contains("覆盖率") || lower.contains("mock")) {
            return ReviewDimension.TESTING;
        }
        if (lower.contains("维护") || lower.contains("耦合") || lower.contains("设计模式") || lower.contains("复用")) {
            return ReviewDimension.MAINTAINABILITY;
        }
        return ReviewDimension.CODE_QUALITY;
    }
}
