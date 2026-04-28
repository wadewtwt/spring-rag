package com.example.springrag.domain.chat;

import java.io.Serializable;
import java.time.Instant;

/**
 * 领域层中的一条聊天消息。
 * <p>
 * domain 层负责表达“业务世界里有哪些核心对象”。
 * 这里的 ChatMessage 不关心数据库、不关心 HTTP、不关心 Spring，
 * 只关心“消息本身应该长什么样”。
 *
 * @param role 消息角色，例如 user、assistant
 * @param content 消息正文
 * @param timestamp 消息产生时间
 */
public record ChatMessage(String role, String content, Instant timestamp) implements Serializable {

    /**
     * 便捷构造方法。
     * <p>
     * 调用方只传角色和内容时，时间默认取当前时刻。
     *
     * @param role 消息角色
     * @param content 消息内容
     */
    public ChatMessage(String role, String content) {
        this(role, content, Instant.now());
    }
}
