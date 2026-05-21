package com.ruima.ai.center.service;

import com.ruima.ai.center.model.dto.CodeReviewReport;
import com.ruima.ai.center.model.dto.CodeReviewRequest;

public interface AiCodeReviewService {

    /**
     * 执行AI代码评审
     * @param request 评审请求
     * @return 评审报告
     */
    CodeReviewReport review(CodeReviewRequest request);
}
