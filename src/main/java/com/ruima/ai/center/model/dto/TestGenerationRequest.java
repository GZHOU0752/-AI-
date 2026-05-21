package com.ruima.ai.center.model.dto;

import javax.validation.constraints.NotBlank;

public class TestGenerationRequest {

    @NotBlank(message = "目标类名不能为空")
    private String targetClassName;

    @NotBlank(message = "目标类源码不能为空")
    private String sourceCode;

    private String packageName;

    private TestType testType = TestType.UNIT;

    private String framework = "JUnit5";

    private boolean generateNestedTests = true;

    public String getTargetClassName() { return targetClassName; }
    public void setTargetClassName(String targetClassName) { this.targetClassName = targetClassName; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public TestType getTestType() { return testType; }
    public void setTestType(TestType testType) { this.testType = testType; }
    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }
    public boolean isGenerateNestedTests() { return generateNestedTests; }
    public void setGenerateNestedTests(boolean generateNestedTests) { this.generateNestedTests = generateNestedTests; }

    public enum TestType {
        UNIT, INTEGRATION
    }
}
