package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;

import java.util.List;

/**
 * RAG workflow-facing abstraction for LLM operations.
 */
public interface RagLlmGateway {

    String rewriteQuestion(String question, String currentQuery, int retryCount, List<SourceReference> sources);

    RetrievalEvaluation evaluateRetrieval(String question, String currentQuery, List<SourceReference> sources);

    String generateAnswer(String question, String currentQuery, List<SourceReference> sources);
}
