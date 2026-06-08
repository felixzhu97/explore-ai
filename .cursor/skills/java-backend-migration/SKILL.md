---
name: java-backend-migration
description: 迁移后端从 Python/FastAPI 微服务迁移到 Java 最新稳定版本和 Spring Boot。包含 Spring Boot 项目搭建、Python 微服务（AI Agents、RAG、TTS、Vision）Java 重写指南。
---

# 后端迁移指南

## 迁移范围

| 当前技术栈 | 目标技术栈 |
|-----------|-----------|
| Python/FastAPI (`services/ai_agents`) | Java / Spring Boot 3.4 + LangChain4j |
| Python/FastAPI (`services/rag`) | Spring Boot + LangChain4j + 向量数据库 |
| Python/FastAPI (`services/tts-service`) | Spring Boot + TTS SDK |
| Python/FastAPI (`services/vision-service`) | Spring Boot + AI/ML SDK |
| Python/FastAPI (`services/text-service`) | Spring Boot + LLM SDK |
| Python/FastAPI (`services/media-gen`) | Spring Boot + Media SDK |

## 项目结构

当前 Java 后端已迁移到 `apps/server-java/`：

```
apps/server-java/
├── build.gradle.kts
├── settings.gradle.kts
├── common/
│   ├── build.gradle.kts
│   └── src/main/java/com/ai/common/
│       ├── dto/
│       └── exception/
├── gateway/
│   ├── build.gradle.kts
│   └── src/main/java/com/ai/
│       ├── GatewayApplication.java
│       └── config/
└── services/
    └── rag-service/
        ├── build.gradle.kts
        └── src/main/java/com/ai/rag/
            ├── RagServiceApplication.java
            ├── config/
            ├── controller/
            ├── model/
            ├── repository/
            └── service/
```

## Part 1: RAG Service 迁移

### 技术栈
- Spring Boot 3.4 + WebFlux
- LangChain4j 1.5.0
- Qdrant 向量数据库
- HuggingFace Embedding (sentence-transformers)

### 端口配置
- Gateway: 9000
- RAG Service: 9001

### 依赖配置

```kotlin
// apps/server-java/services/rag-service/build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.projectreactor:reactor-core:3.7.0")

    implementation("dev.langchain4j:langchain4j-core:1.5.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.5.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.5.0")
    implementation("dev.langchain4j:langchain4j-qdrant:1.5.0")
    implementation("dev.langchain4j:langchain4j-hugging-face:1.5.0")

    implementation(project(":common"))
}
```

### 核心实现

```java
// ChatModel 配置
@Configuration
public class ChatModelConfig {
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o")
            .build();
    }
}

// RAG Chat Service
@Service
public class RagChatService {
    public Flux<String> streamChat(RagChatRequest request) {
        // 1. 向量检索
        // 2. 构建 prompt
        // 3. LLM 流式生成
        return Flux.from(chatModel.chat(prompt));
    }
}
```

## Part 2: AI Agents Service 迁移

### 技术栈
- Spring Boot + LangChain4j
- Supervisor Agent + 子 Agent 模式

### 端口配置
- AI Agents: 8003 (Python) / 待迁移到 Java

## Part 3: TTS Service 迁移

### 技术栈
- Spring Boot + Azure/Google Speech SDK

### 端口配置
- TTS: 8013

## Part 4: Vision Service 迁移

### 技术栈
- Spring Boot + YOLO/Stable Diffusion

### 端口配置
- Vision: 8000

## 小步迁移指南（Incremental Migration）

### 核心原则

1. **按服务拆分迁移单元**：把“Python 服务 -> Java 服务”视为最小迁移单元。
2. **迁移服务前必须保证该服务的所有端点在 Java 侧已经等价实现**。
3. **每完成一个服务，必须完成健康检查、接口契约、业务路径的 MCP 验证，再继续下一服务**。
4. **禁止在一次提交中同时修改多个服务的路由、配置或数据模型**。
5. **遇到失败，优先回滚最近一个迁移单元，而不是继续叠加变更**。

### 迁移顺序

1. `services/rag` -> Java `rag-service` (Port 9001) ✅ 已完成框架
2. `services/tts-service` -> Java `tts-service` (Port 9002)
3. `services/vision-service` -> Java `vision-service` (Port 9003)
4. `services/ai_agents` -> Java `ai-agents-service` (Port 9004)
5. `services/text-service` -> Java `text-service` (Port 9005)
6. `services/media-gen` -> Java `media-gen-service` (Port 9006)

### 验证清单（逐服务）

| 服务 | 接口 | 验证方式 |
|------|------|---------|
| `rag` | `/api/rag/documents/upload` | curl + 向量检索 |
| `rag` | `/api/rag/chat/stream` | SSE 流测试 |
| `tts` | `/api/tts/synthesize` | 音频生成 |
| `vision` | `/api/vision/detect` | 目标检测 |
| `ai_agents` | `/api/agents/chat` | 路由与回答 |
| `text` | `/api/text/chat` | 文本生成 |
| `media-gen` | `/image/generate` | 图像生成 |

### 常见迁移失败与处理

| 现象 | 可能原因 | 建议处理 |
|------|---------|---------|
| 健康检查通过，接口返回 500 | 依赖未初始化 | 检查 Qdrant / LLM 连接 |
| 响应 JSON 结构不一致 | DTO 字段名/类型不匹配 | 回滚到最近一个 DTO 迁移步骤 |
| 性能低于 Python 侧 | 连接池或线程配置问题 | 检查 HikariCP / 异步配置 |
| 外部依赖超时 | 网络或 SDK 版本问题 | 使用 curl 单独验证依赖 |

---

## 验证清单

- [ ] 所有 Python/FastAPI 服务已迁移到 Java/Spring Boot
- [ ] AI Agents Supervisor 正确路由到子 Agent
- [ ] RAG 文档检索和向量存储正常工作
- [ ] TTS 多 Provider 支持
- [ ] Vision 图像识别和生成
- [ ] 统一 API 网关正确转发请求
- [ ] 单元测试覆盖核心 Service
- [ ] 集成测试覆盖 Controller
- [ ] 各服务健康检查端点正常
