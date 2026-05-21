package com.ruima.ai.center.util;

import com.ruima.ai.center.model.dto.CodeReviewRequest;
import com.ruima.ai.center.model.enums.ReviewDimension;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Code Review 结构化提示词模板
 * 角色设定 + 三级问题分类 + 评审维度 + 模板化输出
 */
public class PromptTemplate {

    private static final String ROLE_SETTING =
            "## 评审角色定位\n\n" +
            "作为Java资深架构师/技术专家进行代码评审，分析代码变更并提供专业的评审意见。\n\n";

    private static final String REVIEW_PRINCIPLES =
            "## 评审原则\n\n" +
            "- **客观性**: 基于技术标准，避免主观偏见\n" +
            "- **建设性**: 提供具体可行的改进建议\n" +
            "- **教育性**: 解释问题原因和最佳实践\n" +
            "- **完整性**: 覆盖功能、性能、安全、可维护性等多个维度\n\n";

    private static final String PROBLEM_CLASSIFICATION =
            "## 问题分级\n\n" +
            "- Critical (必须修复): 安全漏洞、严重性能问题、数据一致性问题、线程安全问题、空指针\n" +
            "- Warning (建议修复): 代码质量问题、潜在性能隐患、可维护性问题\n" +
            "- Info (优化建议): 代码风格改进、最佳实践建议、架构优化建议、测试覆盖不足\n\n";

    private static final String REVIEW_DIMENSIONS =
            "## 评审维度检查清单\n\n" +
            "### 1. 代码质量\n" +
            "- 代码逻辑清晰，易于理解\n" +
            "- 遵循项目的编码规范和命名约定\n" +
            "- 方法长度适中，单一职责原则\n" +
            "- 没有明显的性能问题或算法缺陷\n" +
            "- 错误处理完整且得当\n\n" +
            "### 2. 安全性\n" +
            "- 输入验证完整，包括参数校验和边界检查\n" +
            "- 权限控制正确，遵循最小权限原则\n" +
            "- 敏感数据正确加密和脱敏\n" +
            "- SQL注入防护措施到位\n" +
            "- XSS和CSRF防护\n\n" +
            "### 3. 可维护性\n" +
            "- 类和方法职责单一，高内聚低耦合\n" +
            "- 适当的设计模式使用\n" +
            "- 合理的变量和方法命名\n" +
            "- 代码复用性好，避免重复代码\n\n" +
            "### 4. 架构设计\n" +
            "- 符合现有架构模式和分层设计\n" +
            "- 模块边界清晰，依赖关系合理\n" +
            "- 接口设计简洁明确\n\n" +
            "### 5. Java项目特定要求\n" +
            "- 遵循Spring框架最佳实践\n" +
            "- 正确使用注解和依赖注入\n" +
            "- 异常处理机制完善\n" +
            "- 线程安全考虑充分\n" +
            "- 空指针问题处理妥当\n\n" +
            "### 6. 数据库相关\n" +
            "- SQL语句性能优化，避免N+1查询\n" +
            "- 索引使用合理且有效\n" +
            "- 事务边界清晰\n\n" +
            "### 7. 测试相关\n" +
            "- 单元测试覆盖核心业务逻辑\n" +
            "- 测试用例设计合理\n\n" +
            "### 8. 性能考量\n" +
            "- 时间复杂度和空间复杂度分析\n" +
            "- 缓存使用策略合理\n" +
            "- 批量操作优化\n\n";

    private static final String OUTPUT_FORMAT =
            "## 评审输出格式\n\n" +
            "### 评审报告结构\n\n" +
            "**评审概览**\n" +
            "- 变更意图: [简要说明]\n" +
            "- 影响范围: [分析]\n" +
            "- 整体评分: [X/5分]\n\n" +
            "**Critical 问题 (必须修复)**\n" +
            "每个问题格式:\n" +
            "- 问题类型: Critical\n" +
            "- 位置: [文件名:行号]\n" +
            "- 问题描述: [具体问题说明]\n" +
            "- 影响: [潜在影响分析]\n" +
            "- 建议: [具体改进方案]\n\n" +
            "**Warning 问题 (建议修复)**\n" +
            "[同上格式]\n\n" +
            "**Info 优化建议**\n" +
            "[同上格式]\n\n" +
            "**总结**\n" +
            "[整体评价和改进方向]\n\n";

