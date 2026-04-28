package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.ChatMessage;
import com.example.springrag.domain.chat.SessionState;
import com.example.springrag.domain.chat.SourceReference;
import com.example.springrag.infrastructure.chat.JpaSessionStateRepository;
import com.example.springrag.infrastructure.chat.SpringDataSessionStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaSessionStateRepository.class, ObjectMapper.class})
class JpaSessionStateRepositoryTest {

    @Autowired
    private SessionStateRepository repository;

    @Autowired
    private SpringDataSessionStateRepository springDataSessionStateRepository;

    @Test
    void shouldSaveAndLoadFullSessionSnapshot() {
        SessionState state = SessionState.empty("thread-jpa-1");
        state.appendMessage(new ChatMessage("user", "first question"));
        state.appendMessage(new ChatMessage("assistant", "first answer"));
        state.setLastSources(List.of(new SourceReference("guide.md", "Warranty period is two years.")));
        state.setHandoffSuggested(true);
        state.incrementFailures();

        repository.save(state);

        SessionState loaded = repository.load("thread-jpa-1");
        assertThat(loaded.getThreadId()).isEqualTo("thread-jpa-1");
        assertThat(loaded.getMessages()).hasSize(2);
        assertThat(loaded.getMessages().get(0).content()).isEqualTo("first question");
        assertThat(loaded.getLastSources()).containsExactly(new SourceReference("guide.md", "Warranty period is two years."));
        assertThat(loaded.isHandoffSuggested()).isTrue();
        assertThat(loaded.getConsecutiveFailures()).isEqualTo(1);
        assertThat(springDataSessionStateRepository.count()).isEqualTo(1);
    }
}
