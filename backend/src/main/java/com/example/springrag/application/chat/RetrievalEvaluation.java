package com.example.springrag.application.chat;

/**
 * Result of evaluating whether retrieved context is sufficient to answer.
 *
 * @param satisfied whether current retrieval is sufficient
 * @param reason short explanation for debugging or UI reporting
 */
public record RetrievalEvaluation(boolean satisfied, String reason) {
}
