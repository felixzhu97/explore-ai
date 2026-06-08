---
name: python-microservices-migration
description: 将 Python/FastAPI 微服务（AI Agents、RAG、TTS、Vision）迁移到 Java/Spring Boot。包含 FastAPI 到 Spring Boot 的完整 API 映射、LangChain4j 集成、向量数据库适配器、多 Provider TTS 架构。
---

# Python 微服务迁移到 Java

## 服务概览

| 服务 | 端口 | 依赖 | 迁移目标 |
|------|------|------|---------|
| `services/ai_agents` | 8003 | LangGraph, LLM Gateway | `langchain4j` |
| `services/rag` | 8001 | Qdrant, Embedding | `qdrant-client` |
| `services/tts-service` | 8002 | Azure, Google, ElevenLabs | Azure/Google SDK |
| `services/vision-service` | 8000 | YOLO, Stable Diffusion | Spring AI |
| `services/text-service` | - | Text processing | `@Service` |

---

## 迁移对照表

### FastAPI → Spring Boot

| FastAPI | Spring Boot |
|---------|-------------|
| `@router.post()` | `@PostMapping` |
| `async def endpoint()` | `@Async` + `CompletableFuture` |
| `UploadFile = File(...)` | `MultipartFile file` |
| `Query(param)` | `@RequestParam` |
| `Path(param)` | `@PathVariable` |
| `Body(param)` | `@RequestBody` |
| `Depends()` | `@Autowired` |
| `HTTPException` | `@ExceptionHandler` |
| `BackgroundTasks` | `@Async` + `@EventListener` |

---

## Part 1: RAG Service 迁移

### 项目结构

```
services/rag/src/
├── main.py                    →  RagServiceApplication.java
├── api/
│   ├── documents.py           →  DocumentController.java
│   └── chat.py               →  ChatController.java
├── core/
│   ├── vector_store.py       →  QdrantAdapter.java
│   ├── embedding.py         →  EmbeddingService.java
│   └── llm_gateway.py       →  LlmGateway.java
├── document_loader/           →  DocumentParser.java
├── persistence/               →  Repository + JPA
└── config.py                 →  RagConfig.java
```

### Document API 迁移

```python
# Python: api/documents.py
@router.post("/documents/upload")
async def upload_document(
    file: UploadFile = File(...),
    collection: str = Query("default")
):
    content = await file.read()
    chunks = document_loader.load(content, file.filename)
    vectors = embedding_model.embed(chunks)
    doc_id = vector_store.upsert(chunks, vectors)
    return {"doc_id": doc_id, "chunk_count": len(chunks)}
```

```java
// Java: DocumentController.java
@RestController
@RequestMapping("/api/rag/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "default") String collection) {

        DocumentUploadResponse response = documentService.uploadDocument(file, collection);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(documentService.listDocuments(page, size));
    }

    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String docId) {
        documentService.deleteDocument(docId);
    }
}
```

### Chat API 迁移

```python
# Python: api/chat.py
@router.post("/chat/")
async def chat(request: ChatRequest):
    query_vector = embedding_model.embed([request.query])
    results = vector_store.search(query_vector, top_k=5)
    context = format_context(results)

    response = llm_gateway.generate(
        system_prompt=RAG_PROMPT,
        user_prompt=f"Context: {context}\n\nQuestion: {request.query}"
    )

    return {"answer": response, "sources": [r.payload for r in results]}
```

```java
// Java: ChatController.java
@RestController
@RequestMapping("/api/rag/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    @PostMapping("/stream")
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }
}

// ChatService.java
@Service
@RequiredArgsConstructor
public class ChatService {

    private final EmbeddingService embeddingService;
    private final VectorStoreAdapter vectorStoreAdapter;
    private final LlmGateway llmGateway;

    public ChatResponse chat(ChatRequest request) {
        // 1. Generate embedding
        float[] queryVector = embeddingService.embed(request.query());

        // 2. Search vector store
        List<SearchResult> results = vectorStoreAdapter.search(queryVector, 5);

        // 3. Build context
        String context = results.stream()
            .map(SearchResult::getContent)
            .collect(Collectors.joining("\n---\n"));

        // 4. Generate response
        String prompt = String.format("Context:\n%s\n\nQuestion: %s", context, request.query());
        String answer = llmGateway.generate(RAG_PROMPT, prompt);

        return new ChatResponse(answer, results);
    }
}
```

### Qdrant Adapter

