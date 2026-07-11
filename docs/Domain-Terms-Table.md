# Domain Terms — Single Table | 商业领域术语总表

> Canonical source: [Domain-Glossary.md](Domain-Glossary.md). English terms are preferred for code, API, and commits.

| 业务域 | 英文术语 | 中文 | 类型 | 定义 | 代码映射 |
|--------|----------|------|------|------|----------|
| 通用架构 | Aggregate Root | 聚合根 | Architecture | 事务边界内的聚合根，外部仅通过根访问 | `ChatSession` |
| 通用架构 | Entity | 实体 | Architecture | 有身份标识、可变生命周期的领域对象 | `ChatMessage`, `Document` |
| 通用架构 | Value Object | 值对象 | Architecture | 不可变、按值比较、无独立身份 | `ChatSessionId`, `SourceDocument` |
| 通用架构 | Domain Service | 领域服务 | Architecture | 跨实体、无状态领域逻辑 | `LanguageDetectionService` |
| 通用架构 | Use Case | 用例 | Architecture | 应用层编排，不含业务规则细节 | `RagChatUseCase`, `ChatUseCase` |
| 通用架构 | Facade | 门面 | Architecture | 简化应用入口，协调多个用例 | `ChatFacade`, `ToolsFacade` |
| 通用架构 | Port | 端口 | Architecture | 领域定义的外部能力契约 | `DocumentSearchTool`, `WebSearchTool` |
| 通用架构 | Adapter | 适配器 | Architecture | 端口的基础设施实现 | `OllamaEmbeddingAdapter` |
| 通用架构 | Repository | 仓储 | Architecture | 聚合根持久化抽象 | `ChatSessionRepository` |
| 通用架构 | Streaming (SSE) | 流式响应 | Technical | 通过 SSE 实时输出 AI 内容 | `StreamingService` |
| 通用架构 | Provider | 提供商 | Business | LLM / AI 服务供应商 | `selectedProvider` |
| 通用架构 | Model | 模型 | Business | Provider 下的具体 LLM 型号 | `ModelInfo` |
| 通用架构 | Domain Exception | 领域异常 | Architecture | 业务规则违反异常 | `ChatSessionNotFoundException` |
| 通用架构 | Health Check | 健康检查 | Capability | 服务存活探针 | `GET /api/health` |
| 通用架构 | Service Status | 服务状态 | UI Concept | 前端组件在线状态 | `ServiceStatus` |
| 通用架构 | Error Code | 错误码 | Technical | 机器可读 API 错误标识 | 见 §API 错误码 |
| 通用架构 | Sources Event | 来源事件 | Technical / SSE | RAG 流中携带引用的命名 SSE 事件 | event `"sources"` |
| 通用架构 | AI Service Exception | AI 服务异常 | Domain Exception | AI 提供商运行时失败 | `AiServiceException` |
| 通用架构 | RAG Service Exception | RAG 服务异常 | Domain Exception | RAG 域错误基类 | `RagServiceException` |
| 通用架构 | Document Processing Exception | 文档处理异常 | Domain Exception | 文档 ETL 管道失败 | `DocumentProcessingException` |
| AI 工程 | Large Language Model (LLM) | 大语言模型 | Technical | 文本生成与推理神经网络模型 | `ChatModel` |
| AI 工程 | ChatClient | 对话客户端 | Technical | Spring AI 流式 LLM 交互 API | `ChatClient` |
| AI 工程 | ChatModel | 对话模型 | Technical | LLM 提供商抽象 | `org.springframework.ai.chat.model.ChatModel` |
| AI 工程 | Prompt | 提示词 | Technical | 发送给 LLM 的输入文本 | `Prompt`, `PromptTemplate` |
| AI 工程 | System Prompt | 系统提示词 | Technical | 定义 AI 角色与行为的指令 | `PromptTemplates` |
| AI 工程 | Prompt Template | 提示词模板 | Technical | 带占位符的可复用提示词 | `PromptTemplate` |
| AI 工程 | Context Window | 上下文窗口 | Technical | 单次请求带入的历史消息上限 | `ChatSession.getRecentMessages()` |
| AI 工程 | Token | 令牌 | Technical | LLM 输入输出与计费原子单位 | — |
| AI 工程 | Temperature | 温度 | Technical | 控制输出随机性 (0–1) | — |
| AI 工程 | Retrieval-Augmented Generation (RAG) | 检索增强生成 | Pattern | 检索 + LLM 生成模式 | `RagChatUseCase` |
| AI 工程 | Augmented Generation | 增强生成 | Pattern | 基于检索上下文的 LLM 生成 | `RagChatUseCase.chat()` |
| AI 工程 | Vector Store | 向量存储 | Technical | 存储 Embedding 用于相似度检索 | `H2VectorAdapter`, pgvector |
| AI 工程 | Tool Callback | 工具回调 | Technical | LLM 发起工具调用的 Spring AI 机制 | `ToolCallback` |
| AI 工程 | Advisor | 顾问 | Technical | ChatClient 调用链拦截/增强器 | Spring AI Advisors |
| AI 工程 | Multimodal | 多模态 | Technical | 文本 + 图像等组合输入 | `VisionChatUseCase` |
| AI 工程 | Model Context Protocol (MCP) | 模型上下文协议 | Protocol | 向 LLM 暴露 Tools/Resources 的标准协议 | `AiMcpServerService` |
| AI 工程 | Agent | 智能体 | Pattern | 有目标与工具访问权的自主 AI 实体 | `.cursor/agents/` |
| AI 工程 | Orchestrator | 编排器 | Pattern | 委派任务给子智能体的 Agent | `orchestrator.md` |
| AI 工程 | Subagent | 子智能体 | Pattern | 单一职责专用 Agent | `.cursor/agents/*.md` |
| AI 工程 | Grounding | 事实锚定 | Pattern | 将回答约束在检索来源内 | `RagChatUseCase.buildPrompt()` |
| AI 工程 | Prompt Engineering | 提示工程 | Practice | 通过提示词优化输出质量 | — |
| 对话 | Chat Session | 会话 | Aggregate Root | 用户与 AI 的多轮对话容器 | `ChatSession` |
| 对话 | Chat Message | 消息 | Entity | 会话中的单条消息 | `ChatMessage` |
| 对话 | User Message | 用户消息 | Role | 用户发送的消息 | role=`user` |
| 对话 | Assistant Message | 助手消息 | Role | AI 返回的消息 | role=`assistant` |
| 对话 | System Message | 系统消息 | Role | 系统角色消息 | role=`system` |
| 对话 | Chat Session ID | 会话标识 | Value Object | 会话唯一标识 | `ChatSessionId` |
| 对话 | Message ID | 消息标识 | Value Object | 消息唯一标识 | `MessageId` |
| 对话 | Chat Session Status | 会话状态 | Enum | 会话生命周期状态 | `ChatSessionStatus` |
| 对话 | ACTIVE | 活跃 | Enum Value | 会话可接收新消息 | `ACTIVE` |
| 对话 | CLOSED | 已关闭 | Enum Value | 会话已结束 | `CLOSED` |
| 对话 | Chat Stream | 流式对话 | Use Case Behavior | SSE 实时接收 AI 回复 | `ChatUseCase.chatStream()` |
| 对话 | Text Chat Stream | 文本流式对话 | API Behavior | 主对话流式端点 | `POST /api/text/chat/stream` |
| 对话 | Simple Chat | 简单对话 | API Behavior | 无会话的遗留对话 | `POST /api/chat/simple` |
| 对话 | Text Chat Options | 文本对话选项 | Application Concept | provider / model / toolsEnabled | `TextChatOptions` |
| 对话 | Tools Enabled | 启用工具 | Config / Flag | 对话流是否启用工具调用 | `toolsEnabled` |
| 对话 | Provider Catalog | 提供商目录 | Capability | 列出可用 LLM 提供商 | `GET /api/text/providers` |
| 对话 | Model Catalog | 模型目录 | Capability | 列出 Provider 下模型 | `GET /api/text/models` |
| 对话 | Session Title Generator | 会话标题生成器 | Application Service | LLM 生成会话短标题 | `SessionTitleGenerator` |
| 对话 | Chat Memory | 对话记忆 | Technical | Spring AI 记忆与领域会话同步 | `ChatMemorySessionBridge` |
| 对话 | Session Info | 会话信息 | DTO | 会话元数据 API 视图 | `SessionInfo` |
| 对话 | Last Activity At | 最后活跃时间 | Domain Field | 最近活动时间戳 | `ChatSession.lastActivityAt` |
| 对话 | Recent Messages | 最近消息 | Domain Behavior | 上下文窗口内最近 N 条消息 | `getRecentMessages(int)` |
| 对话 | Language Detection | 语言检测 | Domain Service | 检测用户输入语言 | `LanguageDetectionService` |
| 对话 | Tool Call | 工具调用记录 | UI / API Concept | 单次工具调用及状态 | `ToolCall` |
| 对话 | Pinned Session | 固定会话 | UI Concept | 侧边栏置顶会话 | `SidebarService.togglePin()` |
| 对话 | Recent Session | 最近会话 | UI Concept | 侧边栏最近会话 | `SidebarService._recentSessions` |
| 知识问答 | Document QA | 文档问答 | Capability | RAG 产品用户面名称 | route `/rag` |
| 知识问答 | Document | 文档 | Entity | 用户上传的知识源文件 | `Document` |
| 知识问答 | Document ID | 文档标识 | Value Object | 文档唯一标识 | `DocumentId` |
| 知识问答 | Document Status | 文档状态 | Enum | 文档处理生命周期 | `DocumentStatus` |
| 知识问答 | UPLOADING | 上传中 | Enum Value | 文件上传中 | → PROCESSING / FAILED |
| 知识问答 | PROCESSING | 处理中 | Enum Value | 分块与向量化中 | → READY / FAILED |
| 知识问答 | READY | 就绪 | Enum Value | 可用于 RAG 检索 | → PROCESSING |
| 知识问答 | FAILED | 失败 | Enum Value | 处理失败 | → PROCESSING |
| 知识问答 | Document Chunk | 文档分块 | Entity | 检索最小单元 | `DocumentChunk` |
| 知识问答 | Chunk Index | 分块序号 | Domain Field | 分块在文档内序号 | `chunkIndex` |
| 知识问答 | Chunk Count | 分块数量 | Metric | 文档产生的分块数 | `chunkCount` |
| 知识问答 | Raw Document | 原始文档 | Value Object | ETL 前的规范化文档视图 | `RawDocument` |
| 知识问答 | Chunking | 分块 | Application Behavior | 将文档文本切分为块 | `ChunkingService` |
| 知识问答 | Chunk Size | 分块大小 | Config | 每块最大字符数 (默认 500) | `RagProperties.Chunk.size` |
| 知识问答 | Chunk Overlap | 分块重叠 | Config | 相邻块重叠字符数 (默认 50) | `RagProperties.Chunk.overlap` |
| 知识问答 | Embedding | 嵌入向量 | Technical | 文本向量表示 | `EmbeddingAdapter` |
| 知识问答 | Retrieval | 检索 | Application Behavior | 向量相似度查找相关块 | `DocumentSearchService` |
| 知识问答 | Top K | 检索数量 | Config | 最大返回块数 (默认 5) | `topK` |
| 知识问答 | Score Threshold | 分数阈值 | Config | 最低相似度 (默认 0.5) | `scoreThreshold` |
| 知识问答 | Document-Scoped Retrieval | 文档范围检索 | Application Behavior | 限定检索到选定文档 | `docIds` |
| 知识问答 | Source Document | 来源文档 | Value Object | 检索命中块及相似度 | `SourceDocument` |
| 知识问答 | Similarity Score | 相似度分数 | Metric | 查询与块的余弦相似度 | `score` |
| 知识问答 | Context | 上下文 | Application Concept | 传入 LLM 的检索文本与来源 | `retrieveContext()` |
| 知识问答 | Retrieval Result | 检索结果 | Application Concept | 上下文文本 + 来源文档集合 | `RetrievalResult` |
| 知识问答 | RAG Chat | RAG 对话 | Use Case | 基于检索上下文生成回答 | `RagChatUseCase` |
| 知识问答 | Vision Chat | 视觉问答 | Use Case | 带图片附件的多模态 RAG 对话 | `VisionChatUseCase` |
| 知识问答 | Document Upload | 文档上传 | Use Case | 上传并触发 ETL | `DocumentUploadService` |
| 知识问答 | Document Delete | 文档删除 | Use Case Behavior | 删除文档及全部分块 | `DELETE /api/rag/documents/{id}` |
| 知识问答 | Document Reprocess | 文档重处理 | Domain Behavior | 对 READY/FAILED 文档重新处理 | `Document.markProcessing()` |
| 知识问答 | Document Summary | 文档摘要 | DTO | 文档列表视图字段 | `DocumentSummaryDto` |
| 知识问答 | RAG ETL Pipeline | RAG 数据管道 | Pipeline | Reader → Transformer → Writer | `DocumentUploadService` |
| 知识问答 | Document Reader | 文档读取器 | Port | 读取原始字节为 RawDocument | `DocumentReader` |
| 知识问答 | Document Transformer | 文档转换器 | Port | 分块 + 向量化 | `DocumentTransformer` |
| 知识问答 | Document Writer | 文档写入器 | Port | 持久化到向量库 | `DocumentWriter` |
| 知识问答 | Vector Similarity | 向量相似度 | Domain Utility | 两向量余弦相似度计算 | `VectorSimilarity` |
| 知识问答 | Quick Prompts | 快捷提问 | UX Concept | 预设 RAG 起始问题 | i18n `ragChat.*` |
| 知识问答 | Citation | 引用 | UI Component | 来源文档归因展示 | `CitationComponent` |
| 工具调用 | Tool Calling | 工具调用 | Capability | LLM 根据意图调用外部工具 | `ToolsFacade` |
| 工具调用 | Tool Chat | 工具对话 | Use Case Behavior | 带工具能力的 AI 对话 | `ToolsController.chatWithTools()` |
| 工具调用 | Tool Result | 工具结果 | Value Object | 工具执行成功/失败结果 | `ToolResult` |
| 工具调用 | Document Search Tool | 文档搜索工具 | Port | 在知识库中搜索文档 | `DocumentSearchTool` |
| 工具调用 | RAG Search Tool | RAG 搜索工具 | Adapter | 文档搜索基础设施实现 | `RagSearchTool` |
| 工具调用 | Web Search Tool | 网页搜索工具 | Port / Adapter | Serper 实时网页搜索 | `WebSearchTool` |
| 工具调用 | Weather Tool | 天气工具 | Tool | 查询天气与预报 | `WeatherTools` |
| 工具调用 | Weather Query | 天气查询 | Value Object | 校验后的城市名 | `WeatherQuery` |
| 工具调用 | Weather Forecast | 天气预报查询 | Value Object | 城市 + 预报天数 | `WeatherForecast` |
| 工具调用 | Weather Info | 天气信息 | Value Object | 当前天气状况 | `WeatherInfo` |
| 工具调用 | Weather Report | 天气报告 | Domain Service | 生成天气与预报 | `WeatherReport` |
| 工具调用 | getWeather | 获取天气 | Tool | LLM 可调用当前天气工具 | `WeatherTools.getWeather` |
| 工具调用 | getForecast | 获取预报 | Tool | LLM 可调用预报工具 | `WeatherTools.getForecast` |
| 工具调用 | searchDocuments | 搜索文档 | Tool | LLM 可调用知识库搜索 | `RagSearchTool.searchDocuments` |
| 工具调用 | listDocuments | 列出文档 | Tool | LLM 可调用文档清单 | `RagSearchTool.listDocuments` |
| 工具调用 | searchWeb | 网页搜索 | Tool | LLM 可调用网页搜索 | `WebSearchTool.searchWeb()` |
| 工具调用 | Web Search Chat | 网页搜索对话 | Capability | 触发 searchWeb 的对话模式 | `POST /api/tools/chat` |
| 工具调用 | Document Search Chat (Streaming) | 文档搜索流式对话 | Capability | SSE 工具调用文档问答 | `POST /api/tools/chat/stream` |
| 工具调用 | Tool Callback Registry | 工具回调注册表 | Port | 工具名到 ToolCallback 映射 | `McpToolCallbackRegistry` |
| 图像生成 | Image Generation | 图像生成 | Use Case | 根据文本提示生成图像 | `ImageFacade` |
| 图像生成 | Image Prompt | 图像提示词 | Value Object | 校验后的图像生成提示 (≤4000 字) | `ImagePrompt` |
| 图像生成 | Image Options | 图像选项 | Value Object | 模型、质量、尺寸、数量 | `ImageOptions` |
| 图像生成 | Image Catalog | 图像目录 | Value Object | 支持的模型/尺寸/质量 | `ImageCatalog` |
| 图像生成 | Image Size | 图像尺寸 | Value Object | 宽×高尺寸 | `ImageSize` |
| 图像生成 | Image Quality | 图像质量 | Config | 生成质量档位 | `standard`, `hd` |
| 图像生成 | Generated Image | 生成图像 | Value Object | 生成结果 URL 或 base64 | `GeneratedImage` |
| 图像生成 | Revised Prompt | 修订提示词 | API Field | 提供商优化后的提示词 | `revisedPrompt` |
| 图像生成 | Generation Status | 生成状态 | Enum-like | 生成请求结果状态 | `SUCCESS`, `ERROR` |
| 图像分析 | Image Analysis | 图像分析 | Capability | 独立 caption/detect/OCR | route `/vision` |
| 图像分析 | Caption | 图像描述 | Use Case Behavior | 自然语言描述图像内容 | `POST /api/vision/caption` |
| 图像分析 | Object Detection | 目标检测 | Use Case Behavior | 检测物体及边界框 | `POST /api/vision/detect` |
| 图像分析 | OCR | 文字识别 | Use Case Behavior | 提取图像可见文字 | `POST /api/vision/ocr` |
| 图像分析 | Detection | 检测结果 | Value Object / DTO | 单个检测物体 | `Detection`, `DetectionDto` |
| 图像分析 | Bounding Box | 边界框 | Value Object | 检测区域矩形坐标 | `bbox` |
| 图像分析 | Confidence Score | 置信度 | Metric | 检测置信度 (0–1) | `confidence` |
| 图像分析 | Full Text | 全文 | API Field | OCR 提取的完整文本 | `fullText` |
| 图像分析 | Processing Time | 处理耗时 | Metric | 分析端到端延迟 (ms) | `processingTimeMs` |
| 生成 (UI) | Generation | 生成 | Capability | 图像生成 + TTS 父级 UI 壳 | route `/generate` |
| 生成 (UI) | Negative Prompt | 负面提示词 | UX Concept | 描述需避免的图像内容 (仅 UI) | i18n |
| 生成 (UI) | Voice Selection | 音色选择 | Value Object | 校验后的音色 + TTS 模型对 | `VoiceSelection` |
| 生成 (UI) | Stream TTS | 流式语音合成 | Use Case Behavior | 实时流式音频合成 | `GET /api/audio/stream` |
| 生成 (UI) | Speech Speed | 语速 | UX / Request Field | TTS 播放速度倍率 | `TtsRequest.speed` |
| 语音 | Text-to-Speech (TTS) | 语音合成 | Use Case | 文字转语音 | `AudioFacade` |
| 语音 | Voice | 音色 | Business Concept | 合成使用的声音类型 | `VoiceInfo`, `VoiceCatalog` |
| 语音 | Speech Text | 语音文本 | Value Object | TTS 校验输入文本 | `SpeechText` |
| 语音 | Synthesized Audio | 合成音频 | Value Object | TTS 输出音频字节 | `SynthesizedAudio` |
| 语音 | Synthesize | 合成 | Use Case Behavior | 执行 TTS 转换 | `POST /api/audio/speak` |
| 语音 | Automatic Speech Recognition (ASR) | 自动语音识别 | Capability | 语音转文字 | `StreamingTranscriptionUseCase` |
| 语音 | Streaming Transcription | 流式转写 | Use Case Behavior | WebSocket 实时 ASR | `AudioTranscriptionWebSocketHandler` |
| 语音 | Transcription | 转写 | Application Concept | 单次 ASR 结果 | `WhisperCppTranscriptionAdapter` |
| MCP | MCP Server | MCP 服务端 | Service | 对外暴露平台 AI 能力 | `AiMcpServerService` |
| MCP | MCP Client | MCP 客户端 | Service | 连接并调用外部 MCP 服务 | `AiMcpClientService` |
| MCP | MCP Tool | MCP 工具 | Technical | MCP 协议下可调用工具 | `registerTools()` |
| MCP | MCP Tool Definition | MCP 工具定义 | Value Object | MCP 工具名称与描述 | `McpToolDefinition` |
| MCP | MCP Session | MCP 会话 | Entity | 与 MCP 服务端的活跃连接 | `McpSession` |
| MCP | MCP Session Status | MCP 会话状态 | Enum | MCP 连接生命周期 | `ACTIVE`, `CLOSED` |
| MCP | MCP Server Connection | MCP 服务端连接 | Value Object | 外部 MCP 服务连接元数据 | `McpServerConnection` |
| MCP | MCP Chat | MCP 对话 | Use Case Behavior | 通过 MCP Client 发起的对话 | `McpClientController.chat()` |
| MCP | MCP Capabilities | MCP 能力 | Technical | 服务端能力标识 | `/api/mcp/info` |
| MCP | MCP Resource | MCP 资源 | Protocol Concept | MCP URI 只读数据资源 | `@McpResource` |
| MCP | MCP Prompt | MCP 提示模板 | Protocol Concept | MCP 暴露的预定义提示模板 | `availablePrompts` |
| MCP | search_knowledge_base | 知识库搜索 | MCP Tool | MCP 语义文档搜索工具 | `searchKnowledgeBase` |
| 结构化分析 | Structured Output | 结构化输出 | Technical | AI 返回强类型 JSON | `.entity()` |
| 结构化分析 | Text Analysis | 文本分析 | Use Case | 摘要、情感、实体提取 | `AnalysisFacade` |
| 结构化分析 | Text Analysis (Entity) | 文本分析结果 | Entity | 领域层富分析对象 | `TextAnalysis` |
| 结构化分析 | Text Analysis Result | 分析结果 | DTO | API 层分析响应 | `TextAnalysisResult` |
| 结构化分析 | Analysis Text | 分析文本 | Value Object | 校验后的分析输入 (≤50000 字) | `AnalysisText` |
| 结构化分析 | Language Hint | 语言提示 | Value Object | 响应语言提示 | `LanguageHint` |
| 结构化分析 | Structured Analysis Repository | 结构化分析仓储 | Port | LLM 结构化分析端口 | `StructuredAnalysisRepository` |
| 结构化分析 | Summary | 摘要 | Business Concept | 文本简要重述 | `summary` |
| 结构化分析 | Sentiment | 情感 | Enum | 情感分类 | `POSITIVE`, `NEUTRAL`, `NEGATIVE` |
| 结构化分析 | Key Points | 关键点 | Business Concept | 提取的核心要点 | `keyPoints` |
| 结构化分析 | Named Entities | 命名实体 | Business Concept | NLP 提取的命名实体 | `entities` |
| 对话评估 | Chat Evaluation | 对话评估 | Use Case | 评估回答质量与安全性 | `ChatQualityEvaluator` |
| 对话评估 | Chat Evaluation Result | 评估结果 | Value Object | 多维度评分与标记 | `ChatEvaluationResult` |
| 对话评估 | LLM-as-a-Judge | LLM 评判 | Pattern | 独立 LLM 评分模式 | `ChatQualityEvaluator` |
| 对话评估 | Relevancy Evaluator | 相关性评估器 | Evaluator | 衡量回答与问题相关性 | `RelevancyEvaluator` |
| 对话评估 | Fact-Checking Evaluator | 事实性评估器 | Evaluator | 对照上下文核查事实 | `FactCheckingEvaluator` |
| 对话评估 | Reference Documents | 参考文档 | Input Concept | 评估用上下文分块 | `referenceDocuments` |
| 对话评估 | Coherence Score | 连贯性分数 | Metric | 逻辑流畅度 (0–1) | `coherenceScore` |
| 对话评估 | Relevance Score | 相关性分数 | Metric | 问题相关度 (0–1) | `relevanceScore` |
| 对话评估 | Helpfulness Score | 有用性分数 | Metric | 实用价值 (0–1) | `helpfulnessScore` |
| 对话评估 | Factuality Score | 事实性分数 | Metric | 事实准确度 (0–1) | `factualityScore` |
| 对话评估 | Factuality Available | 事实性可用 | Metric / Flag | 是否执行了事实性评分 | `factualityAvailable` |
| 对话评估 | Overall Score | 综合分数 | Metric | 加权综合分 (0–1) | `overallScore` |
| 对话评估 | Safety Flag | 安全标记 | Metric | 潜在安全风险标识 | `safetyFlags` |
| 对话评估 | Evaluation Suggestions | 评估建议 | Business Concept | LLM 生成的改进建议 | `suggestions` |
| 对话评估 | Safety Evaluation | 安全评估 | Domain Behavior | 毒性/偏见/危险指令评估 | `SAFETY_EVALUATION_PROMPT` |
| 对话评估 | Evaluation ChatClient | 评估对话客户端 | Technical | 独立评估用 ChatClient | `evaluationChatClient` |
| 规划中 | Supervisor Agent | 协调器 Agent | Planned | 多 Agent 编排 | i18n `supervisor` |
| 规划中 | Kubernetes Agent | K8s Agent | Planned | 集群/部署运维 Agent | i18n `kubernetes` |
| 规划中 | Monitoring Agent | 监控 Agent | Planned | 指标与告警分析 Agent | i18n `monitoring` |
| 规划中 | Model Management Agent | 模型管理 Agent | Planned | 模型版本与推理管理 | i18n `model` |
| 规划中 | LLMOps Agent | LLMOps Agent | Planned | 训练/评估/微调工作流 | i18n `llmops` |
| 规划中 | AIOps Agent | AIOps Agent | Planned | 故障分析与自动化 | i18n `aiops` |
| 规划中 | Vector Database Management | 向量库管理 | Planned | 向量索引与检索运维 | i18n `vectordb` |
| API 错误码 | SESSION_NOT_FOUND | 会话不存在 | Error Code | 会话 ID 不存在 | HTTP 404 |
| API 错误码 | DOCUMENT_NOT_FOUND | 文档不存在 | Error Code | 文档 ID 不存在 | HTTP 404 |
| API 错误码 | AI_SERVICE_ERROR | AI 服务错误 | Error Code | LLM / 提供商失败 | HTTP 503 |
| API 错误码 | RAG_SERVICE_ERROR | RAG 服务错误 | Error Code | RAG 管道失败 | HTTP 500 |
| API 错误码 | IMAGE_PROVIDER_NOT_CONFIGURED | 图像提供商未配置 | Error Code | 图像生成不可用 | HTTP 503 |
| API 错误码 | TTS_PROVIDER_NOT_CONFIGURED | TTS 提供商未配置 | Error Code | 语音合成不可用 | HTTP 503 |
| API 错误码 | VALIDATION_ERROR | 校验错误 | Error Code | 请求体无效 | HTTP 400 |
| API 错误码 | CHAT_MEMORY_ERROR | 对话记忆错误 | Error Code | 记忆同步失败 | HTTP 500 |
| API 错误码 | FILE_TOO_LARGE | 文件过大 | Error Code | 上传超过 50MB | HTTP 413 |
| API 错误码 | INTERNAL_ERROR | 内部错误 | Error Code | 未处理服务端错误 | HTTP 500 |

*共 175 条 · 最后更新：2026-07-11*
