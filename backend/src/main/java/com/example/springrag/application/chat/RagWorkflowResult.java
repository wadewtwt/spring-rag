package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SourceReference;

import java.util.List;

/**
 * Public result returned from the RAG workflow.
 *
 * @param answer final answer text
 * @param retrievalSatisfied whether the workflow found enough context to answer
 * @param finalQuery final query used for retrieval
 * @param retryCount number of rewrite-and-retry rounds used
 * @param sources final retrieved sources used by the workflow
 */
public record RagWorkflowResult(
        String answer,
        boolean retrievalSatisfied,
        String finalQuery,
        int retryCount,
        List<SourceReference> sources
) {
}
