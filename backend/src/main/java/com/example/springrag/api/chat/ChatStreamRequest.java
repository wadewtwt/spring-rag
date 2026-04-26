package com.example.springrag.api.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatStreamRequest(
        @NotBlank(message = "threadId 不能为空")
        String threadId,
        @NotBlank(message = "message 不能为空")
        String message
) {
}
