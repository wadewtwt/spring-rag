package com.example.springrag.application.chat.support;

import com.example.springrag.application.chat.RagLlmGateway;
import com.example.springrag.application.chat.RetrievalEvaluation;
import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SourceReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeWorkflowLlmGateway implements RagLlmGateway {

    private final Map<String, String> rewrites = new HashMap<>();
    private final Map<String, RetrievalEvaluation> evaluations = new HashMap<>();
    private final Map<String, String> answers = new HashMap<>();
    private List<ChatMessage> lastAnswerHistory = List.of();

    public FakeWorkflowLlmGateway withRewrite(String currentQuery, String rewrittenQuery) {
        rewrites.put(currentQuery, rewrittenQuery);
        return this;
    }

    public FakeWorkflowLlmGateway withEvaluation(String currentQuery, boolean satisfied, String reason) {
        evaluations.put(currentQuery, new RetrievalEvaluation(satisfied, reason));
        return this;
    }

    public FakeWorkflowLlmGateway withAnswer(String currentQuery, String answer) {
        answers.put(currentQuery, answer);
        return this;
    }

    @Override
    public String rewriteQuestion(String question, String currentQuery, int retryCount, List<SourceReference> sources, List<ChatMessage> history) {
        return rewrites.getOrDefault(currentQuery, currentQuery);
    }

    @Override
    public RetrievalEvaluation evaluateRetrieval(String question, String currentQuery, List<SourceReference> sources, List<ChatMessage> history) {
        return evaluations.getOrDefault(currentQuery, new RetrievalEvaluation(false, "missing fake evaluation"));
    }

    @Override
    public String generateAnswer(String question, String currentQuery, List<SourceReference> sources, List<ChatMessage> history) {
        this.lastAnswerHistory = history;
        return answers.getOrDefault(currentQuery, "missing fake answer");
    }

    public List<ChatMessage> lastAnswerHistory() {
        return lastAnswerHistory;
    }
}
