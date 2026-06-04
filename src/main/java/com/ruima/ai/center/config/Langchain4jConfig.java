package com.ruima.ai.center.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Langchain4jConfig {

    // ── DeepSeek 对话 ──

    @Value("${langchain4j.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${langchain4j.deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${langchain4j.deepseek.chat-model}")
    private String deepseekChatModel;

    @Value("${langchain4j.deepseek.temperature:0.3}")
    private Double temperature;

    @Value("${langchain4j.deepseek.max-tokens:8192}")
    private Integer maxTokens;

    // ── DashScope Embedding ──

    @Value("${langchain4j.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${langchain4j.dashscope.embedding-model:text-embedding-v3}")
    private String embeddingModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekChatModel)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(embeddingModelName)
                .build();
    }
}