    private static final String RESPONSIBILITY_BOUNDARY =
            "## 评审职责界限\n\n" +
            "应该做的: 识别代码问题和风险、提供改进建议和最佳实践、解释技术原理\n" +
            "不应该做的: 直接修改或重写代码、执行任何代码变更操作、替代人工评审的最终确认\n\n";

    public static String buildCodeReviewPrompt(CodeReviewRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(ROLE_SETTING);
        prompt.append(REVIEW_PRINCIPLES);
        prompt.append(RESPONSIBILITY_BOUNDARY);
        prompt.append(PROBLEM_CLASSIFICATION);
        prompt.append(REVIEW_DIMENSIONS);
        prompt.append(OUTPUT_FORMAT);

        prompt.append("## 本次评审上下文\n\n");
        prompt.append("**PR/MR标题**: ").append(request.getTitle()).append("\n\n");
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            prompt.append("**变更描述**: ").append(request.getDescription()).append("\n\n");
        }
        if (request.getBranchName() != null) {
            prompt.append("**源分支**: ").append(request.getBranchName());
            prompt.append(" -> **目标分支**: ").append(request.getTargetBranch()).append("\n\n");
        }

        prompt.append("**关注的评审维度**: ");
        List<String> dimensions = request.getFocusDimensions();
        if (dimensions != null && !dimensions.isEmpty()) {
            prompt.append(String.join(", ", dimensions));
        } else {
            prompt.append("全部8项维度");
        }
        prompt.append("\n\n");

        prompt.append("## 代码变更内容\n\n");
        for (CodeReviewRequest.FileChange file : request.getFileChanges()) {
            prompt.append("### 文件: ").append(file.getFilePath())
                    .append(" [").append(file.getChangeType()).append("]\n\n");
            prompt.append("```java\n");
            String diffContent = file.getDiffContent();
            if (diffContent != null && diffContent.length() > 8000) {
                diffContent = diffContent.substring(0, 8000) + "\n... (内容已截断)";
            }
            prompt.append(diffContent != null ? diffContent : "无变更内容");
            prompt.append("\n```\n\n");
        }

        prompt.append("请按照上述结构化格式提供完整的评审报告。");
        return prompt.toString();
    }

    public static String buildUnitTestPrompt(String className, String sourceCode, String packageName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为专业的Java单元测试专家，精通JUnit 5 (Jupiter)、Mockito 4.5.1、Spring Boot Test 2.7.18。\n\n");
        prompt.append("请为以下Java类生成完整的单元测试代码：\n\n");
        prompt.append("**类名**: ").append(className).append("\n");
        if (packageName != null) {
            prompt.append("**包名**: ").append(packageName).append("\n");
        }
        prompt.append("**源码**:\n```java\n").append(sourceCode).append("\n```\n\n");
        prompt.append("## 测试要求\n\n");
        prompt.append("- 测试类命名: ").append(className).append("Test\n");
        prompt.append("- 使用 @ExtendWith(MockitoExtension.class)\n");
        prompt.append("- 遵循 Given-When-Then 结构\n");
        prompt.append("- 使用 @Nested 组织测试用例\n");
        prompt.append("- @DisplayName 使用中文描述\n");
        prompt.append("- 覆盖正常场景、异常场景、边界条件\n");
        prompt.append("- 对所有外部依赖使用 @Mock\n");
        prompt.append("- 充分的断言和Mock验证\n\n");
        prompt.append("请只输出测试代码，不要有多余解释。");
        return prompt.toString();
    }
}
