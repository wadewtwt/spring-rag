# Spring RAG 全栈骨架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个可运行的 Spring Boot + React 单仓全栈骨架，具备聊天 SSE、会话状态占位、文档上传占位与最小聊天页。

**Architecture:** 仓库拆分为 `backend` 和 `frontend` 两个目录。后端围绕聊天编排、会话状态和上传占位展开，前端提供最小聊天体验并消费后端 SSE 流。真实 RAG、状态机、向量库与数据库先通过接口抽象预留。

**Tech Stack:** Java 17, Spring Boot 3, Maven, JUnit 5, React, Vite, TypeScript, Vitest, React Testing Library

---

## 文件结构

- Create: `D:\work\java\spring-rag\backend\pom.xml`
- Create: `D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\...`
- Create: `D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\...`
- Create: `D:\work\java\spring-rag\frontend\package.json`
- Create: `D:\work\java\spring-rag\frontend\vite.config.ts`
- Create: `D:\work\java\spring-rag\frontend\src\...`
- Create: `D:\work\java\spring-rag\frontend\src\test\...`

### Task 1: 初始化后端工程

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/springrag/SpringRagApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/example/springrag/SmokeTest.java`

- [ ] **Step 1: 写失败测试**

```java
@SpringBootTest
class SmokeTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -Dtest=SmokeTest test`
Expected: FAIL，因为缺少 Spring Boot 项目骨架。

- [ ] **Step 3: 写最小实现**

```java
@SpringBootApplication
public class SpringRagApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringRagApplication.class, args);
    }
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `mvn -q -Dtest=SmokeTest test`
Expected: PASS

### Task 2: 健康检查接口

**Files:**
- Create: `backend/src/main/java/com/example/springrag/api/HealthController.java`
- Test: `backend/src/test/java/com/example/springrag/api/HealthControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnOkHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -Dtest=HealthControllerTest test`
Expected: FAIL，返回 404。

- [ ] **Step 3: 写最小实现**

