package com.ruima.ai.center.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Langchain4jConfig {

    @Value("${langchain4j.dashscope.api-key:}")
    private String apiKey;

    @Value("${langchain4j.dashscope.chat-model:qwen-plus}")
    private String chatModelName;

    @Value("${langchain4j.dashscope.embedding-model:text-embedding-v3}")
    private String embeddingModelName;

    @Value("${langchain4j.dashscope.temperature:0.3}")
    private Float temperature;

    @Value("${langchain4j.dashscope.max-tokens:4096}")
    private Integer maxTokens;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }
}
