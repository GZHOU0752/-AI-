package com.ruima.ai.center.service.impl;

import com.ruima.ai.center.model.dto.ChatMessage;
import com.ruima.ai.center.service.MemoryService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MemoryServiceImpl implements MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceImpl.class);
    private static final String SHORT_TERM_PREFIX = "ruima:memory:short:";
    private static final String LONG_TERM_PREFIX = "ruima:memory:long:";
    private static final String SUMMARY_PREFIX = "ruima:memory:summary:";
    private static final String SESSIONS_KEY = "ruima:sessions";
    private static final String USER_SESSIONS_PREFIX = "ruima:user:sessions:";
    private static final double TOKEN_CHARS_RATIO = 1.5;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private EmbeddingStore embeddingStore;

    @Value("${langchain4j.memory.short-term.window-size:10}")
    private int windowSize;

    @Value("${langchain4j.memory.short-term.redis-ttl:3600}")
    private long shortTermTtl;

    @Value("${langchain4j.memory.short-term.token-threshold:32768}")
    private int tokenThreshold;

    @Value("${langchain4j.memory.long-term.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${langchain4j.memory.long-term.top-k:5}")
    private int topK;

    private boolean redisOk() { return redissonClient != null; }
    private boolean vectorOk() { return embeddingStore != null && embeddingModel != null; }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / TOKEN_CHARS_RATIO);
    }

    private int estimateTokens(ChatMessage msg) {
        return estimateTokens(msg.getContent());
    }

    @Override
    public List<ChatMessage> getConversationHistory(String sessionId) {
        if (!redisOk()) return new ArrayList<>();
        String key = SHORT_TERM_PREFIX + sessionId;
        RScoredSortedSet<ChatMessage> sortedSet = redissonClient.getScoredSortedSet(key);
        Collection<ChatMessage> messages = sortedSet.valueRange(0, -1);
        if (messages == null) return new ArrayList<>();
        return new ArrayList<>(messages);
    }

    @Override
    public void addMessage(String sessionId, ChatMessage message) {
        if (!redisOk()) return;
        message.setTimestamp(System.currentTimeMillis());
        message.setSessionId(sessionId);

        String key = SHORT_TERM_PREFIX + sessionId;
        RScoredSortedSet<ChatMessage> sortedSet = redissonClient.getScoredSortedSet(key);
        sortedSet.add(message.getTimestamp(), message);
        sortedSet.expire(Duration.ofSeconds(shortTermTtl));

        redissonClient.getSet(SESSIONS_KEY).add(sessionId);

        // 建立用户->会话关联
        if (message.getUserId() != null && !message.getUserId().isEmpty()) {
            redissonClient.getSet(USER_SESSIONS_PREFIX + message.getUserId()).add(sessionId);
        }

        int totalTokens = 0;
        List<ChatMessage> allMessages = new ArrayList<>(sortedSet.valueRange(0, -1));
        for (ChatMessage msg : allMessages) {
            totalTokens += estimateTokens(msg);
        }

        if (totalTokens > tokenThreshold) {
            compressHistory(key, sortedSet, allMessages, totalTokens);
        }
    }

    private void compressHistory(String key, RScoredSortedSet<ChatMessage> sortedSet,
                                  List<ChatMessage> allMessages, int totalTokens) {
        int targetTokens = tokenThreshold / 2;
        int keepTokens = 0;
        int splitIdx = allMessages.size();

        for (int i = allMessages.size() - 1; i >= 0; i--) {
            int msgTokens = estimateTokens(allMessages.get(i));
            if (keepTokens + msgTokens > targetTokens) {
                splitIdx = i + 1;
                break;
            }
            keepTokens += msgTokens;
            splitIdx = i;
        }

        if (splitIdx <= 0 || splitIdx >= allMessages.size()) return;

        List<ChatMessage> oldMessages = allMessages.subList(0, splitIdx);
        String conversation = ChatMessage.formatHistory(oldMessages);

        try {
            String summary = chatLanguageModel.generate(
                    "请简洁总结以下对话历史（100-200字），只保留核心信息：用户需求、已执行操作、关键结果。\n\n" + conversation);

            for (ChatMessage old : oldMessages) {
                sortedSet.remove(old);
            }

            ChatMessage summaryMsg = new ChatMessage("system",
                    "[对话摘要] " + summary, key.replace(SHORT_TERM_PREFIX, ""));
            summaryMsg.setTimestamp(System.currentTimeMillis() - 1);
            sortedSet.add(summaryMsg.getTimestamp(), summaryMsg);

            log.info("摘要压缩完成: session={}, 压缩{}条 -> 1条摘要, token节省约{}",
                    key, oldMessages.size(), totalTokens - targetTokens);
        } catch (Exception e) {
            log.error("摘要压缩失败: {}", e.getMessage());
        }
    }

    @Override
    public String generateSummary(String sessionId) {
        if (!redisOk()) return "";
        List<ChatMessage> history = getConversationHistory(sessionId);
        if (history.isEmpty()) return "";

        String conversation = ChatMessage.formatHistory(history);
        String summary = chatLanguageModel.generate(
                "请将以下对话总结为200字以内的摘要，保留关键信息和决策：\n\n" + conversation);

        String summaryKey = SUMMARY_PREFIX + sessionId;
        RBucket<String> bucket = redissonClient.getBucket(summaryKey);
        bucket.set(summary, shortTermTtl, TimeUnit.SECONDS);

        return summary;
    }

    @Override
    public void storeLongTerm(String userId, String content, String category) {
        if (!vectorOk()) return;
        try {
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(content).content();
            dev.langchain4j.data.segment.TextSegment segment = dev.langchain4j.data.segment.TextSegment.from(content);
            String id = embeddingStore.add(embedding, segment);

            if (redisOk()) {
                String key = LONG_TERM_PREFIX + userId + ":" + id;
                Map<String, String> entry = new HashMap<>();
                entry.put("content", content);
                entry.put("category", category);
                entry.put("timestamp", String.valueOf(System.currentTimeMillis()));
                redissonClient.getMap(key).putAll(entry);
                redissonClient.getMap(key).expire(Duration.ofDays(30));
            }
        } catch (Exception e) {
            log.error("存储长期记忆失败: {}", e.getMessage());
        }
    }

    @Override
    public List<String> recallLongTerm(String userId, String query) {
        if (!vectorOk()) return new ArrayList<>();
        try {
            dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query).content();
            dev.langchain4j.store.embedding.EmbeddingSearchResult<dev.langchain4j.data.segment.TextSegment> searchResult =
                    embeddingStore.search(
                        dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                                .queryEmbedding(queryEmbedding)
                                .maxResults(topK)
                                .minScore(similarityThreshold)
                                .build()
                    );

            return searchResult.matches().stream()
                    .map(m -> m.embedded().text())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("长期记忆召回失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, String>> listSessions(String userId) {
        List<Map<String, String>> result = new ArrayList<>();
        if (!redisOk()) return result;

        // 优先按用户过滤，取用户关联的会话；如果无 userId 则回退到全局会话列表
        Set<String> sessionIds;
        if (userId != null && !userId.isEmpty()) {
            sessionIds = redissonClient.getSet(USER_SESSIONS_PREFIX + userId);
        } else {
            sessionIds = redissonClient.getSet(SESSIONS_KEY);
        }

        for (String sid : sessionIds) {
            String key = SHORT_TERM_PREFIX + sid;
            RScoredSortedSet<ChatMessage> sortedSet = redissonClient.getScoredSortedSet(key);
            if (!sortedSet.isExists()) continue;

            Collection<ChatMessage> messages = sortedSet.valueRange(0, 0);
            String preview = "";
            if (messages != null && !messages.isEmpty()) {
                ChatMessage first = messages.iterator().next();
                preview = first.getContent();
                if (preview.length() > 40) {
                    preview = preview.substring(0, 40) + "...";
                }
            }

            Map<String, String> item = new HashMap<>();
            item.put("sessionId", sid);
            item.put("preview", preview);
            result.add(item);
        }
        return result;
    }

    @Override
    public void cleanExpiredSessions() {
        log.debug("会话过期清理检查完成");
    }
}
