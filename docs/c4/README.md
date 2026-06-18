# C4 模型文档

使用 PlantUML 绘制的 C4 架构模型，描述 AI Chat & Agent Platform 的完整架构。

## 文件


| 文件                           | 层级  | 说明                                            |
| ---------------------------- | --- | --------------------------------------------- |
| `C1-Context.puml`            | C1  | 系统上下文图                                        |
| `C2-Container.puml`          | C2  | 容器图（3 个子域：Chat / RAG / Tool Calling）          |
| `C3-Component-Backend.puml`  | C3  | 后端组件图（Interface / Domain / Infrastructure 三层） |
| `C3-Component-Frontend.puml` | C3  | 前端组件图（App / Pages / Agents / Core / Shared）   |
| `C4-Deployment.puml`         | C4  | 本地开发环境部署图（端口 9000）                         |


## 技术栈

### 后端 (AI Platform Backend)

- **运行时**: Spring Boot 4.0 / Java 25 / Spring AI 2.0
- **端口**: **9000** (统一)
- **子域**: Chat Domain / RAG Domain / Tool Calling Domain
- **向量库**: PostgreSQL + pgvector (端口 5432)
- **外部服务**: DeepSeek API (LLM + Embedding) / Ollama (本地 Embedding, 端口 11434)

### 前端 (Web Frontend)

- **框架**: Angular 22 + TypeScript
- **路由**: AI Infra / RAG / Vision / AI Hubs
- **端口**: 4200 (dev server, proxy `/api` → `:8080`)

## 功能模块

### Chat Domain

- `AiController`: 聊天 HTTP/SSE 请求
- `ChatSession`: 聊天会话聚合根
- `ChatMessage`: 聊天消息实体
- `AiChatService`: AI 对话核心服务
- `InMemoryChatSessionRepository`: 内存会话存储

### RAG Domain

- `RagController`: RAG HTTP/SSE 请求
- `Document`: 文档实体
- `DocumentChunk`: 文档分块实体
- `RagService`: RAG 服务
- `OllamaEmbeddingAdapter` / `MockEmbeddingAdapter`: Embedding 适配器
- `PgVectorAdapter`: 向量检索适配器
- `JpaDocumentRepository`: JPA 文档仓储

### Tool Calling Domain

- `ToolCallingController`: 工具调用 HTTP 请求
- `RagSearchTool`: RAG 搜索工具
- `WeatherTools`: 天气查询工具

## 部署端口汇总


| 服务                    | 端口       |
| --------------------- | -------- |
| Spring Boot Backend   | **8080** |
| PostgreSQL + pgvector | 5432     |
| Ollama (Embedding)    | 11434    |
| Angular Dev Server    | 4200     |
| DeepSeek API          | HTTPS    |


## 查看

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- `plantuml -o png *.puml`

