package com.ruima.ai.center.config;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStoreConfig.class);

    @Bean
    public EmbeddingStore embeddingStore() {
        log.info("使用 InMemoryEmbeddingStore（本地内存向量存储）");
        return new InMemoryEmbeddingStore();
    }
}
