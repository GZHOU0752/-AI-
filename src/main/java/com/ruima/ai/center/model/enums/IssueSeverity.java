package com.ruima.ai.center.model.enums;

public enum IssueSeverity {

    CRITICAL("Critical", "必须修复 - 安全漏洞、严重性能问题、数据一致性、线程安全、空指针"),
    WARNING("Warning", "建议修复 - 代码质量问题、潜在性能隐患、可维护性问题"),
    INFO("Info", "优化建议 - 代码风格改进、最佳实践建议、架构优化建议、测试覆盖不足");

    private final String label;
    private final String description;

    IssueSeverity(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
