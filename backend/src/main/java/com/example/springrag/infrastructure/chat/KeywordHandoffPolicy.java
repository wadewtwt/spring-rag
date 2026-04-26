package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.HandoffPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeywordHandoffPolicy implements HandoffPolicy {

    private static final List<String> NEGATIVE_KEYWORDS = List.of("投诉", "差评", "人工", "生气", "没解决");

    @Override
    public boolean shouldHandoff(String message, int consecutiveFailures) {
        return consecutiveFailures >= 2 || NEGATIVE_KEYWORDS.stream().anyMatch(message::contains);
    }
}
