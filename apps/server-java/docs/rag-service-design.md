# RAG Service Java Migration - Implementation Summary

## 1. 修改/新增文件清单

### 新增文件

| 文件路径 | 说明 |
|---------|------|
| `services/rag-service/src/main/java/com/ai/rag/store/QdrantEmbeddingStore.java` | Qdrant LangChain4j EmbeddingStore 适配器 |
| `services/rag-service/src/main/java/com/ai/rag/service/ChunkingService.java` | 文档分块服务 |
| `services/rag-service/src/main/java/com/ai/rag/controller/HealthController.java` | 健康检查和信息服务 |

### 修改文件

| 文件路径 | 修改说明 |
|---------|---------|
| `services/rag-service/src/main/java/com/ai/rag/service/VectorSearchService.java` | 改为使用 QdrantEmbeddingStore |
| `services/rag-service/src/main/java/com/ai/rag/service/DocumentService.java` | 集成 ChunkingService 和完整 CRUD |
| `services/rag-service/src/main/java/com/ai/rag/service/RagChatService.java` | 完善 RAG 对话逻辑 |
| `services/rag-service/src/main/java/com/ai/rag/repository/DocumentRepository.java` | 实现完整内存文档仓库 |
| `services/rag-service/src/main/java/com/ai/rag/controller/RagChatController.java` | 对齐 Python API 端点 |
| `services/rag-service/src/main/java/com/ai/rag/controller/DocumentController.java` | 对齐 Python API 端点 |
| `services/rag-service/src/main/java/com/ai/rag/config/QdrantConfig.java` | 配置 QdrantEmbeddingStore Bean |
| `services/rag-service/src/main/java/com/ai/rag/config/LangChain4jConfig.java` | 更新 embedding 配置 |
| `services/rag-service/src/main/java/com/ai/rag/config/ChatModelConfig.java` | 支持多 LLM Provider |
| `services/rag-service/src/main/resources/application.yml` | 完善配置项 |
| `gateway/src/main/java/com/ai/gateway/agent/RagAgent.java` | 实现真正的 RAG Agent |
| `services/rag-service/build.gradle.kts` | 添加 Qdrant 依赖 |

### 删除文件

| 文件路径 | 删除原因 |
|---------|---------|
| `services/rag-service/src/main/java/com/ai/rag/config/ChatClientConfig.java` | 与 ChatModelConfig 重复 |

---

## 2. 核心设计说明

### 2.1 Qdrant 集成方式

**架构选择：自定义 EmbeddingStore 适配器**

```
┌─────────────────────────────────────────────────────────┐
│                    LangChain4j                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │          EmbeddingStore<TextSegment>            │   │
│  └─────────────────────────────────────────────────┘   │
│                         │                               │
│                         ▼                               │
│  ┌─────────────────────────────────────────────────┐   │
│  │      QdrantEmbeddingStore (自定义实现)            │   │
│  │  - 实现 EmbeddingStore 接口                      │   │
│  │  - 封装 QdrantClient                            │   │
│  │  - 支持 metadata 存储                            │   │
│  └─────────────────────────────────────────────────┘   │
│                         │                               │
└─────────────────────────┼───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                      Qdrant                             │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │  Collection  │  │   Points     │  │  Indexes    │  │
│  │  documents   │  │  (vectors)   │  │             │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**关键设计决策：**

1. **不直接使用 langchain4j-qdrant**：因为 langchain4j 1.5.0 的 Qdrant 集成存在 bug，我们创建了自定义适配器
2. **使用 Qdrant gRPC 客户端**：通过 `io.qdrant:qdrant-client` 直接操作 Qdrant
3. **Lazy 初始化**：Collection 在第一次操作时才创建
4. **元数据支持**：在 payload 中存储 `doc_id`、`filename`、`created_at` 等

**QdrantEmbeddingStore 核心方法：**

```java
// 添加向量
String add(Embedding embedding, TextSegment segment)

// 批量添加
List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments)

// 向量搜索
EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request)

// 按文档 ID 删除
void deleteByDocId(String docId)

// 获取统计
Map<String, Object> getStats()
```

### 2.2 分块策略

**配置参数：**

```yaml
rag:
  chunk-size: 500      # 目标分块大小（字符数）
  chunk-overlap: 50   # 分块重叠大小
