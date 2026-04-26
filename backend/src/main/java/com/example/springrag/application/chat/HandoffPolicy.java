package com.example.springrag.application.chat;

public interface HandoffPolicy {

    boolean shouldHandoff(String message, int consecutiveFailures);
}
