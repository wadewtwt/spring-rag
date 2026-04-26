package com.example.springrag.application.chat;

import com.example.springrag.infrastructure.chat.KeywordHandoffPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordHandoffPolicyTest {

    @Test
    void shouldSuggestHandoffWhenMessageContainsNegativeKeyword() {
        HandoffPolicy policy = new KeywordHandoffPolicy();
        assertThat(policy.shouldHandoff("我要投诉，你们一直没解决", 0)).isTrue();
    }
}
