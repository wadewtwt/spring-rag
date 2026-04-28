package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SessionState;
import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.infrastructure.chat.InMemorySessionStateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionStateRepositoryTest {

    @Test
    void shouldSaveAndLoadSessionState() {
        SessionStateRepository repository = new InMemorySessionStateRepository();
        SessionState state = SessionState.empty("thread-1");
        state.appendMessage(new ChatMessage("user", "hello"));

        repository.save(state);

        SessionState loaded = repository.load("thread-1");
        assertThat(loaded.getMessages()).hasSize(1);
    }

    @Test
    void shouldPreserveSourcesAndFailureState() {
        SessionStateRepository repository = new InMemorySessionStateRepository();
        SessionState state = SessionState.empty("thread-2");
        state.appendMessage(new ChatMessage("user", "refund?"));
        state.appendMessage(new ChatMessage("assistant", "please clarify"));
        state.setLastSources(List.of(new SourceReference("policy.md", "Refunds are allowed within seven days.")));
        state.setHandoffSuggested(true);
        state.incrementFailures();
        state.incrementFailures();

        repository.save(state);

        SessionState loaded = repository.load("thread-2");
        assertThat(loaded.getMessages()).hasSize(2);
        assertThat(loaded.getLastSources()).hasSize(1);
        assertThat(loaded.isHandoffSuggested()).isTrue();
        assertThat(loaded.getConsecutiveFailures()).isEqualTo(2);
    }
}
