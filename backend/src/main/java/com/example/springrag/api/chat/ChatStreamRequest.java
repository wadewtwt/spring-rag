package com.example.springrag.api.chat;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天流式接口的请求体。
 * <p>
 * 这里使用 Java record 来表示一个“只承载数据”的对象，
 * 非常适合做请求参数和响应参数。
 *
 * @param threadId 会话线程 ID，用来区分不同对话
 * @param message 用户本次发送的消息内容
 */
public record ChatStreamRequest(
        /**
         * 会话 ID。
         * 前端每次发消息时都要带上它，这样后端才能找到对应的会话状态。
         */
        @NotBlank(message = "threadId 不能为空")
        String threadId,
        /**
         * 用户本轮输入的问题或消息。
         */
        @NotBlank(message = "message 不能为空")
        String message
) {
}
