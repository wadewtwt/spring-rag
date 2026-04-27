# Spring RAG Backend 阅读文档

> 面向对象：1 年左右 Java / Spring Boot 开发经验  
> 阅读目标：看懂这个 `backend` 项目现在“已经实现了什么”“每个文件在做什么”“主要方法怎么串起来工作”  
> 阅读建议：不要一上来逐行硬读代码，先看“整体架构”，再按“入口 -> Controller -> Service -> Domain -> Infrastructure -> Test”的顺序看

---

## 1. 先说结论：这个 backend 现在已经做到了什么

这个后端已经不是一个空骨架了，它已经具备下面这些能力：

- Spring Boot 3 基础启动能力
- 健康检查接口
- RAG 运行状态查询接口
- 文档上传接口
- 基于文本切片的知识库索引
- 基于 LangChain4j 的 embedding / embedding store 抽象
- 本地 embedding 模型和 simple embedding 的双实现
- Chroma 向量存储配置接入
- 内存向量存储回退机制
- 多轮会话的内存态保存
- 人工接管规则判断
- SSE 流式聊天接口
- 基于 LangGraph4j 的最小状态机工作流

但你要注意：

- 现在的 `rewriter / evaluator / generator` 还是“最小可跑版本”，不是完整生产实现
- 会话状态还是内存版，不是数据库版
- 文档解析还是“文本按行切片”，不是 PDF/Markdown 真正解析
- LLM 生成节点还没有接入真实大模型

所以这套代码最适合你的学习方式不是“把它当成成品系统”，而是“把它当成一个分层很清晰、方便继续扩展的 RAG 后端骨架”。

---

## 2. 项目整体结构

`backend` 目录核心结构可以分成 6 层：

1. 启动层
- Spring Boot 应用入口

2. API 层
- 对外暴露 HTTP 接口
- 负责接收请求、校验参数、返回响应

3. Application 层
- 放业务接口
- 放工作流编排逻辑
- 更像“用例层”

4. Domain 层
- 放核心数据对象
- 不直接依赖 Spring

5. Infrastructure 层
- 放具体实现
- 比如内存仓库、embedding 工厂、Chroma 配置、聊天服务实现

6. Test 层
- 验证每一层代码是否符合预期

如果你以后自己写项目，推荐你也参考这种结构。它的好处是：

- Controller 不会太臃肿
- 业务接口和实现分开
- 以后替换底层实现更容易
- 测试更清晰

---

## 3. 推荐阅读顺序

如果你是第一次看这个项目，我建议你按这个顺序读：

1. [pom.xml](D:\work\java\spring-rag\backend\pom.xml)
2. [SpringRagApplication.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\SpringRagApplication.java)
3. API 层 4 个 Controller / Request
4. Domain 层 4 个核心对象
5. `ChatService`、`KnowledgeBaseService`、`SessionStateRepository`、`HandoffPolicy` 这些接口
6. `StubChatService`
7. `InMemoryKnowledgeBaseService`
8. embedding 相关配置和工厂
9. `RagWorkflowService`
10. 所有测试类

你只要按这个顺序，理解会顺很多。

---

## 4. 依赖和构建文件

### 4.1 [pom.xml](D:\work\java\spring-rag\backend\pom.xml)

这个文件是 Maven 项目的核心配置。

### 作用

- 定义项目用什么 Java 版本
- 定义用哪些依赖
- 定义如何打包

### 重点内容

#### Spring Boot 父工程

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
</parent>
```

这表示项目基于 Spring Boot 3.3.5。

#### Java 版本

```xml
<java.version>17</java.version>
```

说明这个项目要求 JDK 17。

#### 主要依赖

- `spring-boot-starter-web`
  作用：提供 Web 开发能力，比如 `@RestController`、JSON、SSE、Tomcat

- `langchain4j`
  作用：提供 RAG 相关数据结构和能力抽象，比如 `Embedding`、`TextSegment`

- `langchain4j-embeddings-bge-small-en-v15-q`
  作用：本地 embedding 模型实现

- `langchain4j-chroma`
  作用：接入 Chroma 向量存储

- `langgraph4j-core`
  作用：状态机工作流编排

- `spring-boot-starter-validation`
  作用：参数校验，比如 `@NotBlank`

- `spring-boot-starter-test`
  作用：测试

### 你要重点理解的事

这个项目不是直接自己实现所有 AI 能力，而是：

- 用 `LangChain4j` 管 embedding / 向量相关抽象
- 用 `LangGraph4j` 管流程编排
- 用 `Spring Boot` 管 Web 和依赖注入

这就是现在 AI Java 项目比较常见的组合方式。

---

## 5. 启动层

### 5.1 [SpringRagApplication.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\SpringRagApplication.java)

### 作用

这是整个 Spring Boot 项目的启动入口。

### 代码解读

#### `@SpringBootApplication`

表示这是 Spring Boot 主启动类，它等价于：

- `@Configuration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

