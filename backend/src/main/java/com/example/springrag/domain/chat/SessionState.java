package com.example.springrag.domain.chat;

import java.util.ArrayList;
import java.util.List;

public class SessionState {

    private final String threadId;
    private final List<ChatMessage> messages;
    private List<SourceReference> lastSources;
    private boolean handoffSuggested;
    private int consecutiveFailures;

    private SessionState(String threadId,
                         List<ChatMessage> messages,
                         List<SourceReference> lastSources,
                         boolean handoffSuggested,
                         int consecutiveFailures) {
        this.threadId = threadId;
        this.messages = new ArrayList<>(messages);
        this.lastSources = new ArrayList<>(lastSources);
        this.handoffSuggested = handoffSuggested;
        this.consecutiveFailures = consecutiveFailures;
    }

    public static SessionState empty(String threadId) {
        return new SessionState(threadId, List.of(), List.of(), false, 0);
    }

    public static SessionState restore(Snapshot snapshot) {
        return new SessionState(
                snapshot.threadId(),
                snapshot.messages(),
                snapshot.lastSources(),
                snapshot.handoffSuggested(),
                snapshot.consecutiveFailures()
        );
    }

    public Snapshot snapshot() {
        return new Snapshot(
                threadId,
                List.copyOf(messages),
                List.copyOf(lastSources),
                handoffSuggested,
                consecutiveFailures
        );
    }

    public void appendMessage(ChatMessage message) {
        this.messages.add(message);
    }

    public List<ChatMessage> recentMessages(int maxMessages) {
        int fromIndex = Math.max(0, messages.size() - maxMessages);
        return List.copyOf(messages.subList(fromIndex, messages.size()));
    }

    public String getThreadId() {
        return threadId;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<SourceReference> getLastSources() {
        return lastSources;
    }

    public void setLastSources(List<SourceReference> lastSources) {
        this.lastSources = new ArrayList<>(lastSources);
    }

    public boolean isHandoffSuggested() {
        return handoffSuggested;
    }

    public void setHandoffSuggested(boolean handoffSuggested) {
        this.handoffSuggested = handoffSuggested;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void incrementFailures() {
        this.consecutiveFailures++;
    }

    public void resetFailures() {
        this.consecutiveFailures = 0;
    }

    public record Snapshot(
            String threadId,
            List<ChatMessage> messages,
            List<SourceReference> lastSources,
            boolean handoffSuggested,
            int consecutiveFailures
    ) {
    }
}
