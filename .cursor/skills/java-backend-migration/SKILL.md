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

## 小步迁移指南（Incremental Migration）

> 本部分用于约束“一次只改一个服务、一步只切一个接口”，降低迁移风险，并为每步建立可复制的验证闭环。

### 核心原则

1. **按服务拆分迁移单元，不要按功能点拆分**：把“Python 服务 -> Java 服务”视为最小迁移单元。
2. **迁移服务前必须保证该服务的所有端点在 Java 侧已经等价实现**。
3. **每完成一个服务，必须完成健康检查、接口契约、业务路径的 MCP 验证，再继续下一服务**。
4. **禁止在一次提交中同时修改多个服务的路由、配置或数据模型**。
5. **遇到失败，优先回滚最近一个迁移单元，而不是继续叠加变更**。

### 逐服务迁移路径

建议按以下顺序进行：

1. `services/rag` -> Java `rag-service`
2. `services/tts-service` -> Java `tts-service`
3. `services/vision-service` -> Java `vision-service`
4. `services/ai_agents` -> Java `ai-agents-service`
5. `services/text-service` -> Java `text-service`

每个服务遵循同一套“小步模板”：

```
Step 0: 基线确认
Step 1: 环境与项目初始化
Step 2: ���心 DTO / 领域对象迁移
Step 3: 单接口迁移与 MCP 验证
Step 4: 完整业务路径迁移
Step 5: 配置与部署兼容
Step 6: 切流与回滚方案
```

### Step 0: 基线确认

在开始迁移前，先冻结以下信息：

| 项目 | 目标 |
|------|------|
| Python 服务健康状态 | 所有接口 `/health` 正常 |
| 接口契约 | 每个 endpoint 的请求/响应示例 |
| 依赖版本 | Python 依赖与外部服务版本 |
| 测试入口 | 自动化脚本或 MCP 场景可复现 |

### Step 1: 环境与项目初始化

只做以下工作：

- 初始化 Spring Boot 项目
- 配置 `build.gradle.kts`
- 创建基础目录结构
- 配置本地启动参数

**验证点**：Java 服务能启动，且 `/health` 返回 `{"status":"degraded"}` 或明确缺少依赖的状态。

### Step 2: 核心 DTO / 领域对象迁移

将 Python `schemas.py` 和 `domain/` 中的类型迁移为 Java `record` / `@Entity`。

**验证点**：
- Java 侧 DTO 可以序列化/反序列化
- 与 Python 侧的 JSON 结构保持一致

**MCP 验证**：在服务日志或内存中做一次 JSON 结构对比。

### Step 3: 单接口迁移与 MCP 验证

对每个 endpoint 执行：

1. 先在 Java 侧实现对应 Controller + Service + Adapter
2. 在同一端口或不同端口启动新旧服务
3. 使用 MCP 浏览器工具或 curl 进行对照调用
4. 仅当这一步验证通过，再进入下一个 endpoint

**推荐验证流程**：

```
1. 启动 Python 服务
2. 启动 Java 服务
3. 使用 MCP browser_take_screenshot / browser_snapshot 查看服务页面
4. 使用 curl 或 MCP browser_navigate 调用接口
5. 对比响应内容
6. 记录截图和结果到 docs/screenshots/
```

### Step 4: 完整业务路径迁移

当所有单接口验证通过后，执行：

- 端到���业务流程（如：上传文档 -> 检索 -> 问答）
- 错误路径（空参数、超时、外部依赖不可用）
- 并发与性能基线对比

**验证点**：至少覆盖 happy path + 2 个异常场景。

### Step 5: 配置与部署兼容

- 环境变量映射
- 配置中心接入
- Dockerfile / 启动脚本
- 健康检查路径保持 `/health`

**验证点**：Java 服务在 staging 环境健康检查通过。

### Step 6: 切流与回滚方案

推荐采用“灰度验证 + 双写观察”：

1. 先在 API Gateway 或 Nginx 增加 Java 服务路由
2. 使用流量比例逐步切流（例如 5% -> 20% -> 100%）
3. 每阶段通过 MCP 浏览器工具和日志观察稳定性
4. 出现问题立即切回 Python 服务

**回滚条件**：
- 健康检查失败
- 错误率升高
- 关键业务路径验证失败

### 验证清单（逐服务）

| 服务 | 接口 | 对照验证 | MCP 验证 |
|------|------|---------|---------|
| `rag` | `/api/rag/documents/upload` | 上传文档并检索 | 截图 + 日志 |
| `rag` | `/api/rag/chat` | RAG 问答结果一致 | 截图 + JSON 对比 |
| `tts` | `/api/tts/synthesize` | 音频可播放 | 控制台网络请求 |
| `tts` | `/api/tts/voices` | 语音列表一致 | 截图 |
| `vision` | `/api/vision/detect` | 目标检测结果一致 | 截图 |
| `vision` | `/api/vision/caption` | 图像描述一致 | 截图 |
| `ai_agents` | `/api/agents/chat` | 路由与回答一致 | 截图 |
| `ai_agents` | `/api/agents/health` | 子 Agent 健康 | 截图 |

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