也就是说，Spring 会从这个类所在包开始扫描 Bean。

#### `@EnableConfigurationProperties(RagProperties.class)`

这句很重要，它告诉 Spring：

- 把配置文件里的 `app.rag.*` 自动绑定到 `RagProperties`

#### `main`

```java
public static void main(String[] args) {
    SpringApplication.run(SpringRagApplication.class, args);
}
```

这是标准 Spring Boot 启动方式。

---

## 6. 配置层

### 6.1 [RagProperties.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\config\RagProperties.java)

### 作用

把配置文件里的 RAG 参数封装成 Java 对象。

### 为什么要这样做

不要在代码里到处写：

- `@Value("${xxx}")`
- `"http://localhost:8000"`
- `"spring-rag"`

而是统一放到一个配置对象里，方便维护和扩展。

### 结构解读

它内部有三层：

- `Embedding`
- `Store`
- `Chroma`

对应配置结构：

```yml
app:
  rag:
    embedding:
      mode: local
    store:
      mode: inmemory
      chroma:
        base-url: http://localhost:8000
```

### 主要方法

#### `getEmbedding()`
- 取 embedding 配置

#### `getStore()`
- 取向量存储配置

#### `Embedding#getMode() / setMode()`
- 当前 embedding 模式
- 现在项目里主要是 `local` 或 `simple`

#### `Store#getMode() / setMode()`
- 当前向量存储模式
- 现在项目里主要是 `inmemory` 或 `chroma`

#### `Chroma` 里的 getters / setters
- Chroma 的连接参数
- 包括 `baseUrl`、`tenant`、`database`、`collection`

### 你要学会的点

`ConfigurationProperties` 是 Spring Boot 项目里非常推荐的配置管理方式，比散落的 `@Value` 更好。

---

## 7. API 层

API 层的职责很简单：

- 接收请求
- 校验参数
- 调用 service
- 返回结果

它不应该放太多真正业务逻辑。

### 7.1 [HealthController.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\api\HealthController.java)

### 作用

提供健康检查接口。

### 接口

- `GET /api/health`

### 方法

#### `health()`

返回：

```json
{"status":"UP"}
```

### 为什么这个接口重要

以后无论是本地调试、网关探活、K8s 探针，都会用到这种接口。

---

### 7.2 [RagStatusController.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\api\RagStatusController.java)

### 作用

暴露当前 RAG 运行模式，方便联调。

### 接口

- `GET /api/rag/status`

### 字段说明

- `embeddingMode`
- `storeMode`
- `chroma.baseUrl`
- `chroma.tenant`
- `chroma.database`
- `chroma.collection`

### 方法

#### 构造方法

```java
public RagStatusController(RagProperties ragProperties)
```

通过构造注入配置对象。

#### `status()`

把 `RagProperties` 转成响应对象返回。

### 内部 record

#### `RagStatusResponse`
- 整个状态响应体

#### `ChromaStatus`
- Chroma 配置部分

### 你要理解的点

这个 Controller 不是业务核心，但对联调很有帮助。  
很多项目里，类似“当前配置状态接口”都很实用。

---

### 7.3 [ChatController.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\api\chat\ChatController.java)

### 作用

提供聊天 SSE 接口。

### 接口

- `POST /api/chat/stream`

### 方法

#### 构造方法

注入 `ChatService`

#### `stream(@Valid @RequestBody ChatStreamRequest request)`

这是核心入口之一。

做了三件事：

1. 用 `@Valid` 触发参数校验
2. 接收 `ChatStreamRequest`
3. 调用 `chatService.stream(request)` 返回 `SseEmitter`

### 为什么返回 `SseEmitter`

