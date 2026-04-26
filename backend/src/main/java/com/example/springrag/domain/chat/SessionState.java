package com.example.springrag.domain.chat;

import java.util.ArrayList;
import java.util.List;

public class SessionState {

    private final String threadId;
    private final List<ChatMessage> messages;
    private List<SourceReference> lastSources;
    private boolean handoffSuggested;
    private int consecutiveFailures;

    private SessionState(String threadId, List<ChatMessage> messages) {
        this.threadId = threadId;
        this.messages = messages;
        this.lastSources = new ArrayList<>();
    }

    public static SessionState empty(String threadId) {
        return new SessionState(threadId, new ArrayList<>());
    }

    public void appendMessage(ChatMessage message) {
        this.messages.add(message);
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
}
