package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SessionState;

public interface SessionStateRepository {

    SessionState load(String threadId);

    void save(SessionState state);
}
