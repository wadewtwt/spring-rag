package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RagWorkflowService;
import com.example.springrag.application.document.KnowledgeBaseService;
import com.example.springrag.config.RagProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfiguration {

    @Bean
    public RagWorkflowService ragWorkflowService(KnowledgeBaseService knowledgeBaseService,
                                                 RagLlmGateway ragLlmGateway,
                                                 RagProperties ragProperties) {
        return new RagWorkflowService(
                knowledgeBaseService,
                ragLlmGateway,
                ragProperties.getRetrieval().getMaxRetries()
        );
    }
}
