---
name: java-backend-migration
description: 迁移后端从 TypeScript/Express 和 Python/FastAPI 微服务迁移到 Java 最新稳定版本 (Java 24+) 和 Spring Boot。包含 Spring Boot 项目搭建、Python 微服务（AI Agents、RAG、TTS、Vision）Java 重写指南。
---

# 后端迁移指南

## 迁移范围

| 当前技术栈 | 目标技术栈 |
|-----------|-----------|
| TypeScript/Express (`apps/server`) | Java 24 / Spring Boot 3.4 |
| Python/FastAPI (`services/ai_agents`) | Spring Boot + LangChain4j |
| Python/FastAPI (`services/rag`) | Spring Boot + Vector DB Client |
| Python/FastAPI (`services/tts-service`) | Spring Boot + TTS SDK |
| Python/FastAPI (`services/vision-service`) | Spring Boot + AI/ML SDK |
| Python/FastAPI (`services/media-gen`) | Spring Boot + Media SDK |

---

## Part 1: Python → Java 微服务迁移

### 微服务对照表

| Python 服务 | 端口 | 核心功能 | Java 实现 |
|-------------|------|---------|-----------|
| `services/ai_agents` | 8003 | Supervisor Agent + 11 子 Agent | `@Service` + `LangChain4j` |
| `services/rag` | 8001 | 文档索引 + 检索 + Chat | `@Service` + `Qdrant Client` |
| `services/tts-service` | 8002 | 多 Provider TTS | `@Service` + Azure/Google SDK |
| `services/vision-service` | 8000 | 图像识别 + 生成 | `@Service` + Spring AI |
| `services/text-service` | - | 文本处理 | `@Service` |

### 迁移检查清单

```
原项目结构                          →  Spring Boot 结构
────────────────────────────────────────────────────────────────
services/{name}/
├── src/main.py                 →  src/main/java/com/ai/{name}/
├── src/api/                    →  src/main/java/com/ai/{name}/controller/
├── src/application/            →  src/main/java/com/ai/{name}/service/
├── src/domain/                 →  src/main/java/com/ai/{name}/domain/
├── src/infrastructure/          →  src/main/java/com/ai/{name}/adapter/
├── src/schemas.py               →  src/main/java/com/ai/{name}/dto/
├── src/config.py               →  src/main/java/com/ai/{name}/config/
├── src/routers/                →  Controller 层已在上方
└── requirements.txt            →  build.gradle.kts dependencies
```

---

### 1. RAG Service (`services/rag` → Java)

#### DTO 转换

```python
# Python: schemas.py
class HealthResponse(BaseModel):
    status: str
    qdrant_connected: bool
    embedding_model: str
    llm_provider: str
```

```java
// Java: DTO
public record HealthResponse(
    String status,
    boolean qdrantConnected,
    String embeddingModel,
    String llmProvider
) {}
```

#### Controller 转换

```python
# Python: FastAPI
@router.post("/documents/upload")
async def upload_document(file: UploadFile = File(...)):
    # ... 文档上传逻辑
    return {"doc_id": doc_id, "status": "indexed"}
```

```java
// Java: Spring Boot
@RestController
@RequestMapping("/api/rag/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file) {
        DocumentUploadResponse result = documentService.uploadDocument(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(documentService.listDocuments(page, size));
    }
}
```

#### Service 转换

```python
# Python: document_loader/loader.py
class DocumentLoader:
    def __init__(self, file_path: str):
        self.file_path = file_path

    def load(self) -> list[Document]:
        if self.file_path.endswith('.pdf'):
            return self._load_pdf()
        elif self.file_path.endswith('.md'):
            return self._load_markdown()
        # ...
```

```java
// Java: Service Layer
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStoreAdapter vectorStoreAdapter;
    private final DocumentParser documentParser;

    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        // Parse document
        List<DocumentChunk> chunks = documentParser.parse(file);

        // Generate embeddings
        List<float[]> embeddings = embeddingService.embed(chunks);

        // Store in vector DB
        String docId = vectorStoreAdapter.upsert(chunks, embeddings);

        // Persist metadata
        documentRepository.save(DocumentMetadata.builder()
            .id(docId)
            .fileName(file.getOriginalFilename())
            .chunkCount(chunks.size())
            .build());

        return new DocumentUploadResponse(docId, "indexed");
    }
}
```