因为聊天不是一次性整个响应返回，而是可以分段推送：

- `chunk`
- `sources`
- `handoff`
- `complete`

这就是 SSE 的典型用法。

---

### 7.4 [ChatStreamRequest.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\api\chat\ChatStreamRequest.java)

### 作用

聊天请求 DTO。

### 字段

- `threadId`
- `message`

### 注解

#### `@NotBlank`

保证字段不能为空字符串。

### 为什么用 record

因为这种 DTO：

- 结构简单
- 不需要复杂行为

用 `record` 非常合适。

---

### 7.5 [DocumentController.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\api\document\DocumentController.java)

### 作用

提供文档上传接口。

### 接口

- `POST /api/documents`

### 方法

#### 构造方法

注入 `KnowledgeBaseService`

#### `upload(@RequestPart("file") MultipartFile file)`

流程：

1. 接收上传文件
2. 读取字节
3. 调用 `knowledgeBaseService.store(...)`
4. 返回 `DocumentRecord`

### 注意点

现在这里只支持“上传即索引”。  
还没有“文档列表 / 删除 / 重建索引”等能力。

---

## 8. Application 层接口

这层你可以理解成“业务能力的抽象定义”。

### 8.1 [ChatService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\ChatService.java)

### 作用

定义聊天服务接口。

### 方法

#### `stream(ChatStreamRequest request)`

输入请求，返回 `SseEmitter`。

### 设计意义

Controller 不直接依赖具体实现，而是依赖接口。

---

### 8.2 [HandoffPolicy.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\HandoffPolicy.java)

### 作用

定义“是否应该转人工”的判断接口。

### 方法

#### `shouldHandoff(String message, int consecutiveFailures)`

根据：

- 用户消息
- 连续失败次数

判断是否该转人工。

### 设计意义

以后你可以替换成：

- 关键词规则版
- 情绪分析版
- 评分模型版

---

### 8.3 [SessionStateRepository.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\SessionStateRepository.java)

### 作用

定义会话状态仓库接口。

### 方法

#### `load(String threadId)`
- 根据线程 ID 读取会话

#### `save(SessionState state)`
- 保存会话

### 设计意义

以后从内存切数据库时，Controller 和 ChatService 不用大改。

---

### 8.4 [KnowledgeBaseService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\document\KnowledgeBaseService.java)

### 作用

定义知识库服务接口。

### 方法

#### `store(String fileName, byte[] content)`
- 存储并索引文档

#### `search(String query)`
- 检索相关片段

### 设计意义

以后知识库实现可以替换：

- 内存版
- Chroma 版
- Milvus 版

---

## 9. Domain 层

这层是“核心数据对象”。

### 9.1 [ChatMessage.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\domain\chat\ChatMessage.java)

### 作用

表示一条聊天消息。

### 字段

- `role`
- `content`
- `timestamp`

### 构造方法

#### `ChatMessage(String role, String content)`

自动补一个 `Instant.now()` 时间。

### 什么时候用

- 用户消息入会话
- 助手回答入会话

---

### 9.2 [SessionState.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\domain\chat\SessionState.java)

### 作用

表示一个会话线程当前的完整状态。

### 字段

- `threadId`
- `messages`
- `lastSources`
- `handoffSuggested`
- `consecutiveFailures`

### 主要方法

#### `empty(String threadId)`
- 创建空会话

#### `appendMessage(ChatMessage message)`
- 往消息列表里追加一条消息

#### `getMessages()`
- 取历史消息

#### `setLastSources(List<SourceReference> lastSources)`
- 保存最近一次回答的来源

#### `setHandoffSuggested(boolean handoffSuggested)`
- 标记是否建议转人工

#### `incrementFailures()`
- 失败次数 +1

#### `resetFailures()`
- 失败次数清零

### 这个类为什么重要

它是会话记忆的核心对象。  
以后无论切数据库、接 LangGraph checkpoint，本质上都绕不开这类状态对象。

---

### 9.3 [SourceReference.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\domain\chat\SourceReference.java)

### 作用

表示一个知识来源片段。

### 字段

- `title`
- `snippet`

### 为什么实现 `Serializable`

因为 LangGraph4j 在执行状态图时会拷贝 / 序列化状态。  
如果来源对象不能序列化，工作流会报错。

这是一个非常典型的“框架约束影响领域对象设计”的例子。

