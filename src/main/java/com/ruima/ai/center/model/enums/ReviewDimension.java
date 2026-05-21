package com.ruima.ai.center.model.enums;

public enum ReviewDimension {

    CODE_QUALITY("代码质量", "代码逻辑清晰、命名规范、方法职责单一"),
    SECURITY("安全性", "输入验证、权限控制、敏感数据保护、注入防护"),
    MAINTAINABILITY("可维护性", "高内聚低耦合、设计模式、代码复用、测试覆盖"),
    ARCHITECTURE("架构设计", "分层设计、模块边界、接口设计、配置管理"),
    JAVA_SPECIFIC("Java特定", "Spring实践、异常处理、线程安全、空指针防护、Java8+特性"),
    DATABASE("数据库相关", "SQL优化、索引使用、事务边界、数据一致性"),
    TESTING("测试相关", "单测覆盖、测试设计、Mock使用、集成测试"),
    PERFORMANCE("性能考量", "时空复杂度、缓存策略、批量操作、异步处理");

    private final String displayName;
    private final String description;

    ReviewDimension(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