```java
// Java: QdrantAdapter.java
@Component
@RequiredArgsConstructor
public class QdrantAdapter {

    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper;
    private static final String COLLECTION_NAME = "documents";

    public void createCollection() {
        if (!qdrantClient.collectionExists(COLLECTION_NAME)) {
            qdrantClient.createCollection(COLLECTION_NAME,
                VectorParams.builder().size(1536).distance(Distance.Cosine).build()
            );
        }
    }

    public String upsert(List<DocumentChunk> chunks, List<float[]> vectors) {
        String docId = UUID.randomUUID().toString();
        List<PointStruct> points = IntStream.range(0, chunks.size())
            .mapToObj(i -> PointStruct.of(
                UUID.randomUUID().toString(),
                vectors.get(i),
                Map.of(
                    "doc_id", docId,
                    "content", chunks.get(i).getContent(),
                    "metadata", objectMapper.valueToTree(chunks.get(i).getMetadata())
                )
            ))
            .toList();

        qdrantClient.upsert(COLLECTION_NAME, points);
        return docId;
    }

    public List<SearchResult> search(float[] queryVector, int topK) {
        SearchResponse response = qdrantClient.search(
            SearchRequest.builder()
                .collectionName(COLLECTION_NAME)
                .queryVector(queryVector)
                .limit(topK)
                .build()
        );

        return response.getResults().stream()
            .map(this::toSearchResult)
            .toList();
    }
}
```

---

## Part 2: AI Agents Service 迁移

### 项目结构

```
services/ai_agents/
├── main.py                   →  AiAgentsApplication.java
├── presentation/agents/      →  Controller + Service
│   ├── supervisor.py        →  SupervisorAgentService.java
│   ├── rag_agent.py         →  RagAgentService.java
│   ├── tts_agent.py         →  TtsAgentService.java
│   └── ...
├── domain/services/         →  Domain Service
├── infrastructure/          →  Adapter + Tools
└── application/graphs/      →  State Machine / Workflow
```

### Supervisor Agent

```python
# Python: Supervisor Agent
class SupervisorAgent:
    def route(self, message: str) -> str:
        intent = self.llm.classify_intent(message)
        return intent  # "rag", "tts", "k8s", etc.

    async def process(self, message: str) -> AgentResponse:
        agent_name = self.route(message)
        agent = self.sub_agents[agent_name]
        return await agent.execute(message)
```

```java
// Java: SupervisorAgentService.java
@Service
@RequiredArgsConstructor
public class SupervisorAgentService {

    private final Map<String, SubAgent> subAgents;
    private final IntentClassifier intentClassifier;

    public AgentResponse process(String userMessage) {
        Intent intent = intentClassifier.classify(userMessage);
        SubAgent agent = subAgents.get(intent.getAgentName());

        if (agent == null) {
            return AgentResponse.error("Unknown agent: " + intent.getAgentName());
        }

        return agent.execute(intent);
    }
}

// IntentClassifier.java
@Component
@RequiredArgsConstructor
public class IntentClassifier {

    private final ChatModel chatModel;

    public Intent classify(String message) {
        String prompt = """
            Classify the following message into one of these intents:
            - rag: for document retrieval and Q&A
            - tts: for text-to-speech
            - vision: for image analysis
            - k8s: for Kubernetes operations
            - mlops: for ML pipeline operations

            Message: %s

            Return only the intent name.
            """.formatted(message);

        String response = chatModel.chat(prompt);

        return new Intent(parseIntent(response.trim()), message);
    }
}
```

### 子 Agent 示例

```java
// RagAgentService.java
@Service
@RequiredArgsConstructor
public class RagAgentService implements SubAgent {

    private final DocumentService documentService;
    private final ChatService chatService;

    @Override
    public boolean canHandle(Intent intent) {
        return intent.getAgentName().equals("rag");
    }

    @Override
    public AgentResponse execute(Intent intent) {
        String query = intent.getMessage();
        ChatResponse response = chatService.chat(new ChatRequest(query));
        return AgentResponse.success(response.getAnswer(), response.getSources());
    }
}
```

### LangChain4j 集成

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.langchain4j:langchain4j:1.0.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.0.0")
}
```

```java
// LangChain4jConfig.java
@Configuration
@RequiredArgsConstructor
public class LangChain4jConfig {

    private final AppSettings appSettings;

