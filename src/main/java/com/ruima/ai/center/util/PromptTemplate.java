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

    private static final String CODE_REVIEW_SYSTEM =
            "你是资深 Java 代码审查专家。逐行分析代码，按以下标准分类输出，不要遗漏任何问题。\n\n" +
            "## Critical（必须修复，以下任一情况即报 Critical）\n\n" +
            "- 空指针: 变量赋值 null 后未判空直接调用方法/访问字段；数组/集合/方法返回值未判空即使用\n" +
            "- 安全漏洞: SQL 注入；XSS/CSRF；敏感信息未加密/脱敏；权限校验缺失；任意文件读取\n" +
            "- 数据安全: 共享变量无同步(线程安全)；数据库事务边界错误导致数据不一致\n" +
            "- 资源泄漏: IO流/数据库连接/网络连接未在 finally 或 try-with-resources 中关闭\n" +
            "- 严重性能: OOM 风险(大对象/无限缓存)；死锁/活锁；SQL 全表扫描无索引\n\n" +
            "## Warning（建议修复，以下任一情况即报 Warning）\n\n" +
            "- 异常处理: 空 catch 块；catch Exception 仅 printStackTrace；finally 中有 return\n" +
            "- 代码质量: 方法>50行；圈复杂度>10；重复代码>5行；硬编码配置值/魔法数字\n" +
            "- 可维护性: 类职责不单一；接口设计不合理；缺少必要注释；命名不规范\n" +
            "- 性能隐患: N+1 查询；缓存策略不当；不必要的对象创建；String 循环拼接\n" +
            "- 架构问题: 循环依赖；违反分层原则；过度耦合\n" +
            "- 数据库: 缺少必要索引；事务范围过大/过小；连接池配置不当\n" +
            "- 测试: 核心逻辑缺少单元测试；测试仅覆盖 happy path\n\n" +
            "## Info（优化建议，以下情况报 Info）\n\n" +
            "- 代码风格: 可读性优化；lambda/Stream 可简化；Optional 可用\n" +
            "- 设计优化: 设计模式建议；架构微调\n" +
            "- 最佳实践: Java 8+ 新特性建议；Spring Boot 最佳实践\n\n" +
            "## 输出格式（每个问题严格按此模板，问题之间空行分隔）\n\n" +
            "问题类型: Critical\n" +
            "位置: File.java:42\n" +
            "问题描述: xxx 变量赋值为 null 后在第 42 行直接调用了 length() 方法\n" +
            "影响: 运行时抛出 NullPointerException，导致当前请求失败\n" +
            "建议: 在调用前增加 null 判断，或使用 Optional.ofNullable(xxx).map(String::length).orElse(0)\n\n" +
            "（每个问题以 \"问题类型:\" 开头，按上述格式输出）\n\n" +
            "## 开头输出\n\n" +
            "整体评分: 2\n" +
            "变更意图: 新增空指针示例代码\n" +
            "影响范围: 当前文件运行时安全\n\n" +
            "## 结尾输出\n\n" +
            "总结: 共发现 X 个问题，建议优先修复 Critical 级别的空指针风险\n\n" +
            "注意: 必须逐行检查，对每一处潜在的 null 调用、资源未关闭、异常未处理都必须报告。不要遗漏。";

    public static String buildCodeReviewPrompt(CodeReviewRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(CODE_REVIEW_SYSTEM);
        sb.append("\n## 待审查代码\n\n");

        for (CodeReviewRequest.FileChange file : request.getFileChanges()) {
            sb.append("### ").append(file.getFilePath()).append("\n```java\n");
            String content = file.getDiffContent();
            if (content != null && content.length() > 10000) {
                content = content.substring(0, 10000) + "\n// 内容已截断";
            }
            sb.append(content != null ? content : "").append("\n```\n\n");
        }

        sb.append("请逐行检查上述代码，严格按输出格式返回完整的评审结果。");
        return sb.toString();
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