---

### 9.4 [DocumentRecord.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\domain\document\DocumentRecord.java)

### 作用

表示上传后的文档记录。

### 字段

- `documentId`
- `fileName`
- `status`

### 当前状态值

当前主要会返回：

- `INDEXED`

---

## 10. Infrastructure 层：聊天相关

### 10.1 [InMemorySessionStateRepository.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\chat\InMemorySessionStateRepository.java)

### 作用

`SessionStateRepository` 的内存实现。

### 字段

#### `storage`

```java
private final Map<String, SessionState> storage = new ConcurrentHashMap<>();
```

用线程安全 Map 保存会话状态。

### 方法

#### `load(String threadId)`

如果有就返回已有会话，没有就返回 `SessionState.empty(threadId)`。

#### `save(SessionState state)`

把状态放进 Map。

### 你要理解的点

这就是典型“接口 + 内存实现”的写法。  
以后替换数据库版时，这个类会被新的实现替代。

---

### 10.2 [KeywordHandoffPolicy.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\chat\KeywordHandoffPolicy.java)

### 作用

`HandoffPolicy` 的关键词规则实现。

### 字段

#### `NEGATIVE_KEYWORDS`

用于判断是否触发人工接管的关键词集合。

### 方法

#### `shouldHandoff(String message, int consecutiveFailures)`

规则：

- 连续失败次数 >= 2
- 或者消息里包含负面关键词

满足任意一个就返回 `true`。

### 这是一个什么层次的实现

这是最小规则版，不是最终版。  
优点是简单、稳定、易于验证。

---

### 10.3 [StubChatService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\chat\StubChatService.java)

这是当前后端最关键的类之一。

### 作用

实现聊天主流程：

- 读取会话
- 检索知识
- 执行状态机
- 决定是否转人工
- 保存会话
- 通过 SSE 返回结果

### 依赖

- `SessionStateRepository`
- `HandoffPolicy`
- `KnowledgeBaseService`
- `RagWorkflowService`

### 方法

#### 构造方法

标准依赖注入。

#### `stream(ChatStreamRequest request)`

这个方法建议你重点读。

它的执行顺序是：

1. 创建 `SseEmitter`
2. 根据 `threadId` 读取会话状态
3. 把用户消息放进会话
4. 调用 `knowledgeBaseService.search(...)` 检索来源
5. 调用 `ragWorkflowService.run(...)` 执行状态机
6. 根据 `handoffPolicy` 判断是否该转人工
7. 把来源、回答、接管状态写回 `SessionState`
8. 保存会话
9. 通过 SSE 推送事件
10. 完成 SSE

### SSE 推送了什么事件

- `chunk`
- `sources`
- `handoff`
- `complete`

### 为什么它叫 `StubChatService`

因为它仍然是“骨架实现”，不是最终的完整生产版聊天服务。  
但它已经能真正工作了。

### 你读这个类时要重点关注什么

- Controller 如何调用 Service
- Service 如何调用知识库和工作流
- 会话状态是如何读写的
- SSE 事件是怎么发出去的

---

## 11. Infrastructure 层：知识库与 embedding

这一块是这个项目最有“AI 项目味道”的部分。

### 11.1 [TextEmbeddingService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\TextEmbeddingService.java)

### 作用

定义文本转向量的统一接口。

### 方法

#### `embed(String text)`

输入文本，输出 `Embedding`。

### 设计意义

屏蔽底层实现差异：

- simple embedding
- 本地 ONNX embedding
- 未来远程 OpenAI embedding

---

### 11.2 [SimpleTextEmbeddingService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\SimpleTextEmbeddingService.java)

### 作用

一个“纯本地、无外部依赖”的简单 embedding 实现。

### 为什么存在

因为真实本地模型初始化可能慢、可能失败、测试也不需要这么重。  
所以需要一个轻量兜底实现。

### 主要字段

#### `DIMENSION = 128`

向量维度。

### 主要方法

#### `embed(String text)`

主要流程：

1. 先归一化文本
2. 按 token 拆分
3. 再按字符和双字符特征做 hash 累积
4. 生成 `float[]`
5. 转成 `Embedding`
6. 调用 `normalize()`

你可以把它理解成一个“简化版的文本特征哈希向量器”。

#### `normalize(String text)`

做文本归一化：

