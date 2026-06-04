package com.ruima.ai.center.config;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStoreConfig.class);

    @Value("${langchain4j.chroma.base-url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${langchain4j.chroma.collection-name:ruima-embeddings}")
    private String collectionName;

    @Bean
    public EmbeddingStore embeddingStore() {
        try {
            ChromaV2Client store = new ChromaV2Client(chromaUrl, collectionName);
            log.info("使用 Chroma 向量存储 (url={}, collection={})", chromaUrl, collectionName);
            return store;
        } catch (Throwable e) {
            log.warn("Chroma 不可用，回退到 InMemoryEmbeddingStore: {}", e.getMessage());
            return new InMemoryEmbeddingStore();
        }
    }
}
