package com.example.springrag.application.chat;

import com.example.springrag.domain.chat.SessionState;

/**
 * 会话状态仓储接口。
 * <p>
 * 这个接口表达的是“应用层需要一个地方来加载和保存会话状态”，
 * 但并不关心这个地方到底是内存、数据库还是缓存。
 * <p>
 * 这也是分层设计中很常见的做法：
 * application 层只定义需要什么能力，infrastructure 层去提供具体实现。
 */
public interface SessionStateRepository {

    /**
     * 根据线程 ID 加载会话状态。
     *
     * @param threadId 会话线程 ID
     * @return 对应会话的状态对象
     */
    SessionState load(String threadId);

    /**
     * 保存会话状态。
     *
     * @param state 需要持久化的会话状态
     */
    void save(SessionState state);
}
