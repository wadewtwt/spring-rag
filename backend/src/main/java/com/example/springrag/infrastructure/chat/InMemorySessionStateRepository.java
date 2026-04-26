package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.SessionStateRepository;
import com.example.springrag.domain.chat.SessionState;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySessionStateRepository implements SessionStateRepository {

    private final Map<String, SessionState> storage = new ConcurrentHashMap<>();

    @Override
    public SessionState load(String threadId) {
        return storage.getOrDefault(threadId, SessionState.empty(threadId));
    }

    @Override
    public void save(SessionState state) {
        // 一期先用内存仓库存放会话状态，后续可以平滑替换为数据库实现。
        storage.put(state.getThreadId(), state);
    }
}