- 转小写
- 去标点
- 去空白干扰

#### `accumulate(float[] vector, String feature, float weight)`

把某个特征 hash 到固定维度上并累加权重。

### 这个类你最应该学什么

不是学 embedding 数学，而是学“先抽象接口，再写兜底实现”的工程思路。

---

### 11.3 [LangChain4jLocalEmbeddingService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\LangChain4jLocalEmbeddingService.java)

### 作用

使用 LangChain4j 的本地 ONNX embedding 模型生成真实向量。

### 字段

#### `EmbeddingModel embeddingModel`

底层模型接口。

### 构造方法

#### 无参构造

直接使用：

`BgeSmallEnV15QuantizedEmbeddingModel`

#### 带参构造

允许外部传入具体模型，便于扩展和测试。

### 方法

#### `embed(String text)`

调用：

```java
embeddingModel.embed(text).content()
```

得到真实 embedding。

### 你要理解的点

这个类是真正连接 LangChain4j 模型能力的地方。

---

### 11.4 [EmbeddingServiceFactory.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\EmbeddingServiceFactory.java)

### 作用

负责创建 embedding service，并处理失败回退。

### 方法

#### `createDefault()`

默认优先创建 `LangChain4jLocalEmbeddingService`

#### `create(Supplier<TextEmbeddingService> localSupplier)`

核心逻辑：

1. 尝试创建真实 embedding service
2. 如果失败，记录 warning
3. 回退到 `SimpleTextEmbeddingService`

### 这是很典型的什么设计

典型的“工厂 + fallback”设计。

---

### 11.5 [EmbeddingServiceConfiguration.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\EmbeddingServiceConfiguration.java)

### 作用

把 `TextEmbeddingService` 注册成 Spring Bean。

### 方法

#### `textEmbeddingService(String mode, EmbeddingServiceFactory factory)`

根据配置决定：

- `simple` -> `SimpleTextEmbeddingService`
- 其他 -> 工厂创建默认实现

### 你要理解的点

这是“配置决定运行时实现”的典型 Spring 写法。

---

### 11.6 [EmbeddingStoreFactory.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\EmbeddingStoreFactory.java)

### 作用

负责创建向量存储，并在 Chroma 失败时回退到内存存储。

### 方法

#### `createChromaOrFallback(Supplier<EmbeddingStore<TextSegment>> chromaSupplier)`

流程：

1. 尝试创建 Chroma store
2. 失败则 warning
3. 回退 `InMemoryEmbeddingStore`

---

### 11.7 [EmbeddingStoreConfiguration.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\EmbeddingStoreConfiguration.java)

### 作用

把 `EmbeddingStore<TextSegment>` 注册成 Spring Bean。

### 方法

#### `embeddingStore(...)`

根据配置决定：

- `chroma` -> 构建 `ChromaEmbeddingStore`
- 其他 -> `InMemoryEmbeddingStore`

### Chroma 相关参数

- `baseUrl`
- `tenant`
- `database`
- `collection`
- `apiVersion = V2`

### 这层为什么重要

它把“向量存储选型”变成了配置，而不是写死在业务代码里。

---

### 11.8 [InMemoryKnowledgeBaseService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\InMemoryKnowledgeBaseService.java)

这个类是知识库主实现，名字里虽然有 `InMemory`，但它实际上已经依赖抽象的 `EmbeddingStore<TextSegment>` 了。

### 作用

- 存文档
- 切片
- 建 embedding
- 存入 embedding store
- 检索相关片段

### 依赖

- `EmbeddingStore<TextSegment>`
- `TextEmbeddingService`

### 方法

#### 构造方法

通过依赖注入得到：

- 向量存储
- embedding 服务

#### `store(String fileName, byte[] content)`

执行顺序：

1. 生成 `documentId`
2. 把字节转成字符串
3. `splitIntoChunks(text)` 切片
4. 每个切片构造 `Metadata`
5. 每个切片转成 `TextSegment`
6. 对每个切片生成 embedding
7. 调用 `embeddingStore.add(...)`
8. 返回 `DocumentRecord`

#### `search(String query)`

执行顺序：

1. 把 query 转成 embedding
2. 构建 `EmbeddingSearchRequest`
3. 配置 `maxResults(3)` 和 `minScore(0.15)`
4. 调用 `embeddingStore.search(...)`
5. 把命中的 `TextSegment` 转成 `SourceReference`

