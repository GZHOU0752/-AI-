package com.ruima.ai.center.service;

import com.ruima.ai.center.model.dto.CoverageReport;
import com.ruima.ai.center.model.dto.TestGenerationRequest;

public interface UnitTestService {

    /**
     * AI生成单元测试代码
     */
    String generateTests(TestGenerationRequest request);

    /**
     * AI分析测试覆盖率
     */
    CoverageReport analyzeCoverage(String className, String sourceCode, String testCode);

    /**
     * 生成覆盖率报告
     */
    String generateCoverageReport(CoverageReport report);
}
