package com.ruima.ai.center.service.impl;

import com.ruima.ai.center.model.dto.KnowledgeDocument;
import com.ruima.ai.center.service.RagService;
import com.ruima.ai.center.util.TextChunker;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private EmbeddingStore embeddingStore;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Value("${rag.chunking.max-chunk-size:1000}")
    private int maxChunkSize;

    @Value("${rag.chunking.chunk-overlap:200}")
    private int chunkOverlap;

    @Value("${rag.retrieval.top-k:5}")
    private int retrievalTopK;

    @Value("${rag.retrieval.min-score:0.7}")
    private double minScore;

    @Value("${rag.retrieval.hybrid-search-weight-bm25:0.3}")
    private double bm25Weight;

    @Value("${rag.retrieval.hybrid-search-weight-vector:0.7}")
    private double vectorWeight;

    private TextChunker textChunker;

    @javax.annotation.PostConstruct
    void initTextChunker() {
        this.textChunker = new TextChunker(maxChunkSize, chunkOverlap);
    }

    @Override
    public void ingestDocument(KnowledgeDocument document) {
        if (embeddingStore == null) {
            log.warn("向量存储不可用, 文档入库跳过: {}", document.getTitle());
            return;
        }
        log.info("文档入库: {}", document.getTitle());

        // 语义分块
        List<String> chunks = textChunker.chunk(document.getContent());
        document.setChunks(new ArrayList<>());

        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeDocument.DocumentChunk chunk = new KnowledgeDocument.DocumentChunk();
            chunk.setChunkId(document.getId() + "_chunk_" + i);
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setMetadata(document.getMetadata());
            document.getChunks().add(chunk);

            // 向量化存储
            try {
                dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(chunks.get(i)).content();
                dev.langchain4j.data.segment.TextSegment segment = dev.langchain4j.data.segment.TextSegment.from(chunks.get(i));

                Map<String, String> meta = new HashMap<>();
                meta.put("document_id", document.getId());
                meta.put("title", document.getTitle());
                meta.put("chunk_index", String.valueOf(i));

                String storedId = embeddingStore.add(embedding, segment);
                chunk.setChunkId(storedId);
            } catch (Exception e) {
                log.error("向量化存储失败 chunkId={}: {}", chunk.getChunkId(), e.getMessage());
            }
        }

        log.info("文档入库完成, 分块数: {}", chunks.size());
    }

    @Override
    public List<KnowledgeDocument.DocumentChunk> search(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if (embeddingStore == null) return new ArrayList<>();

        try {
            // 向量检索
            dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchResult<dev.langchain4j.data.segment.TextSegment> vectorResult = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .maxResults(topK * 2) // 多召回一些用于混合排序
                            .minScore(minScore)
                            .build()
            );

            List<ScoredChunk> scoredChunks = new ArrayList<>();

            // 向量检索结果
            for (dev.langchain4j.store.embedding.EmbeddingMatch<dev.langchain4j.data.segment.TextSegment> match : vectorResult.matches()) {
                ScoredChunk sc = new ScoredChunk();
                sc.chunkId = match.embeddingId();
                sc.content = match.embedded() != null ? match.embedded().text() : "";
                sc.vectorScore = match.score();
                sc.finalScore = match.score() * vectorWeight;
                scoredChunks.add(sc);
            }

            // BM25 关键词匹配(简化实现)
            for (ScoredChunk sc : scoredChunks) {
                sc.keywordScore = calculateBm25Score(query, sc.content);
                sc.finalScore = sc.vectorScore * vectorWeight + sc.keywordScore * bm25Weight;
            }

            // 按最终分数排序
            scoredChunks.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));

            return scoredChunks.stream()
                    .limit(topK)
                    .map(sc -> {
                        KnowledgeDocument.DocumentChunk chunk = new KnowledgeDocument.DocumentChunk();
                        chunk.setChunkId(sc.chunkId);
                        chunk.setContent(sc.content);
                        return chunk;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("检索失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private double calculateBm25Score(String query, String document) {
        if (document == null || document.isEmpty()) return 0.0;

        String lowerDoc = document.toLowerCase();
        String[] queryTerms = query.toLowerCase().split("\\s+");

        int matchCount = 0;
        for (String term : queryTerms) {
            int idx = lowerDoc.indexOf(term);
            while (idx != -1) {
                matchCount++;
                idx = lowerDoc.indexOf(term, idx + 1);
            }
        }

        double docLength = lowerDoc.length();
        double avgDocLength = 500.0;
        double k1 = 1.2;
        double b = 0.75;

        double tf = (matchCount * (k1 + 1)) / (matchCount + k1 * (1 - b + b * docLength / avgDocLength));
        return Math.min(tf / (1 + tf), 1.0);
    }

    @Override
    public String rewriteQuery(String originalQuery) {
        String rewritePrompt = "请将以下用户查询重写为更精准、更利于知识库检索的表达方式，" +
                "最多重写3个版本，每个版本一行。\n\n原始查询: " + originalQuery;
        String rewritten = chatLanguageModel.generate(rewritePrompt);

        log.debug("查询重写: {} -> {}", originalQuery, rewritten);
        return rewritten;
    }

    @Override
    public String answer(String question) {
        // 检索相关文档
        List<KnowledgeDocument.DocumentChunk> chunks = search(question, retrievalTopK);
        if (chunks.isEmpty()) {
            return "未找到相关知识内容，请提供更多信息或参考技术文档。";
        }

        // 构建上下文
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            context.append("【参考资料").append(i + 1).append("】\n")
                    .append(chunks.get(i).getContent()).append("\n\n");
        }

        // 基于上下文生成回答
        String answerPrompt = "基于以下参考资料回答问题。如果资料中不包含相关信息，请明确说明。\n\n" +
                "参考资料:\n" + context + "\n" +
                "问题: " + question + "\n\n" +
                "回答:";

        return chatLanguageModel.generate(answerPrompt);
    }

    @Override
    public void ingestDocuments(List<KnowledgeDocument> documents) {
        for (KnowledgeDocument doc : documents) {
            ingestDocument(doc);
        }
    }

    private static class ScoredChunk {
        String chunkId;
        String content;
        double vectorScore;
        double keywordScore;
        double finalScore;
    }
}