```

**分块算法：**

```
原始文本
    │
    ▼
┌─────────────────────────────────────────────────────┐
│                  ChunkingService                     │
│                                                     │
│  while (start < textLength):                        │
│    ┌────────────────────────────────────────────┐   │
│    │ 1. 提取 chunkSize 大小的文本片段            │   │
│    └────────────────────────────────────────────┘   │
│    │                                              │   │
│    ▼                                              │   │
│  ┌────────────────────────────────────────────┐   │
│  │ 2. 在自然边界处截断                           │   │
│  │    优先级: \n\n > \n > . > ? > ! > ; > ,    │   │
│  └────────────────────────────────────────────┘   │
│    │                                              │   │
│    ▼                                              │   │
│  ┌────────────────────────────────────────────┐   │
│  │ 3. 保存 chunk                               │   │
│  └────────────────────────────────────────────┘   │
│    │                                              │   │
│    ▼                                              │   │
│  ┌────────────────────────────────────────────┐   │
│  │ 4. 移动起始位置 (chunkSize - overlap)        │   │
│  └────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
    │
    ▼
分块列表
```

**特点：**

1. **智能断点**：在句子/段落边界处断句，而非简单字符切割
2. **重叠机制**：相邻分块有 50 字符重叠，保持上下文连续性
3. **最小分块**：自动过滤空白分块

### 2.3 DocumentRepository 设计

**内存存储架构：**

```
DocumentRepository
    │
    ├── Map<UUID, DocumentRecord> documents
    │       │
    │       └── DocumentRecord(Document, DocumentStatus)
    │
    └── DocumentStatus enum
            ├── PENDING    // 文档刚上传
            ├── INDEXING   // 正在向量化
            ├── COMPLETED  // 处理完成
            └── FAILED     // 处理失败
```

**CRUD 操作：**

| 方法 | 说明 |
|-----|------|
| `save(filename, contentType, size, chunks)` | 创建文档记录 |
| `findAll(page, size)` | 分页查询 |
| `findById(id)` | 按 ID 查询 |
| `updateStatus(id, status)` | 更新状态 |
| `updateChunkCount(id, count)` | 更新分块数 |
| `deleteById(id)` | 删除文档 |

---

## 3. API 端点对照

### 3.1 Python vs Java API 对照

| 功能 | Python 端点 | Java 端点 | 状态 |
|-----|------------|----------|------|
| 上传文档 | `POST /api/rag/documents/upload` | `POST /api/rag/documents/upload` | ✅ 对齐 |
| 列出文档 | `GET /api/rag/documents/` | `GET /api/rag/documents/` | ✅ 对齐 |
| 获取文档 | `GET /api/rag/documents/{doc_id}` | `GET /api/rag/documents/{docId}` | ✅ 对齐 |
| 删除文档 | `DELETE /api/rag/documents/{doc_id}` | `DELETE /api/rag/documents/{docId}` | ✅ 对齐 |
| RAG 对话 | `POST /api/rag/chat/` | `POST /api/rag/chat/` | ✅ 对齐 |
| 流式对话 | `POST /api/rag/chat/stream` | `POST /api/rag/chat/stream` | ✅ 对齐 |
| 获取历史 | `GET /api/rag/chat/history/{session_id}` | `GET /api/rag/chat/history/{sessionId}` | ✅ 对齐 |
| 摄入文本 | `POST /api/rag/chat/ingest-text` | `POST /api/rag/chat/ingest-text` | ✅ 对齐 |
| URL 摄入 | `POST /api/rag/documents/ingest-url` | `POST /api/rag/documents/ingest-url` | 🔄 待实现 |

### 3.2 请求/响应格式

**RAG Chat Request：**

```java
public record RagChatRequest(
    @NotBlank String query,      // 查询内容
    String session_id,           // 会话 ID
    Integer top_k,              // Top-K 结果数
    Double temperature,          // 温度参数
    String[] doc_ids             // 文档过滤
) {}
```

**RAG Chat Response：**

```java
public record ChatResponse(
    String answer,               // 回答内容
    String session_id,           // 会话 ID
    List<SourceDocument> sources, // 来源文档
    List<String> source_texts   // 来源文本
) {}
```

**Document Upload Response：**

```java
public record UploadResponse(
    String doc_id,               // 文档 ID
    String filename,             // 文件名
    int chunks,                 // 分块数
    String status                // 状态
) {}
```

---

## 4. Gateway 框架集成点

### 4.1 Agent 接口实现

RagAgent 实现了 `com.ai.common.agent.Agent` 接口：

```java
@Component
public class RagAgent implements Agent {
    private final RagChatService ragChatService;

