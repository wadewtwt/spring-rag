package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RetrievalEvaluation;
import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SourceReference;

import java.util.List;

public class DisabledRagLlmGateway implements RagLlmGateway {

    private static final String MESSAGE = "RAG LLM gateway is disabled";

    @Override
    public String rewriteQuestion(String question, String currentQuery, int retryCount, List<SourceReference> sources, List<ChatMessage> history) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public RetrievalEvaluation evaluateRetrieval(String question, String currentQuery, List<SourceReference> sources, List<ChatMessage> history) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public String generateAnswer(String question, String currentQuery, List<SourceReference> sources, List<ChatMessage> history) {
        throw new IllegalStateException(MESSAGE);
    }
}