#### `splitIntoChunks(String text)`

当前最小切片策略：

- 按行切
- 去掉空行

### 这个类你该怎么学

重点不是“切片策略够不够强”，而是先理解：

- 文档内容如何变成 `TextSegment`
- 文本如何变成 `Embedding`
- 检索结果如何再映射回业务对象

---

## 12. LangGraph4j 工作流层

这部分是项目里最值得你重点看的部分之一。

### 12.1 [RagWorkflowResult.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\RagWorkflowResult.java)

### 作用

工作流输出对象。

### 字段

- `answer`
- `retrievalSatisfied`

### 这是一个非常典型的“流程输出 DTO”。

---

### 12.2 [RagWorkflowState.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\RagWorkflowState.java)

### 作用

状态机运行时的状态对象。

### 继承

继承自 `AgentState`

### 为什么要有这个类

LangGraph4j 的核心思路就是：

- 图中的节点不是简单函数串联
- 而是围绕“状态”不断修改和流转

### 方法

#### `question()`
- 从状态里取问题

#### `sources()`
- 从状态里取来源列表

#### `retrievalSatisfied()`
- 取检索是否满足要求

#### `answer()`
- 取最终回答

### 这里的特点

状态本质是个 `Map<String, Object>`，  
这个类只是给它包了一层类型友好的访问方法。

---

### 12.3 [RagWorkflowService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\RagWorkflowService.java)

这是当前后端第二个最关键的类。

### 作用

用 LangGraph4j 定义最小 RAG 工作流。

### 方法

#### `run(String question, List<SourceReference> sources)`

做的事：

1. 构造 graph
2. 把初始状态塞进去
3. 执行 graph
4. 取最终状态
5. 转成 `RagWorkflowResult`

#### `buildGraph()`

这是核心方法。

### 当前图结构

```text
START
  -> rewriter
  -> evaluator
      -> generator  (如果有来源)
      -> fallback   (如果没来源)
END
```

### 每个节点的作用

#### `rewriter`

当前只是把问题透传回去。  
这是未来“问题改写”能力的占位点。

#### `evaluator`

当前逻辑很简单：

- `sources` 非空 -> `retrievalSatisfied = true`
- 否则 false

这是未来“检索质量评估”能力的占位点。

#### `generator`

当前逻辑：

- 取第一条来源
- 拼成“根据知识库《xxx》的内容：yyy”

这是未来真实 LLM 生成节点的占位点。

#### `fallback`

当前逻辑：

- 返回一个“资料不足”的兜底回答

### `addConditionalEdges(...)`

这是 LangGraph4j 的关键。

它说明：

- 在 `evaluator` 执行后，不是固定走一条边
- 而是根据状态决定走 `generator` 还是 `fallback`

### 你一定要学会的点

这就是状态机和普通 service 方法最大的区别：

- 普通 service：你手写 `if else`
- 状态机：你把节点和边定义成图

这是 RAG 流程逐渐复杂后非常值得用的方式。

---

## 13. 配置文件

### 13.1 [application.yml](D:\work\java\spring-rag\backend\src\main\resources\application.yml)

### 作用

默认运行配置。

### 关键配置

- `server.port: 8089`
- `app.rag.embedding.mode: local`
- `app.rag.store.mode: inmemory`
- Chroma 默认连接参数

### 说明

虽然默认 `storeMode` 还是 `inmemory`，但 Chroma 参数已经配好了。

---

### 13.2 [application-chroma.yml](D:\work\java\spring-rag\backend\src\main\resources\application-chroma.yml)

### 作用

开启 `chroma` profile 时，把向量存储切到 Chroma。

### 内容

```yml
app:
  rag:
    store:
      mode: chroma
```

### 这就是 profile 覆盖配置的典型用法。

---

### 13.3 [src/test/resources/application.yml](D:\work\java\spring-rag\backend\src\test\resources\application.yml)

### 作用

测试环境专用配置。

### 当前测试模式

- `embedding.mode = simple`
- `store.mode = inmemory`

### 为什么这么配

因为测试追求：

- 快
- 稳
- 不依赖外部服务

---

## 14. 测试层怎么读

测试层不是附属品，它其实是最好的“功能说明书”。

### 14.1 [SmokeTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\SmokeTest.java)

