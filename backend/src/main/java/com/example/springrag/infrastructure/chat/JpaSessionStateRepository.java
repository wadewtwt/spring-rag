package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.SessionStateRepository;
import com.example.springrag.domain.chat.SessionState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSessionStateRepository implements SessionStateRepository {

    private final SpringDataSessionStateRepository repository;
    private final ObjectMapper objectMapper;

    public JpaSessionStateRepository(SpringDataSessionStateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.findAndRegisterModules();
    }

    @Override
    public SessionState load(String threadId) {
        return repository.findById(threadId)
                .map(this::toSessionState)
                .orElseGet(() -> SessionState.empty(threadId));
    }

    @Override
    public void save(SessionState state) {
        SessionStateEntity entity = repository.findById(state.getThreadId())
                .orElseGet(() -> new SessionStateEntity(state.getThreadId(), ""));
        entity.setSnapshotJson(toJson(state.snapshot()));
        repository.save(entity);
    }

    private SessionState toSessionState(SessionStateEntity entity) {
        try {
            SessionState.Snapshot snapshot = objectMapper.readValue(entity.getSnapshotJson(), SessionState.Snapshot.class);
            return SessionState.restore(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize session snapshot", exception);
        }
    }

    private String toJson(SessionState.Snapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session snapshot", exception);
        }
    }
}