#### Adapter 转换 (Infrastructure)

```python
# Python: core/vector_store.py
class VectorStore:
    def __init__(self):
        self.client = QdrantClient(url=settings.QDRANT_URL)

    def search(self, query_vector, top_k=5):
        results = self.client.search(
            collection_name=self.collection_name,
            query_vector=query_vector,
            limit=top_k
        )
        return results
```

```java
// Java: Adapter Layer
@Component
@RequiredArgsConstructor
public class QdrantAdapter {

    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper;

    public List<SearchResult> search(float[] queryVector, int topK) {
        SearchResponse response = qdrantClient.search(
            SearchRequest.builder()
                .collectionName(collectionName)
                .queryVector(queryVector)
                .limit(topK)
                .build()
        );

        return response.getResults().stream()
            .map(this::toSearchResult)
            .toList();
    }

    public void upsert(String docId, List<DocumentChunk> chunks, List<float[]> vectors) {
        List<PointStruct> points = IntStream.range(0, chunks.size())
            .mapToObj(i -> PointStruct.of(
                UUID.randomUUID().toString(),
                vectors.get(i),
                Map.of(
                    "doc_id", objectMapper.valueToTree(chunks.get(i).getMetadata()),
                    "content", chunks.get(i).getContent())
            ))
            .toList();

        qdrantClient.upsert(collectionName, points);
    }
}
```

---

### 2. AI Agents Service (`services/ai_agents` → Java)

#### Supervisor Agent

```python
# Python: Supervisor Agent (LangGraph)
class SupervisorAgent:
    def __init__(self):
        self.graph = self._build_graph()

    def route(self, state: AgentState) -> str:
        intent = self.llm.classify_intent(state["messages"][-1].content)
        return intent  # 返回子 Agent 名称
```

```java
// Java: Supervisor Service
@Service
@RequiredArgsConstructor
public class SupervisorAgentService {

    private final LangChain4jService langChain;
    private final Map<String, SubAgent> subAgents;

    public AgentResponse process(String userMessage) {
        // 1. Intent Classification
        Intent intent = langChain.classifyIntent(userMessage);

        // 2. Route to Sub-Agent
        SubAgent agent = subAgents.get(intent.getAgentName());
        if (agent == null) {
            return new AgentResponse("No agent found for intent: " + intent);
        }

        // 3. Execute Agent
        return agent.execute(intent);
    }
}
```

#### 子 Agent 映射

| Python Agent | Java Service |
|-------------|-------------|
| `RagAgent` | `RagAgentService` |
| `TTSAgent` | `TtsAgentService` |
| `K8sAgent` | `KubernetesAgentService` |
| `LLMOpsAgent` | `MlOpsAgentService` |
| `AIOpsAgent` | `AIOpsAgentService` |
| `VideoAgent` | `VideoAgentService` |

```java
// Java: Sub-Agent Interface
public interface SubAgent {
    AgentResponse execute(Intent intent);
    boolean canHandle(Intent intent);
}

// Example: RAG Agent
@Service
@RequiredArgsConstructor
public class RagAgentService implements SubAgent {

    private final VectorStoreAdapter vectorStore;
    private final LlmGateway llmGateway;

    @Override
    public AgentResponse execute(Intent intent) {
        // 1. Search vector DB
        List<SearchResult> results = vectorStore.search(intent.getQuery());

        // 2. Build context
        String context = results.stream()
            .map(SearchResult::getContent)
            .collect(Collectors.joining("\n"));

        // 3. Generate response
        String response = llmGateway.generate(
            "Answer based on context: " + context + "\n\nQuestion: " + intent.getQuery()
        );

        return new AgentResponse(response, results);
    }
}
```

