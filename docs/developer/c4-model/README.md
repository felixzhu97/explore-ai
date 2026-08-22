# C4 模型文档

使用 PlantUML 绘制的 C4 架构模型，描述 AI Chat & Agent Platform 的完整架构。

Source of truth: `.puml`。官方 C4: [c4model.com](https://c4model.com/)。库: [C4-PlantUML](https://github.com/plantuml-stdlib/C4-PlantUML)。

## Visual tracks

| Track | Files | Style |
| ----- | ----- | ----- |
| **Structural C4** | C1–C3, Deployment | Official **`C4_blue_new`** theme（线框；勿与 zinc 混用） |
| **Domain + Dynamics** | Code domain model, `C4-Dynamic-*` | Shared zinc look via [`style-zinc.puml`](style-zinc.puml) |

Do not mix `C4_blue_new` into domain/dynamic diagrams（也不要把 `style-zinc.puml` 用于 structural C4）。

## 文件

| 文件 | 层级 | 说明 |
| --- | --- | --- |
| `C1-Context.puml` | C1 | 系统上下文图（含 LaunchDarkly、Datadog、cloud-minimal prod） |
| `C2-Container.puml` | C2 | 容器图（13 个子域 + 功能开关横切） |
| `C3-Component.puml` | C3 | **单图**：前后端组件 + Clean Architecture 四层 |
| `C4-Code-Domain-Model.puml` | **Code** | 领域模型（Entity 行为 + VO / Repository；对齐 `com.ai.*.domain`） |
| `C4-Deployment.puml` | Deployment | **单图**：本地 dev + 生产（Vercel + Render） |
| `style-zinc.puml` | Shared | Code + Dynamics 共用 zinc 样式 |
| `C4-Dynamic-Document-Upload.puml` | Dynamic | 文档上传 ETL |
| `C4-Dynamic-Rag-Ask.puml` | Dynamic | RAG SSE 问答 |
| `C4-Dynamic-Chat-Tools.puml` | Dynamic | Chat 工具 SSE + A2UI 图表 |

> **Code vs Deployment**：C4 官方第 4 层是 **Code**（类与关系）。本仓 `C4-Deployment.puml` 是部署视图；领域类型总览见 `C4-Code-Domain-Model.puml`。图中 stereotype 表示约定，**没有**共享 Java `Entity`/`AggregateRoot` 基类。


### When to open which track

- 边界 / 部署拓扑 → Structural C4（`C4_blue_new`）
- 统一语言 / 聚合行为与 VO → Code domain model
- 运行时主链路（上传、RAG、Chat 工具）→ `C4-Dynamic-*`

---

---

## C1 - 系统上下文图

![C1-Context](png/C1-Context.png)

---

## C2 - 容器图

![C2-Container](png/C2-Container.png)

---

## C3 - 组件图

![C3-Component](png/C3-Component.png)

前后端合并在单图中（参照 Academic C3 结构）；Web App 边界 + Backend 四层分组。

---

## Code - 领域模型

![C4-Code-Domain-Model](png/C4-Code-Domain-Model.png)

按 `com.ai.*.domain` 分包；Aggregate / Entity 展示领域行为（factory、状态转换、聚合内操作）。术语见 [Glossary](../../Glossary.md) Appendix A。

---

## C4 - 部署图

![C4-Deployment](png/C4-Deployment.png)

本地 dev（`:4200` → `:9000`）与生产（Vercel + Render Starter）合并在单图中；详见图内 `cloud-minimal prod` 注释。

---

## Dynamic - 运行时序列

| 图 | 链路 |
| --- | --- |
| [C4-Dynamic-Document-Upload.puml](C4-Dynamic-Document-Upload.puml) | 文档上传 → 分块 → 嵌入 → H2 |
| [C4-Dynamic-Rag-Ask.puml](C4-Dynamic-Rag-Ask.puml) | RAG 提问 → 检索 → SSE 流式回答 |
| [C4-Dynamic-Chat-Tools.puml](C4-Dynamic-Chat-Tools.puml) | Chat 工具调用 → SSE → A2UI 图表 |

![C4-Dynamic-Rag-Ask](png/C4-Dynamic-Rag-Ask.png)

---

## 技术栈

### 后端 (AI Platform Backend)

- **运行时**: Spring Boot 4.1 / Java 25 / Spring AI 2.0
- **架构**: Clean Architecture（`domain/repository/`，非 Hexagonal `domain/port/`）
- **端口**: dev **9000** / prod **8080** (Render `PORT`)
- **子域 (13)**: Chat / Pipeline / Skill / RAG / Tools / Analysis / Eval / Image / Image Analysis (`vision`) / Audio (TTS+ASR) / MCP Server / MCP Client / Metrics
- **持久化**: H2 嵌入式（会话元数据 `JdbcChatSessionMetadataRepository` + 消息 `JdbcChatMemoryRepository` + 向量 + AI 调用事件 `ai_invocation_events`）
- **功能开关**: LaunchDarkly（`ModuleAccessFilter` + `FeatureFlagService`）
- **可观测性**: Datadog RUM（前端，可选）；APM javaagent 可选（Render Starter 512MB 默认关闭）
- **外部服务 (cloud)**: DeepSeek API (LLM) / OpenAI API (DALL-E + TTS) / Serper.dev (Web 搜索) / Resend (Automations 邮件)
- **本地服务 (dev / prod 默认关闭)**: Ollama / whisper.cpp / Tesseract / ONNX Image Analysis

### Pipeline（工作流）子域

路由 `/pipelines`（`module-pipelines`），API `/api/pipelines/*`：

| 能力 | API | 主要组件 |
| --- | --- | --- |
| Worker 列表 / 健康 | `GET /api/pipelines/list`, `.../health` | `PipelineController`, `PipelineFacade` |
| 单 Worker SSE | `POST /api/pipelines/{type}/invoke/sse` | `SpringAiWorkerAgentInvoker` |
| Supervisor SSE | `POST /api/pipelines/supervisor/invoke/sse` | `SpringAiSupervisorRouter` |
| 画布图 SSE | `POST /api/pipelines/invoke/sse` | `OrchestratorWorkersUseCase`；节点可带 `systemPrompt`/`toolKeys` 快照 |

Worker 定义来自 Pipeline 内置目录：`AgentTemplateCatalog`（`classpath:agent-templates/{lang}.json`）经 `CatalogAgentRegistry` 提供调色板种子；画布双击编辑的是**图内节点副本**。Classpath Agent Skills：`AgentSkillsRuntime`（`com.ai.common`，`app.pipeline.skills`）。

### Prompt Catalog（横切）

提示词作为工程资产外置到 `src/main/resources/prompts/`：

| 路径 | 用途 |
| --- | --- |
| `shared/style-minimal.st` / `format-gfm.st` | 极简风格 + GFM（单一事实源） |
| `chat/` | 默认 system 角色、工具策略、A2UI |
| `rag/` | RAG system + 多语言 user / no-context |
| `agent/` | 历史 Worker `.st` 参考（权威定义已迁至 `agent-templates/`） |
| `agent-templates/` | 多语言 Agent 模版 JSON（en/zh/ja/fr/es） |
| `task/` | 摘要 / 翻译 / Q&A |
| `guards/after-tools.st` | 工具调用后最终作答提醒 |

组合入口：`PromptTemplates` + `ClasspathPromptTemplate`；RAG/Vision user：`LocalizedRagPromptBuilder`。

### Image Analysis 子域（包名 `com.ai.vision`）

独立 `/vision` 页面，**不经过 Ollama**（受 `module-vision` flag 守卫）：

| 能力 | API | 适配器 | 依赖 |
| --- | --- | --- | --- |
| Caption | `POST /api/vision/caption` | `OnnxBlipCaptioner` | `models/blip_*.onnx` |
| Detect | `POST /api/vision/detect` | `OnnxYoloDetector` | `models/yolov8n.onnx` |
| OCR | `POST /api/vision/ocr` | `Tess4jOcrEngine` | Tesseract + `models/tessdata/` |
| Health | `GET /api/vision/health` | — | 各 Provider 就绪状态 |

> **区分**: **Image Analysis** (`/vision`, `/api/vision/*`) vs **Vision Chat** (RAG 流式多模态，Ollama qwen3.5)

### Metrics 子域（包名 `com.ai.metrics`）

路由 `/metrics`（Work 导航，无 feature flag），API `/api/metrics/*`：

| 能力 | API | 主要组件 |
| --- | --- | --- |
| 概览 | `GET /api/metrics/overview` | `MetricsController`, `MetricsUseCase` |
| 域快照 | `GET /api/metrics/domains/{domain}` | `JdbcMetricsQueryRepository` |
| 时序 | `GET /api/metrics/series` | `SeriesSnapshot` |
| 下钻 | `GET /api/metrics/drilldown` | `AiInvocationEvent`, `JdbcAiInvocationEventRepository` |

业务路径通过 `AiInvocationRecorder` 旁路写入 `ai_invocation_events`（Chat / RAG / Agents / Tools / Vision）。
LLM 调用另由 Spring AI Micrometer Observation 导出 GenAI 语义指标（`/actuator/prometheus`）；业务域计数仍以 `AiInvocationRecorder` 为准。
RAG 检索经 `H2SpringAiVectorStore`（Spring AI `VectorStore` SPI）+ `VectorStoreDocumentRetriever`；本地 profile 默认关闭 MCP client，避免 STDIO 阻断启动（生产用 Streamable HTTP）。

### 前端 (Web Frontend)

- **框架**: Angular 22 + TypeScript
- **路由**: `/chat` `/chat/:sessionId` / `/generate` / `/rag` / `/metrics` + flag `/pipelines` `/skills` `/vision` `/mcp` `/eval` `/asr`
- **对话壳**: `shared/components/chat-shell`（message-pane / sender-bar / bubble-list / welcome），供 Chat / RAG / Agents 共用
- **实现目录**: `app/chat/`、`app/generate/{image,tts}/`、`app/metrics/`（与业务域 / 路由对齐）
- **API 服务**: `ApiChatService` / `ApiRagService` / `ApiMediaService` / `AgentsService` / `MetricsService` + `sse-client.ts` + shared ECharts panels
- **功能开关**: `FeatureFlagService` + `moduleEnabledGuard`（LaunchDarkly Client SDK）
- **可观测性**: `datadog-rum.config.ts`
- **端口**: dev 4200 (proxy `/api` → `:9000`) / prod Vercel 静态托管

### Chat 流式 API

| 端点 | 说明 |
| --- | --- |
| `POST /api/text/chat/stream` | SSE 流式对话（`TextController`） |
| `GET /api/text/providers` | 可用 LLM Provider |
| `GET /api/text/models` | 模型列表 |
| `POST /api/chat` | 非流式对话（`ChatController`） |
| `GET/POST /api/sessions` | 会话 CRUD |

---

## 部署拓扑

### 本地开发

```
Browser :4200 → Angular Dev Server → proxy /api/* → Spring Boot :9000
                                              ↘ H2 ./data/explore-ai
                                              ↘ Ollama :11434 / whisper :8178 / Tesseract
                                              ↘ DeepSeek / OpenAI / Serper / Resend
```

### 生产 (cloud-minimal)

```
Browser → Vercel (Angular static) → Render Starter explore-ai (:8080 + H2 ephemeral)
        ↘ Datadog RUM (us5, 可选)        ↘ 无 APM sidecar（Starter 512MB，不启 javaagent）
        ↘ LaunchDarkly Client            ↘ LaunchDarkly Server
                                         → DeepSeek / OpenAI / Serper / Resend
```

**Prod frontend**: `https://www.felixzhu.chat` (Vercel)  
**Prod API (browser)**: same-origin `/api` → Vercel rewrite → Render  
**Prod OAuth (browser)**: same-origin `/oauth2/*` + `/login/oauth2/*` → Vercel rewrite → Render  
**Prod API (direct)**: `https://explore-ai-3krr.onrender.com/api`  
**Google OAuth redirect URI**: `https://www.felixzhu.chat/login/oauth2/code/google`  
**GitHub OAuth redirect URI**: `https://www.felixzhu.chat/login/oauth2/code/github`


**cloud-minimal**: `module-pipelines` / `module-skills` **开启**；Vision / ASR / MCP / Eval / Ollama **关闭**

---

## 部署端口汇总

| 服务 | 端口 / 说明 |
| --- | --- |
| Spring Boot Backend (dev) | **9000** |
| Spring Boot Backend (prod) | **8080** |
| H2 Embedded | 内嵌 (dev `./data` / prod `/app/data` volume) |
| Ollama (Embedding/RAG Vision) | 11434 [local] |
| whisper.cpp (ASR) | 8178 [local] |
| Tesseract OCR | 系统安装 (JNA) [local] |
| Image Analysis ONNX Models | `models/` 本地文件 [local] |
| Angular Dev Server | 4200 |
| Vercel (prod frontend) | HTTPS |
| Render explore-ai (prod backend) | HTTPS → :8080（Starter：always-on / 无持久盘） |
| DeepSeek / OpenAI / Serper / Resend | HTTPS |
| LaunchDarkly / Datadog us5 | HTTPS |

---

## 功能开关 (LaunchDarkly)

| Flag Key | 模块 | 前端路由 | 后端路径前缀 | prod fallback |
| --- | --- | --- | --- | --- |
| `module-pipelines` | Pipeline（工作流） | `/pipelines` | `/api/pipelines` | `true` |
| `module-skills` | Skills | `/skills` | `/api/skills` | `true` |
| `module-vision` | Image Analysis | `/vision` | `/api/vision` | `false` |
| `module-audio-asr` | ASR | `/asr` | `/ws/audio` | `false` |
| `module-mcp` | MCP | `/mcp` | `/api/mcp` | `false` |
| `module-eval` | Eval | `/eval` | `/api/eval` | `false` |

---

## 模型配置

| 用途 | 模型 | 维度/说明 |
| --- | --- | --- |
| LLM Chat / Agent | deepseek-v4-flash | DeepSeek API |
| Embedding | qwen3-embedding:0.6b | 1024 维 (Ollama, local Qwen3 Embedding) |
| Vision Chat (RAG) | qwen3.5 | Ollama 多模态对话 (local) |
| Caption | BLIP base ONNX | ONNX Runtime 本地 |
| Detect | YOLOv8n ONNX | COCO 80 类 |
| OCR | eng + chi_sim | Tesseract tessdata |
| ASR | whisper-base | whisper.cpp 本地 |
| Image Gen | dall-e-3 | OpenAI API |
| TTS | gpt-4o-mini-tts | OpenAI API |

---

## 查看与更新

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- 重新生成 PNG：

```bash
cd docs/developer/c4-model && mkdir -p png && plantuml -tpng -o png *.puml
```

若本机无 `plantuml` CLI：

```bash
cd docs/developer/c4-model && docker run --rm -v "$PWD":/data plantuml/plantuml -tpng -o png /data/*.puml
```

## 相关文档

- [术语表](../../Glossary.md) — Ubiquitous Language 与代码映射
- [API 文档](../api.md) — REST / SSE 与 JSON 字段约定
- [用户故事地图](../../product-owner/User-Story-Map.md)