    @Bean
    public ChatModel chatModel() {
        return switch (appSettings.getLlmProvider()) {
            case "openai" -> OpenAiChatModel.builder()
                .apiKey(appSettings.getOpenAiApiKey())
                .modelName(appSettings.getLlmModel())
                .temperature(0.7)
                .build();
            case "ollama" -> new OllamaChatModel(OllamaChatModel.builder()
                .baseUrl(appSettings.getOllamaBaseUrl())
                .modelName(appSettings.getOllamaModel())
                .build());
            default -> throw new IllegalArgumentException("Unknown LLM provider");
        };
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
```

---

## Part 3: TTS Service 迁移

### Provider 架构

```python
# Python: Provider Factory
class TTSFactory:
    PROVIDERS = {
        "azure": AzureTTSProvider,
        "google": GoogleTTSProvider,
        "elevenlabs": ElevenLabsProvider,
        "edge": EdgeTTSProvider,
        "coqui": CoquiTTSProvider,
    }

    def get_provider(self, name: str) -> TTSProvider:
        return self.PROVIDERS.get(name, EdgeTTSProvider)()
```

```java
// Java: TTS Provider Architecture

// TtsProvider.java
public interface TtsProvider {
    byte[] synthesize(String text, VoiceConfig config);
    List<Voice> listVoices();
    boolean healthCheck();
}

// TtsService.java
@Service
@RequiredArgsConstructor
public class TtsService {

    private final Map<TtsProviderType, TtsProvider> providers;
    private final AppSettings settings;

    public byte[] synthesize(SynthesizeRequest request) {
        TtsProvider provider = providers.get(settings.getTtsProvider());
        return provider.synthesize(request.text(), request.voiceConfig());
    }
}

// Provider Implementations
@Component
@ConditionalOnProperty(name = "tts.provider", havingValue = "azure")
public class AzureTtsProvider implements TtsProvider {

    private final AzureSpeechConfig config;

    @Override
    public byte[] synthesize(String text, VoiceConfig config) {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(
            config.getApiKey(),
            config.getRegion()
        );

        try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig)) {
            SpeechSynthesisResult result = synthesizer.speakTextAsync(text).get();
            return result.getAudioData();
        }
    }
}

@Component
@ConditionalOnProperty(name = "tts.provider", havingValue = "google")
public class GoogleTtsProvider implements TtsProvider {
    // Google Cloud Text-to-Speech SDK integration
}
```

### TTS Controller

```java
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    @PostMapping("/synthesize")
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody SynthesizeRequest request) {
        byte[] audio = ttsService.synthesize(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "audio/mp3")
            .body(audio);
    }

    @GetMapping("/voices")
    public ResponseEntity<List<Voice>> listVoices() {
        return ResponseEntity.ok(ttsService.listVoices());
    }

    @GetMapping("/providers")
    public ResponseEntity<List<String>> listProviders() {
        return ResponseEntity.ok(List.of("azure", "google", "elevenlabs", "edge"));
    }
}
```

---

## Part 4: Vision Service 迁移

### Vision Controller

```python
# Python: api/vision.py
@router.post("/vision/detect")
async def detect_objects(image: UploadFile = File(...)):
    image_bytes = await image.read()
    results = yolo_detector.detect(image_bytes)
    return {"objects": results}
```

```java
// Java: VisionController.java
@RestController
@RequestMapping("/api/vision")
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

    @PostMapping("/ocr")
    public ResponseEntity<OcrResult> ocr(
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(visionService.ocr(image));
    }
}

// VisionService.java
@Service
@RequiredArgsConstructor
public class VisionService {

    private final ObjectDetectionAdapter yoloAdapter;
    private final ImageCaptioningAdapter blipAdapter;
    private final OcrAdapter ocrAdapter;

    public DetectionResult detectObjects(MultipartFile image) {
        byte[] imageBytes = image.getResource().getContentAsByteArray();
        return yoloAdapter.detect(imageBytes);
    }
}
```

---

## Part 5: 健康检查迁移

```python
# Python: FastAPI Health Check
@app.get("/health")
async def health():
    qdrant_connected = False
    try:
        vector_store.client.get_collection(collection_name)
        qdrant_connected = True
    except Exception:
        pass

    return HealthResponse(
        status="ok" if qdrant_connected else "degraded",
        qdrant_connected=qdrant_connected,
    )
```

```java
// Java: Spring Boot Health Check
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final QdrantAdapter qdrantAdapter;
    private final EmbeddingService embeddingService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        boolean qdrantConnected = qdrantAdapter.isConnected();

        String status = qdrantConnected ? "ok" : "degraded";
        return ResponseEntity.ok(new HealthResponse(
            status,
            qdrantConnected,
            embeddingService.getModelName(),
            appSettings.getLlmProvider()
        ));
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
Step 2: 核心 DTO / 领域对象迁移
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

- 端到端业务流程（如：上传文档 -> 检索 -> 问答）
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

### RAG Service
- [ ] 文档上传 → 向量存储
- [ ] 文档搜索 → 相似度检索
- [ ] Chat → RAG 问答

### AI Agents Service
- [ ] Supervisor 正确路由
- [ ] 各子 Agent 执行正常
- [ ] LangChain4j LLM 调用

### TTS Service
- [ ] Edge TTS (默认 Provider)
- [ ] Azure TTS
- [ ] Google TTS
- [ ] ElevenLabs
- [ ] 语音列表获取

### Vision Service
- [ ] YOLO 目标检测
- [ ] BLIP 图像描述
- [ ] OCR 文字识别
- [ ] 图像生成
