package com.ruima.ai.center.service;

import com.ruima.ai.center.model.dto.KnowledgeDocument;
import java.util.List;

/**
 * RAG（检索增强生成）服务
 * 查询重写 + 语义分块 + 混合检索(BM25+向量)
 */
public interface RagService {

    /**
     * 文档入库 - 解析并向量化存储
     */
    void ingestDocument(KnowledgeDocument document);

    /**
     * 混合检索 - BM25 + 向量检索
     */
    List<KnowledgeDocument.DocumentChunk> search(String query, int topK);

    /**
     * 查询重写 - 优化检索查询
     */
    String rewriteQuery(String originalQuery);

    /**
     * RAG问答 - 检索+生成
     */
    String answer(String question);

    /**
     * 批量文档入库
     */
    void ingestDocuments(List<KnowledgeDocument> documents);
}
