package com.example.springrag.application.chat;

import com.example.springrag.api.chat.ChatStreamRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天应用服务接口。
 * <p>
 * 这一层属于 application 层，也可以理解为“用例层”或“业务编排层”的入口抽象。
 * Controller 不关心聊天到底怎么完成，只依赖这个接口即可。
 * <p>
 * 这样做的好处是：
 * 1. API 层和具体实现解耦；
 * 2. 后续可以替换不同聊天实现；
 * 3. 更容易做测试和扩展。
 */
public interface ChatService {

    /**
     * 发起一次流式聊天。
     *
     * @param request 聊天请求，里面包含线程 ID 和用户输入
     * @return 用于向前端持续推送事件的 SSE 发射器
     */
    SseEmitter stream(ChatStreamRequest request);
}
