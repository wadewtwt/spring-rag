package com.example.springrag.application.chat;

import com.example.springrag.api.chat.ChatStreamRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    SseEmitter stream(ChatStreamRequest request);
}
