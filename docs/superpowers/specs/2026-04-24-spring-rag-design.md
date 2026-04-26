# Spring RAG 全栈骨架设计文档

## 1. 目标

本项目一期目标是交付一个可运行、可扩展的企业级 AI 客服骨架，采用单仓前后端分目录方案：

- `backend/`：Spring Boot 后端，负责聊天编排、会话状态、知识库接口、SSE 输出。
- `frontend/`：最小聊天前端，负责消息输入、流式展示、线程标识管理、来源占位展示。

一期聚焦“骨架可运行”而不是“能力全部做深”。因此 RAG、人工接管、状态持久化先提供清晰接口和可替换占位实现，确保后续可以平滑替换为 LangChain4j、LangGraph4j、向量数据库和数据库持久层。

## 2. 范围

### 2.1 一期包含

- Spring Boot 3 + Java 17 的后端工程骨架
- 聊天 SSE 接口
- 会话状态模型与内存版持久层
- RAG 编排服务接口与占位实现
- 文档上传接口与占位实现
- 人工接管判定接口与占位实现
- 最小前端聊天页面
- 流式回答展示
- `threadId` 本地持久化
- 回答来源区域占位

### 2.2 一期不包含

- 真实 PDF/Markdown 解析与切片
- 真实向量数据库接入
- 真实 LangGraph 状态机编排
- 真实大模型调用
- 真实人工客服通知通道
- 生产级鉴权、限流、多租户隔离

## 3. 架构方案

### 3.1 总体结构

采用单仓全栈结构：

- `backend/`
  - `api`：控制器、请求响应对象
  - `application`：聊天编排、上传编排
  - `domain`：会话、消息、来源、人工接管等核心模型
  - `infrastructure`：内存仓库、占位实现、配置
- `frontend/`
  - `src/components`：聊天窗口、输入框、消息列表
  - `src/services`：后端 API / SSE 调用
  - `src/types`：前端数据类型

### 3.2 运行流

1. 前端生成或复用 `threadId`。
2. 用户发起聊天请求。
3. 后端读取会话状态。
4. 编排服务执行：
   - 问题改写占位
   - 知识检索占位
   - 结果评估占位
   - 回答生成占位
   - 人工接管判定占位
5. 后端以 SSE 流式返回文本片段。
6. 会话状态写回内存仓库。
7. 前端逐步渲染消息，并展示来源占位信息。

## 4. 核心模块设计

### 4.1 聊天模块

聊天模块提供 `/api/chat/stream` SSE 接口。控制器只负责参数校验和流式输出，业务编排集中在应用服务，避免控制器承担状态机职责。

### 4.2 会话模块

会话模块围绕 `threadId` 管理消息历史、来源、最近一次接管状态。接口设计保持与未来数据库存档一致：

- `load(threadId)`
- `save(sessionState)`

一期先用内存实现，后续可替换为 MySQL/JdbcCheckpointSaver。

### 4.3 RAG 编排模块

一期不直接引入真实 LangGraph 节点执行，而是先抽象出清晰步骤接口：

- `rewriteQuestion`
- `retrieveKnowledge`
- `evaluateKnowledge`
- `generateAnswer`

这样二期接入 LangChain4j / LangGraph4j 时，只需替换实现而不是推翻 API。

### 4.4 文档模块

一期仅提供上传接口和元信息回显，保留后续扩展点：

- 文件接收
- 基础格式校验
- 返回文档 ID、文件名、状态

### 4.5 人工接管模块

一期使用简单规则引擎占位：

- 命中强负面关键词
- 连续回答失败次数达到阈值

命中后在响应中标记 `handoffSuggested=true`，不直接接入通知系统。

## 5. 接口设计

### 5.1 SSE 聊天接口

- `POST /api/chat/stream`
- 请求体：
  - `threadId`
  - `message`
- SSE 事件：
  - `chunk`：文本片段
  - `sources`：来源列表
  - `complete`：最终完成信号
  - `handoff`：建议人工接管

### 5.2 文档上传接口

- `POST /api/documents`
- `multipart/form-data`
- 返回文档基础元信息与当前状态

### 5.3 健康检查接口

- `GET /api/health`

## 6. 前端设计

前端采用最小化 Vite + React 实现，目标是快速验证完整链路而不是做复杂后台。

页面包含：

- 标题区：展示系统名称与当前线程号摘要
- 消息区：区分用户消息和 AI 消息
- 来源区：展示引用来源占位
- 输入区：发送消息并显示加载状态
- 系统提示区：展示错误与人工接管提示

前端通过 `fetch + ReadableStream` 处理 SSE 文本流，避免引入额外复杂依赖。

## 7. 数据模型

### 7.1 后端核心对象

- `ChatMessage`
  - `role`
  - `content`
  - `timestamp`
- `SourceReference`
  - `title`
  - `snippet`
- `SessionState`
  - `threadId`
  - `messages`
  - `lastSources`
  - `handoffSuggested`
- `DocumentRecord`
  - `documentId`
  - `fileName`
  - `status`

### 7.2 前端核心对象

- `ChatRequest`
- `StreamEvent`
- `ChatMessageView`
- `SourceReferenceView`

## 8. 异常与边界处理

- 空消息：直接拒绝
- 未提供 `threadId`：由前端生成，后端要求非空
- 检索为空：返回“未找到足够资料”的安全兜底文本
- SSE 中断：前端提示重试，不自动重放历史流
- 上传非法文件：返回 400

## 9. 测试策略

后端使用：

- JUnit 5
- Spring Boot Test
- MockMvc

前端使用：

- Vitest
- React Testing Library

关键测试覆盖：

- 健康检查接口
- SSE 聊天接口基础行为
- 会话状态保存与读取
- 人工接管规则判定
- 前端页面渲染
- 前端线程 ID 生成与复用

## 10. 风险与后续演进

一期的主要风险不是技术实现，而是“接口先搭好但后续实现变形”。为降低这个风险，本设计优先固定领域对象、服务接口和 SSE 协议，再在二期逐步替换底层能力。

二期建议按以下顺序演进：

1. 接入真实文档解析与切片
2. 接入真实 Embedding 与向量检索
3. 将编排服务替换为 LangGraph4j 状态机
4. 接入数据库持久化与检查点
5. 接入人工客服通知与运维监控
