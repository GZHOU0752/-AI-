package com.ruima.ai.center.service;

import com.ruima.ai.center.model.dto.ChatMessage;
import com.ruima.ai.center.model.dto.KnowledgeDocument;
import java.util.List;
import java.util.Map;

/**
 * 团队智能问答知识库服务
 */
public interface KnowledgeBaseService {

    /**
     * 智能问答 - 结合短期记忆+长期记忆+RAG
     */
    String ask(String sessionId, String userId, String question);

    /**
     * 获取会话上下文
     */
    List<ChatMessage> getContext(String sessionId);

    /**
     * 上传知识文档
     */
    void uploadDocument(KnowledgeDocument document);

    /**
     * 搜索知识库
     */
    List<Map<String, Object>> searchKnowledge(String query);
}
