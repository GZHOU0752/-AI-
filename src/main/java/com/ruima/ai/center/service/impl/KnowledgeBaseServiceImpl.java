package com.ruima.ai.center.service.impl;

import com.ruima.ai.center.model.dto.ChatMessage;
import com.ruima.ai.center.model.dto.KnowledgeDocument;
import com.ruima.ai.center.service.KnowledgeBaseService;
import com.ruima.ai.center.service.MemoryService;
import com.ruima.ai.center.service.RagService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private RagService ragService;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Override
    public String ask(String sessionId, String userId, String question) {
        log.info("智能问答: sessionId={}, userId={}, question={}", sessionId, userId, question);

        // 1. 获取短期记忆（会话上下文）
        List<ChatMessage> history = memoryService.getConversationHistory(sessionId);

        // 2. 获取长期记忆（个性化知识）
        List<String> longTermMemories = memoryService.recallLongTerm(userId, question);

        // 3. RAG检索
        List<KnowledgeDocument.DocumentChunk> ragChunks = ragService.search(question, 5);

        // 4. 构建综合上下文并生成回答
        StringBuilder contextPrompt = new StringBuilder();
        contextPrompt.append("你是一个团队智能问答助手。请基于以下上下文回答用户问题。\n\n");

        // 短期记忆上下文
        if (!history.isEmpty()) {
            contextPrompt.append("### 对话历史\n");
            contextPrompt.append(ChatMessage.formatHistory(history)).append("\n");
        }

        // 长期记忆上下文
        if (!longTermMemories.isEmpty()) {
            contextPrompt.append("### 用户相关知识\n");
            for (String mem : longTermMemories) {
                contextPrompt.append("- ").append(mem).append("\n");
            }
            contextPrompt.append("\n");
        }

        // RAG检索到的知识
        if (!ragChunks.isEmpty()) {
            contextPrompt.append("### 知识库相关内容\n");
            for (int i = 0; i < ragChunks.size(); i++) {
                contextPrompt.append("【").append(i + 1).append("】")
                        .append(ragChunks.get(i).getContent()).append("\n");
            }
            contextPrompt.append("\n");
        }

        contextPrompt.append("### 当前问题\n").append(question).append("\n\n");
        contextPrompt.append("请综合上述信息，给出专业、准确的回答。");

        String answer = chatLanguageModel.generate(contextPrompt.toString());

        // 记录对话到短期记忆（关联用户）
        ChatMessage userMsg = new ChatMessage("user", question, sessionId, userId);
        memoryService.addMessage(sessionId, userMsg);
        ChatMessage aiMsg = new ChatMessage("assistant", answer, sessionId, userId);
        memoryService.addMessage(sessionId, aiMsg);

        // 异步存储长期记忆（重要问答）
        memoryService.storeLongTerm(userId,
                "Q: " + question + " A: " + answer.substring(0, Math.min(200, answer.length())),
                "qa_history");

        return answer;
    }

    @Override
    public List<ChatMessage> getContext(String sessionId) {
        return memoryService.getConversationHistory(sessionId);
    }

    @Override
    public void uploadDocument(KnowledgeDocument document) {
        ragService.ingestDocument(document);

        // 同时记录到长期记忆
        if (document.getUploadedBy() != null) {
            memoryService.storeLongTerm(document.getUploadedBy(),
                    "已上传文档: " + document.getTitle(),
                    "document_upload");
        }

        log.info("文档上传并入库完成: {}", document.getTitle());
    }

    @Override
    public List<Map<String, Object>> searchKnowledge(String query) {
        List<KnowledgeDocument.DocumentChunk> chunks = ragService.search(query, 5);
        List<Map<String, Object>> results = new ArrayList<>();

        for (KnowledgeDocument.DocumentChunk chunk : chunks) {
            Map<String, Object> item = new HashMap<>();
            item.put("chunkId", chunk.getChunkId());
            item.put("content", chunk.getContent());
            item.put("score", 1.0); // 简化的分数
            results.add(item);
        }

        return results;
    }
}
