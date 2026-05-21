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
        report.setFormattedReport(formatReport(report));

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

        report.setOverallScore(extractDouble(aiResponse, SCORE_PATTERN, 3.0));
        report.setChangeIntent(extractField(aiResponse, INTENT_PATTERN));
        report.setImpactScope(extractField(aiResponse, SCOPE_PATTERN));

        String[] sections = aiResponse.split("(?=(?:Critical|Warning|Info)\\s)");
        for (String section : sections) {
            IssueSeverity severity = IssueSeverity.valueOf(TYPE_PATTERN.matcher(section).find()
                    ? TYPE_PATTERN.matcher(section).group(1).toUpperCase() : "INFO");

            List<CodeReviewIssue> targetList;
            switch (severity) {
                case CRITICAL: targetList = criticalIssues; break;
                case WARNING:  targetList = warningIssues;  break;
                default:       targetList = infoIssues;     break;
            }

            targetList.addAll(parseIssuesFromSection(section));
        }

        report.setCriticalIssues(criticalIssues);
        report.setWarningIssues(warningIssues);
        report.setInfoIssues(infoIssues);
        report.setSummary(String.format("共发现 %d 个Critical问题、%d 个Warning问题、%d 个Info建议",
                criticalIssues.size(), warningIssues.size(), infoIssues.size()));

        return report;
    }

    private List<CodeReviewIssue> parseIssuesFromSection(String section) {
        List<CodeReviewIssue> issues = new ArrayList<>();
        String[] issueBlocks = section.split("(?=问题类型[:：])");

        for (String block : issueBlocks) {
            if (!block.contains("问题类型")) continue;

            CodeReviewIssue issue = new CodeReviewIssue();
            issue.setSeverity(extractSeverity(block));
            issue.setFilePath(extractField(block, LOCATION_PATTERN, 1));
            issue.setLineNumber(extractInt(block, LOCATION_PATTERN, 2));
            issue.setDescription(extractField(block, DESC_PATTERN));
            issue.setImpact(extractField(block, IMPACT_PATTERN));
            issue.setSuggestion(extractField(block, SUGGEST_PATTERN));
            issue.setDimension(inferDimension(issue.getDescription()));

            if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
                issues.add(issue);
            }
        }

        return issues;
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
        Matcher m = TYPE_PATTERN.matcher(text);
        if (m.find()) {
            return IssueSeverity.valueOf(m.group(1).toUpperCase());
        }
        return IssueSeverity.INFO;
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

    private String formatReport(CodeReviewReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 代码评审报告\n\n");
        sb.append("## 评审概览\n\n");
        sb.append("- **变更意图**: ").append(Objects.toString(report.getChangeIntent(), "")).append("\n");
        sb.append("- **影响范围**: ").append(Objects.toString(report.getImpactScope(), "")).append("\n");
        sb.append("- **整体评分**: ").append(report.getOverallScore()).append("/5分\n\n");

        appendIssueSection(sb, "Critical 问题 (必须修复)", "red_circle", report.getCriticalIssues());
        appendIssueSection(sb, "Warning 问题 (建议修复)", "yellow_circle", report.getWarningIssues());
        appendIssueSection(sb, "Info 优化建议", "blue_circle", report.getInfoIssues());

        sb.append("---\n\n");
        sb.append("**评审时间**: ").append(report.getReviewTime()).append("\n");
        sb.append("**报告ID**: ").append(report.getId()).append("\n");

        return sb.toString();
    }

    private void appendIssueSection(StringBuilder sb, String title, String icon, List<CodeReviewIssue> issues) {
        sb.append("## ").append(icon).append(" ").append(title).append("\n\n");

        if (issues == null || issues.isEmpty()) {
            sb.append("> 无\n\n");
            return;
        }

        for (int i = 0; i < issues.size(); i++) {
            CodeReviewIssue issue = issues.get(i);
            sb.append("### ").append(i + 1).append(". ").append(Objects.toString(issue.getTitle(), "")).append("\n\n");
            sb.append("| 属性 | 内容 |\n|------|------|\n");
            sb.append("| 位置 | ").append(Objects.toString(issue.getFilePath(), ""))
                    .append(":").append(issue.getLineNumber()).append(" |\n");
            sb.append("| 维度 | ").append(issue.getDimension() != null ? issue.getDimension().getDisplayName() : "-").append(" |\n");
            sb.append("| 描述 | ").append(Objects.toString(issue.getDescription(), "")).append(" |\n");
            sb.append("| 影响 | ").append(Objects.toString(issue.getImpact(), "")).append(" |\n");
            sb.append("| 建议 | ").append(Objects.toString(issue.getSuggestion(), "")).append(" |\n\n");
        }
    }
}