```java
@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `mvn -q -Dtest=HealthControllerTest test`
Expected: PASS

### Task 3: 会话状态仓库与人工接管规则

**Files:**
- Create: `backend/src/main/java/com/example/springrag/domain/chat/ChatMessage.java`
- Create: `backend/src/main/java/com/example/springrag/domain/chat/SessionState.java`
- Create: `backend/src/main/java/com/example/springrag/domain/chat/SourceReference.java`
- Create: `backend/src/main/java/com/example/springrag/application/chat/SessionStateRepository.java`
- Create: `backend/src/main/java/com/example/springrag/infrastructure/chat/InMemorySessionStateRepository.java`
- Create: `backend/src/main/java/com/example/springrag/application/chat/HandoffPolicy.java`
- Create: `backend/src/main/java/com/example/springrag/infrastructure/chat/KeywordHandoffPolicy.java`
- Test: `backend/src/test/java/com/example/springrag/application/chat/InMemorySessionStateRepositoryTest.java`
- Test: `backend/src/test/java/com/example/springrag/application/chat/KeywordHandoffPolicyTest.java`

- [ ] **Step 1: 写失败测试**

```java
class InMemorySessionStateRepositoryTest {
    @Test
    void shouldSaveAndLoadSessionState() {
        SessionStateRepository repository = new InMemorySessionStateRepository();
        SessionState state = SessionState.empty("thread-1");
        state.appendMessage(new ChatMessage("user", "你好"));

        repository.save(state);

        SessionState loaded = repository.load("thread-1");
        assertThat(loaded.getMessages()).hasSize(1);
    }
}
```

```java
class KeywordHandoffPolicyTest {
    @Test
    void shouldSuggestHandoffWhenMessageContainsNegativeKeyword() {
        HandoffPolicy policy = new KeywordHandoffPolicy();
        assertThat(policy.shouldHandoff("我要投诉，你们一直没解决", 0)).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -Dtest=InMemorySessionStateRepositoryTest,KeywordHandoffPolicyTest test`
Expected: FAIL，因为类尚未定义。

- [ ] **Step 3: 写最小实现**

```java
public interface SessionStateRepository {
    SessionState load(String threadId);
    void save(SessionState state);
}
```

```java
public interface HandoffPolicy {
    boolean shouldHandoff(String message, int consecutiveFailures);
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `mvn -q -Dtest=InMemorySessionStateRepositoryTest,KeywordHandoffPolicyTest test`
Expected: PASS

### Task 4: 聊天 SSE 接口

**Files:**
- Create: `backend/src/main/java/com/example/springrag/api/chat/ChatStreamRequest.java`
- Create: `backend/src/main/java/com/example/springrag/application/chat/ChatService.java`
- Create: `backend/src/main/java/com/example/springrag/infrastructure/chat/StubChatService.java`
- Create: `backend/src/main/java/com/example/springrag/api/chat/ChatController.java`
- Test: `backend/src/test/java/com/example/springrag/api/chat/ChatControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnServerSentEvents() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"threadId":"thread-1","message":"你好"}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -Dtest=ChatControllerTest test`
Expected: FAIL，返回 404。

- [ ] **Step 3: 写最小实现**

```java
@PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@Valid @RequestBody ChatStreamRequest request) {
    return chatService.stream(request);
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `mvn -q -Dtest=ChatControllerTest test`
Expected: PASS

### Task 5: 文档上传接口

**Files:**
- Create: `backend/src/main/java/com/example/springrag/domain/document/DocumentRecord.java`
- Create: `backend/src/main/java/com/example/springrag/api/document/DocumentController.java`
- Test: `backend/src/test/java/com/example/springrag/api/document/DocumentControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptDocumentUpload() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                .file("file", "hello".getBytes(StandardCharsets.UTF_8))
                .param("filename", "guide.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -Dtest=DocumentControllerTest test`
Expected: FAIL，返回 404。

- [ ] **Step 3: 写最小实现**

```java
@PostMapping
public DocumentRecord upload(@RequestPart("file") MultipartFile file) {
    return new DocumentRecord(UUID.randomUUID().toString(), file.getOriginalFilename(), "UPLOADED");
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `mvn -q -Dtest=DocumentControllerTest test`
Expected: PASS

### Task 6: 初始化前端工程与基础渲染

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`
- Test: `frontend/src/App.test.tsx`

- [ ] **Step 1: 写失败测试**

```tsx
it('renders application title', () => {
  render(<App />);
  expect(screen.getByText('Spring RAG 客服控制台')).toBeInTheDocument();
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test -- App.test.tsx`
Expected: FAIL，因为前端工程不存在。

- [ ] **Step 3: 写最小实现**

```tsx
export default function App() {
  return <h1>Spring RAG 客服控制台</h1>;
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `npm run test -- App.test.tsx`
Expected: PASS

### Task 7: 聊天页面、线程 ID 与来源占位

**Files:**
- Create: `frontend/src/types/chat.ts`
- Create: `frontend/src/services/chat.ts`
- Create: `frontend/src/components/ChatWindow.tsx`
- Modify: `frontend/src/App.tsx`
- Test: `frontend/src/components/ChatWindow.test.tsx`

- [ ] **Step 1: 写失败测试**

```tsx
it('creates and displays thread id on first render', () => {
  render(<App />);
  expect(screen.getByText(/线程 ID/)).toBeInTheDocument();
});
```

```tsx
it('renders source placeholder area', () => {
  render(<App />);
  expect(screen.getByText('参考来源')).toBeInTheDocument();
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test -- ChatWindow.test.tsx`
Expected: FAIL，因为聊天界面尚未实现。

- [ ] **Step 3: 写最小实现**

```tsx
function createThreadId(): string {
  return crypto.randomUUID();
}
```

```tsx
<section>
  <h2>参考来源</h2>
</section>
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `npm run test -- ChatWindow.test.tsx`
Expected: PASS

### Task 8: 前端接入后端 SSE

**Files:**
- Modify: `frontend/src/services/chat.ts`
- Modify: `frontend/src/components/ChatWindow.tsx`
- Test: `frontend/src/components/ChatWindow.test.tsx`

- [ ] **Step 1: 写失败测试**

```tsx
it('shows assistant chunks when stream emits chunk events', async () => {
  render(<App />);
  // 模拟流事件后应看到逐步拼接的回答
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test -- ChatWindow.test.tsx`
Expected: FAIL，因为 SSE 处理尚未实现。

- [ ] **Step 3: 写最小实现**

```tsx
for await (const event of streamChat(request)) {
  if (event.type === 'chunk') {
    // 逐步更新回答文本
  }
}
```

- [ ] **Step 4: 再次运行测试确认通过**

Run: `npm run test -- ChatWindow.test.tsx`
Expected: PASS

### Task 9: 端到端验证

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `frontend/package.json`

- [ ] **Step 1: 运行后端测试**

Run: `mvn test`
Expected: PASS

- [ ] **Step 2: 运行前端测试**

Run: `npm test`
Expected: PASS

- [ ] **Step 3: 构建前后端**

Run: `mvn package`
Expected: BUILD SUCCESS

Run: `npm run build`
Expected: build completed