    @Override
    public String name() {
        return "RagAgent";
    }

    @Override
    public AgentType type() {
        return AgentType.RAG;
    }

    @Override
    public Mono<AgentResponse> process(AgentRequest request) {
        // 调用 RAG 服务处理请求
    }
}
```

### 4.2 AgentRegistry 自动注册

由于 RagAgent 使用 `@Component` 注解，Spring 会自动将其注入到 AgentRegistry：

```java
@Component
public class AgentRegistry {
    public AgentRegistry(List<Agent> agentList) {
        // agentList 包含所有 Agent 实现，包括 RagAgent
        this.agents = agentList.stream()
            .collect(Collectors.toMap(Agent::type, Function.identity()));
    }
}
```

### 4.3 请求流程

```
┌─────────────────────────────────────────────────────────────┐
│                       Gateway                                │
│                                                              │
│  AgentController                                             │
│       │                                                      │
│       ▼                                                      │
│  AgentRegistry.getAgent(AgentType.RAG)                       │
│       │                                                      │
│       ▼                                                      │
│  RagAgent.process(AgentRequest)                              │
│       │                                                      │
│       ├── ragChatService.chat(query, topK)                   │
│       │       │                                              │
│       │       ├── vectorSearchService.searchSimilar()        │
│       │       │       │                                      │
│       │       │       └── QdrantEmbeddingStore.search()      │
│       │       │                                              │
│       │       └── chatModel.chat()                           │
│       │              │                                        │
│       │              └── LLM Response                         │
│       │                                                      │
│       ▼                                                      │
│  AgentResponse (with sources)                                 │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 集成配置

**Gateway application.yml：**

```yaml
# Gateway 会自动发现并注册 RagAgent
# 无需额外配置
```

**RAG Service 独立运行配置：**

```yaml
# rag-service/src/main/resources/application.yml
server:
  port: 9001

rag:
  qdrant:
    host: localhost
    port: 6333
    collection-name: documents
  llm:
    provider: openai
    model-name: gpt-4o
```

---

## 5. 环境变量配置

| 变量 | 默认值 | 说明 |
|-----|-------|------|
| `QDRANT_HOST` | localhost | Qdrant 服务器地址 |
| `QDRANT_PORT` | 6333 | Qdrant gRPC 端口 |
| `QDRANT_COLLECTION` | documents | Collection 名称 |
| `QDRANT_API_KEY` | - | Qdrant API Key（可选） |
| `EMBEDDING_DIMENSION` | 384 | Embedding 维度 |
| `EMBEDDING_MODEL` | all-MiniLM-L6-v2 | HuggingFace 模型 |
| `LLM_PROVIDER` | openai | LLM 提供商 |
| `LLM_MODEL` | gpt-4o | LLM 模型名称 |
| `OPENAI_API_KEY` | - | OpenAI API Key |
| `LLM_BASE_URL` | - | OpenAI 兼容 API 地址 |
| `CHUNK_SIZE` | 500 | 文档分块大小 |
| `CHUNK_OVERLAP` | 50 | 分块重叠大小 |

---

## 6. 依赖关系

```
common (API 定义)
    │
    └── Agent, AgentRequest, AgentResponse, AgentType

gateway (Web 层)
    │
    ├── AgentRegistry
    ├── AgentController
    └── RagAgent (实现) ──────────────────┐
                                          │
services/rag-service (RAG 核心)           │
    │                                     │
    ├── QdrantConfig                      │
    │       │                             │
    │       └── QdrantEmbeddingStore ─────┘ (使用)
    │             │
    │             └── QdrantClient (io.qdrant)
    │
    ├── LangChain4jConfig
    │       │
    │       └── EmbeddingModel (HuggingFace/OpenAI)
    │
    ├── ChatModelConfig
    │       │
    │       └── ChatLanguageModel (OpenAI/Ollama)
    │
    ├── VectorSearchService
    ├── DocumentService
    ├── RagChatService
    ├── ChunkingService
    ├── DocumentRepository
    └── Controllers (REST API)
```
