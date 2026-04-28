package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SourceReference;

import java.util.List;

public interface RagLlmGateway {

    String rewriteQuestion(String question,
                           String currentQuery,
                           int retryCount,
                           List<SourceReference> sources,
                           List<ChatMessage> history);

    RetrievalEvaluation evaluateRetrieval(String question,
                                          String currentQuery,
                                          List<SourceReference> sources,
                                          List<ChatMessage> history);

    String generateAnswer(String question,
                          String currentQuery,
                          List<SourceReference> sources,
                          List<ChatMessage> history);
}
