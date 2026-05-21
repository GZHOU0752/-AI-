package com.ruima.ai.center.service;

import com.ruima.ai.center.model.dto.ChatMessage;
import java.util.List;
import java.util.Map;

/**
 * 会话记忆服务 - 短期记忆 + 长期记忆
 */
public interface MemoryService {

    /**
     * 短期记忆 - 获取会话历史（滑动窗口）
     */
    List<ChatMessage> getConversationHistory(String sessionId);

    /**
     * 短期记忆 - 添加消息到会话
     */
    void addMessage(String sessionId, ChatMessage message);

    /**
     * 短期记忆 - 生成会话摘要
     */
    String generateSummary(String sessionId);

    /**
     * 长期记忆 - 存储知识条目
     */
    void storeLongTerm(String userId, String content, String category);

    /**
     * 长期记忆 - 语义相似度召回
     */
    List<String> recallLongTerm(String userId, String query);

    /**
     * 获取用户的所有会话列表（含摘要预览）
     */
    List<Map<String, String>> listSessions(String userId);

    /**
     * 长期记忆 - 清理过期会话
     */
    void cleanExpiredSessions();
}
