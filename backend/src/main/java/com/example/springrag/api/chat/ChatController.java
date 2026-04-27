package com.example.springrag.api.chat;

import com.example.springrag.application.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天接口的 HTTP 入口。
 * <p>
 * 这个 Controller 自己不做复杂业务，只做两件事：
 * 接收前端请求，并把请求转交给应用层的 {@link ChatService}。
 * 这样做的好处是 Controller 保持很薄，真正的聊天流程编排放在业务层实现。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /**
     * 应用层聊天服务。
     */
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 以 SSE 的形式返回聊天响应。
     * <p>
     * SSE（Server-Sent Events）可以让后端持续往前端推送事件，
     * 很适合做流式对话、逐段输出答案、输出来源列表、输出状态通知等场景。
     *
     * @param request 前端传来的聊天请求，包含线程 ID 和用户消息
     * @return 一个可以持续推送事件的 {@link SseEmitter}
     */
    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
        return chatService.stream(request);
    }
}
