package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.config.RagProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfiguration {

    @Bean
    public RagLlmGateway ragLlmGateway(RagProperties ragProperties, ObjectMapper objectMapper) {
        if (!ragProperties.getLlm().isEnabled()) {
            return new DisabledRagLlmGateway();
        }
        if (ragProperties.getLlm().getApiKey() == null || ragProperties.getLlm().getApiKey().isBlank()) {
            throw new IllegalStateException("app.rag.llm.api-key must be configured when LLM is enabled");
        }
        return new DashScopeRagLlmGateway(ragProperties.getLlm(), objectMapper);
    }
}
