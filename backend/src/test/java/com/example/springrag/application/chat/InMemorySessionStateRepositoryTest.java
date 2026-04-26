package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SessionState;
import com.example.springrag.infrastructure.chat.InMemorySessionStateRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionStateRepositoryTest {

    @Test
    void shouldSaveAndLoadSessionState() {
        SessionStateRepository repository = new InMemorySessionStateRepository();
        SessionState state = SessionState.empty("thread-1");
        state.appendMessage(new ChatMessage("user", "你好"));

        repository.save(state);

        SessionState loaded = repository.load("thread-1");
        assertThat(loaded.getMessages()).hasSize(1);
    }
}
