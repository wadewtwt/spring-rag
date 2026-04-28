package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.SessionStateRepository;
import com.example.springrag.domain.chat.SessionState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStateRepository implements SessionStateRepository {

    private final Map<String, SessionState> storage = new ConcurrentHashMap<>();

    @Override
    public SessionState load(String threadId) {
        return storage.getOrDefault(threadId, SessionState.empty(threadId));
    }

    @Override
    public void save(SessionState state) {
        storage.put(state.getThreadId(), state);
    }
}
