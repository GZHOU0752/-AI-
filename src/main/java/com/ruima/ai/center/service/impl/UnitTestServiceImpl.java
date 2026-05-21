package com.ruima.ai.center.service.impl;

import com.ruima.ai.center.model.dto.CoverageReport;
import com.ruima.ai.center.model.dto.TestGenerationRequest;
import com.ruima.ai.center.service.UnitTestService;
import com.ruima.ai.center.util.PromptTemplate;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UnitTestServiceImpl implements UnitTestService {

    private static final Logger log = LoggerFactory.getLogger(UnitTestServiceImpl.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Override
    public String generateTests(TestGenerationRequest request) {
        log.info("生成单元测试: {}", request.getTargetClassName());

        String prompt = PromptTemplate.buildUnitTestPrompt(
                request.getTargetClassName(),
                request.getSourceCode(),
                request.getPackageName()
        );

        String generatedCode = chatLanguageModel.generate(prompt);

        // 清理输出，提取纯Java代码
        generatedCode = extractJavaCode(generatedCode);

        log.info("单元测试生成完成, 代码长度: {}", generatedCode.length());
        return generatedCode;
    }

    @Override
    public CoverageReport analyzeCoverage(String className, String sourceCode, String testCode) {
        log.info("分析测试覆盖率: {}", className);

        String analysisPrompt = buildCoverageAnalysisPrompt(className, sourceCode, testCode);
        String aiAnalysis = chatLanguageModel.generate(analysisPrompt);

        CoverageReport report = parseCoverageReport(aiAnalysis);
        report.setClassName(className);

        return report;
    }

    @Override
    public String generateCoverageReport(CoverageReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(report.getClassName()).append(" 单元测试覆盖率分析报告\n\n");

        sb.append("## 1. 覆盖率统计\n\n");
        sb.append("- 行覆盖率: ").append(formatPercent(report.getLineCoverage())).append(" (目标>=80%)\n");
        sb.append("- 分支覆盖率: ").append(formatPercent(report.getBranchCoverage())).append(" (目标>=75%)\n");
        sb.append("- 方法覆盖率: ").append(formatPercent(report.getMethodCoverage())).append(" (目标100%)\n");
        sb.append("- 异常覆盖率: ").append(formatPercent(report.getExceptionCoverage())).append(" (目标100%)\n\n");
        sb.append("**综合评价**: ").append(report.getOverallRating()).append("\n\n");

        // 已覆盖场景
        sb.append("## 2. 已覆盖场景\n\n");
        if (report.getCoveredScenarios() != null) {
            for (CoverageReport.CoverageScenario cs : report.getCoveredScenarios()) {
                sb.append("- ").append(cs.getCategory()).append(": ").append(cs.getDescription()).append("\n");
            }
        }
        sb.append("\n");

        // 未覆盖场景
        sb.append("## 3. 未覆盖场景识别\n\n");
        if (report.getUncoveredScenarios() != null) {
            for (CoverageReport.UncoveredScenario us : report.getUncoveredScenarios()) {
                String icon = "red".equals(us.getPriority()) ? "red_circle" :
                        "yellow".equals(us.getPriority()) ? "yellow_circle" : "blue_circle";
                sb.append("### ").append(icon).append(" ").append(us.getDescription()).append("\n\n");
                sb.append("- **风险**: ").append(us.getRisk()).append("\n");
                sb.append("- **影响**: ").append(us.getImpact()).append("\n");
                sb.append("- **建议**: ").append(us.getSuggestion()).append("\n\n");
            }
        }

        // 风险评估
        sb.append("## 8. 风险评估\n\n");
        sb.append("- **生产风险等级**: ").append(report.getRiskLevel()).append("\n");
        sb.append("- **建议修复时间**: ").append(report.getSuggestedFixTime()).append("\n");

        return sb.toString();
    }

    private String buildCoverageAnalysisPrompt(String className, String sourceCode, String testCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为专业的Java单元测试覆盖率分析专家，请分析以下代码的测试覆盖情况，并按指定格式输出分析报告。\n\n");
        prompt.append("## 目标类源码\n```java\n").append(sourceCode).append("\n```\n\n");
        prompt.append("## 现有测试代码\n```java\n").append(testCode).append("\n```\n\n");
        prompt.append("## 分析要求\n");
        prompt.append("1. 分析行覆盖率、分支覆盖率、方法覆盖率、异常覆盖率\n");
        prompt.append("2. 识别已覆盖和未覆盖的场景\n");
        prompt.append("3. 按优先级(HIGH/MEDIUM/LOW)列出未覆盖的场景\n");
        prompt.append("4. 提供具体的测试用例建议\n");
        prompt.append("5. 评估生产风险等级\n\n");
        prompt.append("请用以下JSON格式输出结果（只输出JSON，不要有其他内容）：\n");
        prompt.append("{\n");
        prompt.append("  \"lineCoverage\": 0.0,\n");
        prompt.append("  \"branchCoverage\": 0.0,\n");
        prompt.append("  \"methodCoverage\": 0.0,\n");
        prompt.append("  \"exceptionCoverage\": 0.0,\n");
        prompt.append("  \"overallRating\": \"\",\n");
        prompt.append("  \"riskLevel\": \"\"\n");
        prompt.append("}");
        return prompt.toString();
    }

    private CoverageReport parseCoverageReport(String aiOutput) {
        CoverageReport report = new CoverageReport();

        try {
            // 提取百分比数值
            report.setLineCoverage(extractPercentage(aiOutput, "lineCoverage|行覆盖"));
            report.setBranchCoverage(extractPercentage(aiOutput, "branchCoverage|分支覆盖"));
            report.setMethodCoverage(extractPercentage(aiOutput, "methodCoverage|方法覆盖"));
            report.setExceptionCoverage(extractPercentage(aiOutput, "exceptionCoverage|异常覆盖"));

            if (report.getLineCoverage() >= 85) {
                report.setOverallRating("优秀");
            } else if (report.getLineCoverage() >= 70) {
                report.setOverallRating("良好");
            } else if (report.getLineCoverage() >= 50) {
                report.setOverallRating("待改进");
            } else {
                report.setOverallRating("不合格");
            }

            if (report.getLineCoverage() < 70) {
                report.setRiskLevel("高");
                report.setSuggestedFixTime("立即");
            } else if (report.getLineCoverage() < 85) {
                report.setRiskLevel("中");
                report.setSuggestedFixTime("本周");
            } else {
                report.setRiskLevel("低");
                report.setSuggestedFixTime("本月");
            }
        } catch (Exception e) {
            log.error("解析覆盖率报告失败: {}", e.getMessage());
        }

        return report;
    }

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```java\\s*\\n([\\s\\S]*?)```");
    private static final Pattern COVERAGE_PATTERN = Pattern.compile("(?:lineCoverage|branchCoverage|methodCoverage|exceptionCoverage|行覆盖|分支覆盖|方法覆盖|异常覆盖)[:：]\\s*([\\d.]+)(%?)");

    private double extractPercentage(String text, String patternName) {
        Matcher m = COVERAGE_PATTERN.matcher(text);
        while (m.find()) {
            double val = Double.parseDouble(m.group(1));
            return val > 1.0 ? val / 100.0 : val;
        }
        return 0.0;
    }

    private String extractJavaCode(String aiOutput) {
        Matcher m = CODE_BLOCK_PATTERN.matcher(aiOutput);
        if (m.find()) {
            return m.group(1).trim();
        }
        return aiOutput.trim();
    }

    private String formatPercent(double val) {
        return String.format("%.1f%%", val * 100);
    }
}