### 作用

验证 Spring 上下文能正常启动。

### 方法

#### `contextLoads()`

只要项目启动没炸，这个测试就通过。

---

### 14.2 [HealthControllerTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\api\HealthControllerTest.java)

### 作用

验证健康检查接口。

### 方法

#### `shouldReturnOkHealthStatus()`

断言：

- HTTP 200
- `status == UP`

---

### 14.3 [RagStatusControllerTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\api\RagStatusControllerTest.java)

### 作用

验证 RAG 状态接口。

### 方法

#### `shouldExposeCurrentRagModes()`

断言：

- `embeddingMode == simple`
- `storeMode == inmemory`
- `chroma.baseUrl == http://localhost:8000`

注意这里会读测试配置。

---

### 14.4 [ChatControllerTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\api\chat\ChatControllerTest.java)

### 作用

验证聊天接口返回的是 SSE。

### 方法

#### `shouldReturnServerSentEvents()`

断言响应头里有：

- `text/event-stream`

---

### 14.5 [ChatRagIntegrationTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\api\chat\ChatRagIntegrationTest.java)

### 作用

验证“上传文档 -> 聊天检索命中”的最小闭环。

### 方法

#### `shouldUseUploadedKnowledgeWhenAnsweringQuestion()`

步骤：

1. 先上传文档
2. 再发聊天请求
3. 断言回答里有文档内容
4. 断言回答里有文件名

这是非常有价值的一条集成测试。

---

### 14.6 [DocumentControllerTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\api\document\DocumentControllerTest.java)

### 作用

验证文档上传接口。

### 方法

#### `shouldAcceptDocumentUpload()`

断言上传成功后状态是：

- `INDEXED`

---

### 14.7 [InMemorySessionStateRepositoryTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\application\chat\InMemorySessionStateRepositoryTest.java)

### 作用

验证会话仓库能保存和读取状态。

### 方法

#### `shouldSaveAndLoadSessionState()`

说明：

- 先 save
- 再 load
- 再断言消息数正确

---

### 14.8 [KeywordHandoffPolicyTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\application\chat\KeywordHandoffPolicyTest.java)

### 作用

验证人工接管规则。

### 方法

#### `shouldSuggestHandoffWhenMessageContainsNegativeKeyword()`

只要命中负面关键词就应该返回 `true`

---

### 14.9 [RagWorkflowServiceTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\application\chat\RagWorkflowServiceTest.java)

### 作用

验证状态机工作流分支。

### 方法

#### `shouldUseGeneratorBranchWhenSourcesExist()`

有来源时应走 `generator`

#### `shouldUseFallbackBranchWhenSourcesAreEmpty()`

无来源时应走 `fallback`

---

### 14.10 [InMemoryKnowledgeBaseServiceTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\application\document\InMemoryKnowledgeBaseServiceTest.java)

### 作用

验证知识库存储与检索。

### 方法

#### `shouldStoreDocumentAndRetrieveRelevantChunk()`

验证：

- 文档能入库
- 查询能命中相关片段

#### `shouldReturnAtMostThreeRelevantChunks()`

验证：

- 检索结果最多 3 条

---

### 14.11 [EmbeddingServiceConfigurationTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\infrastructure\document\EmbeddingServiceConfigurationTest.java)

### 作用

验证 embedding 配置切换逻辑。

### 方法

#### `shouldCreateSimpleEmbeddingServiceWhenModeIsSimple()`

说明配置 `simple` 时，应该返回 `SimpleTextEmbeddingService`

---

### 14.12 [EmbeddingServiceFactoryTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\infrastructure\document\EmbeddingServiceFactoryTest.java)

### 作用

验证 embedding service 的 fallback。

### 方法

#### `shouldFallbackToSimpleEmbeddingServiceWhenLocalModelCreationFails()`

模拟真实模型初始化失败后，应回退到 simple。

---

### 14.13 [EmbeddingStoreConfigurationTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\infrastructure\document\EmbeddingStoreConfigurationTest.java)

### 作用

验证 store 配置切换逻辑。

### 方法

#### `shouldCreateInMemoryEmbeddingStoreWhenModeIsInMemory()`

说明配置 `inmemory` 时，应该返回 `InMemoryEmbeddingStore`

---