#### LangChain4j 集成

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.langchain4j:langchain4j:1.0.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.0.0")
}
```

```java
// Java: LangChain4j Configuration
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatModel chatModel(AppSettings settings) {
        return OpenAiChatModel.builder()
            .apiKey(settings.getOpenAiApiKey())
            .modelName(settings.getLlmModel())
            .temperature(0.7)
            .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(AppSettings settings) {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
```

---

### 3. TTS Service (`services/tts-service` → Java)

```python
# Python: Provider Factory
class TTSFactory:
    def get_provider(self, name: str) -> TTSProvider:
        providers = {
            "azure": AzureTTSProvider(),
            "google": GoogleTTSProvider(),
            "elevenlabs": ElevenLabsProvider(),
            "edge": EdgeTTSProvider(),
        }
        return providers.get(name, EdgeTTSProvider())
```

```java
// Java: TTS Provider Strategy Pattern
public interface TtsProvider {
    byte[] synthesize(String text, VoiceConfig config);
    List<Voice> listVoices();
    boolean healthCheck();
}

@Service
@RequiredArgsConstructor
public class TtsService {

    private final Map<TtsProviderType, TtsProvider> providers;
    private final AppSettings settings;

    public byte[] synthesize(SynthesizeRequest request) {
        TtsProvider provider = providers.get(settings.getTtsProvider());
        VoiceConfig config = VoiceConfig.builder()
            .voiceId(request.voiceId())
            .language(request.language())
            .build();

        return provider.synthesize(request.text(), config);
    }

    public List<Voice> listVoices() {
        TtsProvider provider = providers.get(settings.getTtsProvider());
        return provider.listVoices();
    }
}

// Provider Implementations
@Component
public class AzureTtsProvider implements TtsProvider {
    private final AzureSpeechConfig config;

    @Override
    public byte[] synthesize(String text, VoiceConfig voiceConfig) {
        // Azure Speech SDK integration
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(
            config.getApiKey(), config.getRegion()
        );
        // ... synthesis logic
    }
}
```

---

### 4. Vision Service (`services/vision-service` → Java)

```python
# Python: Vision Router
@router.post("/vision/detect")
async def detect_objects(image: UploadFile = File(...)):
    results = yolo_detector.detect(image.file)
    return {"objects": results}
```

```java
// Java: Vision Service
@Service
@RequiredArgsConstructor
public class VisionService {

    private final ObjectDetectionAdapter yoloAdapter;
    private final ImageGenerationAdapter imageGenAdapter;

    public DetectionResult detectObjects(MultipartFile image) {
        return yoloAdapter.detect(image);
    }

    public CaptionResult captionImage(MultipartFile image) {
        return visionModel.caption(image);
    }

    public ImageGenerationResult generateImage(ImageGenRequest request) {
        return imageGenAdapter.generate(request);
    }
}

// Controller
@RestController
@RequestMapping("/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;

    @PostMapping("/detect")
    public ResponseEntity<DetectionResult> detect(
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(visionService.detectObjects(image));
    }

    @PostMapping("/caption")
    public ResponseEntity<CaptionResult> caption(
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(visionService.captionImage(image));
    }
}
```

---

## Part 2: TypeScript/Express → Spring Boot 迁移

### 核心概念对照

| Express/TS | Spring Boot/Java |
|------------|-----------------|
| `Express app` | `@SpringBootApplication` |
| `router.get()` | `@GetMapping` |
| `router.post()` | `@PostMapping` |
| `middleware` | `@Filter` / AOP / Interceptor |
| `Service layer` | `@Service` |
| `Types/Interfaces` | `record` / `class` |

### 项目初始化

```bash
# 使用 Spring Initializr 创建项目
curl https://start.spring.io/starter.zip \
  -d type=gradle \
  -d language=java \
  -d bootVersion=3.4.0 \
  -d baseDir=backend \
  -d groupId=com.ai \
  -d packageName=com.ai.api \
  -d javaVersion=24 \
  -d dependencies=web,data-jpa,validation,security \
  -o backend.zip && unzip backend.zip
```

### Gradle 配置

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // AI Integration
    implementation("dev.langchain4j:langchain4j:1.0.0")

    // Vector DB
    implementation("io.qdrant:qdrant-client:1.9.0")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

---

## Part 3: 统一 API 网关

迁移后建议添加统一 API 网关：

```java
// Java: API Gateway
@SpringBootApplication
@EnableZuulProxy
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("ai-agents", r -> r.path("/api/agents/**")
                .uri("lb://ai-agents-service"))
            .route("rag", r -> r.path("/api/rag/**")
                .uri("lb://rag-service"))
            .route("tts", r -> r.path("/api/tts/**")
                .uri("lb://tts-service"))
            .route("vision", r -> r.path("/api/vision/**")
                .uri("lb://vision-service"))
            .build();
    }
}
```

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
