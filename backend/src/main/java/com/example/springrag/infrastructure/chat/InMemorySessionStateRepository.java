package com.example.springrag.infrastructure.chat;

import com.example.springrag.application.chat.SessionStateRepository;
import com.example.springrag.domain.chat.SessionState;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的会话状态仓储实现。
 * <p>
 * application 层只定义了“要能加载和保存会话状态”，
 * 具体怎么存由 infrastructure 层来决定。
 * 当前这个实现把会话状态放在一个线程安全的内存 Map 里，
 * 适合本地开发、演示和最小链路验证。
 * <p>
 * 它的局限也很明显：
 * 应用一重启，数据就没了；多实例部署时也无法共享状态。
 * 所以后续很适合替换成数据库或缓存版本。
 */
@Repository
public class InMemorySessionStateRepository implements SessionStateRepository {

    /**
     * 使用线程安全的 Map 保存所有会话状态。
     * key 是 threadId，value 是对应的 SessionState。
     */
    private final Map<String, SessionState> storage = new ConcurrentHashMap<>();

    /**
     * 从内存中加载会话状态。
     * <p>
     * 如果当前线程 ID 还没有历史数据，就返回一个空会话，
     * 这样上层业务不需要额外处理“第一次对话”的特殊情况。
     *
     * @param threadId 会话线程 ID
     * @return 对应的会话状态；如果不存在则返回空会话
     */
    @Override
    public SessionState load(String threadId) {
        return storage.getOrDefault(threadId, SessionState.empty(threadId));
    }

    /**
     * 把会话状态写回内存仓储。
     * <p>
     * 当前阶段先用内存方式把链路跑通，
     * 后续可以在不改 application 层接口的前提下平滑切换到数据库实现。
     *
     * @param state 需要保存的会话状态
     */
    @Override
    public void save(SessionState state) {
        storage.put(state.getThreadId(), state);
    }
}