### 14.14 [EmbeddingStoreFactoryTest.java](D:\work\java\spring-rag\backend\src\test\java\com\example\springrag\infrastructure\document\EmbeddingStoreFactoryTest.java)

### 作用

验证 Chroma store fallback。

### 方法

#### `shouldFallbackToInMemoryEmbeddingStoreWhenChromaCreationFails()`

模拟 Chroma 初始化失败后，应回退内存 store。

---

## 15. 这套代码的主调用链

你可以把一次聊天请求理解成下面这条链路：

1. 前端调 `POST /api/chat/stream`
2. [ChatController.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\api\chat\ChatController.java)
3. [StubChatService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\chat\StubChatService.java)
4. [InMemorySessionStateRepository.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\chat\InMemorySessionStateRepository.java) 读取会话
5. [InMemoryKnowledgeBaseService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\document\InMemoryKnowledgeBaseService.java) 检索来源
6. [RagWorkflowService.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\application\chat\RagWorkflowService.java) 走状态机
7. [KeywordHandoffPolicy.java](D:\work\java\spring-rag\backend\src\main\java\com\example\springrag\infrastructure\chat\KeywordHandoffPolicy.java) 判断是否转人工
8. 保存会话状态
9. SSE 推送回答和来源

如果你能把这 9 步讲清楚，说明你已经真正看懂这个后端了。

---

## 16. 现在最值得你重点学习的 8 个点

### 1. Spring Boot 的分层结构

看懂：

- Controller
- Service 接口
- Infrastructure 实现
- Domain 对象

### 2. 构造器注入

项目里大多数依赖都是通过构造方法注入的，这比字段注入更推荐。

### 3. `record` 的使用

像 `ChatStreamRequest`、`DocumentRecord`、`RagWorkflowResult` 都很适合用 `record`。

### 4. SSE

这个项目已经给你一个很好的 SSE 示例。

### 5. 配置驱动实现切换

通过 `application.yml` 切换：

- embedding 模式
- store 模式

### 6. 工厂 + 回退机制

`EmbeddingServiceFactory` 和 `EmbeddingStoreFactory` 非常值得学。

### 7. 向量检索最小实现

`InMemoryKnowledgeBaseService` 展示了：

- 文档切片
- embedding
- 索引
- 检索

### 8. 状态机思想

`RagWorkflowService` 是你理解 LangGraph4j 最好的起点。

---

## 17. 如果你接下来要自己继续读，建议怎么做

### 第一遍

只看这些文件：

- `SpringRagApplication`
- `ChatController`
- `StubChatService`
- `InMemoryKnowledgeBaseService`
- `RagWorkflowService`

目标：

- 搞懂一次聊天请求怎么流动

### 第二遍

看这些文件：

- `RagProperties`
- `EmbeddingServiceConfiguration`
- `EmbeddingStoreConfiguration`
- `EmbeddingServiceFactory`
- `EmbeddingStoreFactory`

目标：

- 搞懂为什么这个项目可以灵活切换底层实现

### 第三遍

看所有测试类。

目标：

- 用测试反过来理解功能

---

## 18. 你现在可以尝试做的练习

为了让你真正掌握这套代码，我建议你自己动手做 3 个小练习：

### 练习 1

把 `KeywordHandoffPolicy` 的规则改成：

- 包含“投诉”返回 true
- 连续失败次数 >= 3 才转人工

然后补对应测试。

### 练习 2

把 `splitIntoChunks` 从“按行切片”改成“每 2 行合成一个 chunk”。

### 练习 3

给 `RagWorkflowService` 增加一个新的节点，比如 `retriever` 占位节点。

这样你会更快看懂状态机怎么扩展。

---

## 19. 最后给你的阅读提醒

你现在 1 年 Java 经验，看这套代码时不要担心“我是不是应该一次全懂”。  
这个项目已经同时涉及了：

- Spring Boot
- SSE
- 配置绑定
- LangChain4j
- Chroma
- LangGraph4j
- 测试

本来就不是一个能 10 分钟全吃透的项目。

你真正应该先拿下的是这 3 件事：

1. 一次聊天请求怎么走完整条链路
2. embedding 和 store 为什么要抽接口 + 配置切换
3. 状态机为什么比 if-else 更适合复杂 RAG 流程

你把这 3 件事掌握了，这个 backend 你就已经读懂大半了。
