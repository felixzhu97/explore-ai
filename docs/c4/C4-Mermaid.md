---
title: AI Chat & Agent Platform - C4 模型
---

## C1 系统上下文图

**描述**: 系统与用户、外部系统的交互关系

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#4A90D9', 'primaryTextColor': '#fff', 'primaryBorderColor': '#2E5090'}}}%%
flowchart TB
    subgraph USER["👤 人员"]
        U["用户"]
    end

    subgraph PLATFORM["🎯 AI Chat & Agent Platform"]
        subgraph FRONTEND["🌐 Web Frontend"]
            FE["Angular 22\nWeb 用户界面"]
        end
        subgraph BACKEND["⚙️ AI Platform Backend"]
            BE["Spring Boot 4.0\nJava 25 / Spring AI 2.0"]
        end
    end

    subgraph EXTERNAL["🔌 外部系统"]
        DS["DeepSeek API\nLLM + Text Embedding"]
        PG["PostgreSQL + pgvector\n向量数据库，端口 5432"]
        OM["Ollama\n本地 Embedding，端口 11434"]
        DALLE["DALL-E API\n图像生成服务"]
        TTS["TTS Service\n语音合成服务"]
    end

    U -->|"对话 / 上传文档 / 调用工具"| FE
    FE -->|"HTTP/SSE 请求"| BE
    BE -->|"LLM 调用 / Embedding 生成"| DS
    BE -->|"向量搜索 / 文档存储"| PG
    BE -->|"本地 Embedding 生成"| OM
    BE -->|"图像生成请求"| DALLE
    BE -->|"语音合成请求"| TTS
```

## C2 容器图

**描述**: 后端的 7 个子域容器

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#4A90D9', 'primaryTextColor': '#fff', 'primaryBorderColor': '#2E5090'}}}%%
flowchart TB
    subgraph USER["👤 用户"]
        U["用户"]
    end

    subgraph FRONTEND["🌐 Web Frontend"]
        NG["Angular 22 + TypeScript\nWeb 用户界面，支持 SSE 实时对话"]
    end

    subgraph BACKEND["⚙️ AI Platform Backend (端口 9000)"]
        direction LR
        
        subgraph CHAT["💬 Chat Domain"]
            CHAT_CTRL["AiController\n@RestController"]
            CHAT_SVC["AiChatService\nDomain Service"]
            CHAT_SESSION["ChatSession\nAggregate Root"]
            CHAT_MSG["ChatMessage\nEntity"]
            CHAT_REPO["InMemoryChatSessionRepository"]
        end
        
        subgraph RAG["📚 RAG Domain"]
            RAG_CTRL["RagController\n@RestController"]
            RAG_SVC["RagService\nDomain Service"]
            DOC["Document\nEntity"]
            DOC_CHUNK["DocumentChunk\nEntity"]
            EMB_ADAPTER["EmbeddingAdapter\nInterface (Port)"]
            VEC_PORT["VectorSearchPort\nInterface (Port)"]
            DOC_REPO_PORT["DocumentRepository\nInterface (Port)"]
            OLLAMA_ADP["OllamaEmbeddingAdapter"]
            PG_ADP["PgVectorAdapter"]
            JPA_REPO["JpaDocumentRepository"]
            PDF_EXT["PdfTextExtractor"]
        end
        
        subgraph TOOL["🔧 Tool Calling Domain"]
            TOOL_CTRL["ToolCallingController\n@RestController"]
            RAG_TOOL["RagSearchTool\n*Tool"]
            WEATHER_TOOL["WeatherTools\n*Tool"]
        end
        
        subgraph IMAGE["🎨 Image Generation Domain"]
            IMG_CTRL["ImageController\n@RestController"]
            IMG_SVC["ImageGenerationService\nDomain Service"]
            IMG_ADP["OpenAiImageAdapter"]
        end
        
        subgraph AUDIO["🔊 Audio/TTS Domain"]
            AUD_CTRL["AudioController\n@RestController"]
            TTS_SVC["TextToSpeechService\nDomain Service"]
            TTS_ADP["OpenAiTtsAdapter"]
        end
        
        subgraph MCP_SRV["🖥️ MCP Server Domain"]
            MCP_CTRL["McpController\n@RestController"]
            MCP_SVC["McpServerService\nDomain Service"]
        end
        
        subgraph MCP_CLI["📡 MCP Client Domain"]
            MCP_CLI_CTRL["McpClientController\n@RestController"]
            MCP_CLI_SVC["AiMcpClientService\nDomain Service"]
        end
    end

    subgraph EXTERNAL["🔌 外部系统"]
        DS["DeepSeek API\nREST API"]
        PG["PostgreSQL + pgvector\n5432"]
        OM["Ollama\n11434"]
        DALLE["DALL-E API"]
        TTS_API["TTS Service"]
    end

    U -->|"对话 / 文档 / 工具"| NG
    NG -->|"聊天请求"| CHAT_CTRL
    NG -->|"RAG 请求"| RAG_CTRL
    NG -->|"工具调用"| TOOL_CTRL
    NG -->|"图像生成"| IMG_CTRL
    NG -->|"TTS 请求"| AUD_CTRL
    NG -->|"MCP Server"| MCP_CTRL
    NG -->|"MCP Client"| MCP_CLI_CTRL

    CHAT_CTRL --> CHAT_SVC
    CHAT_SVC --> CHAT_SESSION
    CHAT_SESSION --> CHAT_MSG
    CHAT_SVC --> CHAT_REPO
    CHAT_REPO --> DS

    RAG_CTRL --> RAG_SVC
    RAG_SVC --> EMB_ADAPTER
    RAG_SVC --> VEC_PORT
    RAG_SVC --> DOC_REPO_PORT
    RAG_SVC --> PDF_EXT
    EMB_ADAPTER --> OLLAMA_ADP
    VEC_PORT --> PG_ADP
    DOC_REPO_PORT --> JPA_REPO
    PG_ADP --> PG
    JPA_REPO --> PG
    OLLAMA_ADP --> OM
    DOC --> DOC_CHUNK

    TOOL_CTRL --> RAG_TOOL
    TOOL_CTRL --> WEATHER_TOOL
    RAG_TOOL --> RAG_SVC
    WEATHER_TOOL --> DS

    IMG_CTRL --> IMG_SVC
    IMG_SVC --> IMG_ADP
    IMG_ADP --> DALLE

    AUD_CTRL --> TTS_SVC
    TTS_SVC --> TTS_ADP
    TTS_ADP --> TTS_API

    MCP_CTRL --> MCP_SVC
    MCP_CLI_CTRL --> MCP_CLI_SVC
```

## C3 组件图 - 后端 (Spring Boot)

**描述**: 后端三层架构 (Interface / Domain / Infrastructure)

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#4A90D9', 'primaryTextColor': '#fff', 'primaryBorderColor': '#2E5090'}}}%%
flowchart TB
    subgraph BACKEND["⚙️ AI Platform Backend (Spring Boot 4.0 / Java 25 / Spring AI 2.0)"]
        direction LR
        
        subgraph IFACE["📥 Interface Layer (adapter/in)"]
            AI_CTRL["AiController\n@RestController"]
            RAG_CTRL["RagController\n@RestController"]
            TOOL_CTRL["ToolCallingController\n@RestController"]
            IMG_CTRL["ImageController\n@RestController"]
            AUD_CTRL["AudioController\n@RestController"]
            MCP_CTRL["McpController\n@RestController"]
            MCP_CLI_CTRL["McpClientController\n@RestController"]
            EX_HDLR["GlobalExceptionHandler\n@RestControllerAdvice"]
            
            subgraph DTOS["DTOs"]
                CHAT_REQ["ChatRequest"]
                CHAT_RESP["ChatResponse"]
                RAG_REQ["RagChatRequest"]
                RAG_RESP["RagChatResponse"]
                IMG_REQ["ImageGenerationRequest"]
                TTS_REQ["TtsRequest"]
            end
        end
        
        subgraph DOMAIN["🏛️ Domain Layer"]
            subgraph CHAT_D["💬 Chat Domain"]
                CHAT_SESSION["ChatSession\nAggregate Root"]
                CHAT_MSG["ChatMessage\nEntity"]
                AI_SVC["AiChatService\nDomain Service"]
            end
            
            subgraph RAG_D["📚 RAG Domain"]
                DOC["Document\nEntity"]
                DOC_CHUNK["DocumentChunk\nEntity"]
                RAG_SVC["RagService\nDomain Service"]
                LANG_DET["LanguageDetectionService"]
            end
            
            subgraph IMG_D["🎨 Image Domain"]
                IMG_SVC["ImageGenerationService"]
            end
            
            subgraph TTS_D["🔊 TTS Domain"]
                TTS_SVC["TextToSpeechService"]
            end
            
            subgraph MCP_D["📡 MCP Domain"]
                MCP_SRV_SVC["McpServerService"]
                MCP_CLI_SVC["AiMcpClientService"]
            end
            
            subgraph VO["📦 Value Objects"]
                SESSION_ID["ChatSessionId"]
                MSG_ID["MessageId"]
                DOC_ID["DocumentId"]
            end
            
            subgraph EXC["⚠️ Domain Exceptions"]
                AI_EXC["AiServiceException"]
                SESS_EXC["ChatSessionNotFoundException"]
                DOC_EXC["DocumentNotFoundException"]
            end
            
            subgraph PORTS["🔌 Domain Ports"]
                CHAT_REPO_PORT["ChatSessionRepository\nInterface"]
            end
        end
        
        subgraph INFRA["🔧 Infrastructure Layer (adapter/out)"]
            subgraph CFG["Configuration"]
                APP_CFG["ApplicationConfig"]
                PG_CFG["PostgresConfig"]
                OLLAMA_CFG["OllamaConfig"]
                CORS_CFG["WebCorsConfig"]
            end
            
            subgraph PERSIST["💾 Persistence"]
                IN_MEM_REPO["InMemoryChatSessionRepository"]
                JPA_REPO["JpaDocumentRepository"]
                DOC_ENTITY["DocumentEntity"]
                CHUNK_ENTITY["DocumentChunkEntity"]
            end
            
            subgraph AI_ADP["🤖 AI Adapters"]
                OLLAMA_ADP["OllamaEmbeddingAdapter"]
                MOCK_ADP["MockEmbeddingAdapter"]
                EMB_PORT["EmbeddingAdapter\nInterface"]
            end
            
            subgraph VEC_ADP["🔍 Vector Adapters"]
                PG_VEC_ADP["PgVectorAdapter"]
            end
            
            subgraph DOC_ADP["📄 Document Adapters"]
                PDF_EXT["PdfTextExtractor"]
            end
            
            subgraph IMG_ADP["🎨 Image Adapters"]
                OPENAI_IMG["OpenAiImageAdapter"]
            end
            
            subgraph TTS_ADP["🔊 TTS Adapters"]
                OPENAI_TTS["OpenAiTtsAdapter"]
            end
            
            subgraph TOOL_IMPL["🔧 Tool Implementations"]
                RAG_TOOL["RagSearchTool"]
                WEATHER["WeatherTools"]
            end
        end
    end

    subgraph EXTERNAL["🔌 外部系统"]
        DS_API["DeepSeek API"]
        PG_DB["PostgreSQL + pgvector"]
        OM_SVC["Ollama"]
        DALLE_API["DALL-E API"]
        TTS_API["TTS Service"]
    end

    %% Interface -> Domain
    AI_CTRL --> AI_SVC
    RAG_CTRL --> RAG_SVC
    TOOL_CTRL --> RAG_TOOL
    TOOL_CTRL --> WEATHER
    IMG_CTRL --> IMG_SVC
    AUD_CTRL --> TTS_SVC
    MCP_CTRL --> MCP_SRV_SVC
    MCP_CLI_CTRL --> MCP_CLI_SVC
    AI_CTRL --> EX_HDLR
    RAG_CTRL --> EX_HDLR

    %% Domain Internal
    AI_SVC --> CHAT_SESSION
    CHAT_SESSION --> CHAT_MSG
    AI_SVC --> CHAT_REPO_PORT
    CHAT_REPO_PORT --> IN_MEM_REPO
    
    RAG_SVC --> DOC
    DOC --> DOC_CHUNK
    RAG_SVC --> LANG_DET
    RAG_SVC --> EMB_PORT
    
    IMG_SVC --> OPENAI_IMG
    TTS_SVC --> OPENAI_TTS

    %% Infrastructure -> External
    IN_MEM_REPO --> DS_API
    OLLAMA_ADP --> OM_SVC
    PG_VEC_ADP --> PG_DB
    JPA_REPO --> PG_DB
    OPENAI_IMG --> DALLE_API
    OPENAI_TTS --> TTS_API
    WEATHER --> DS_API

    %% Tool -> Domain Service
    RAG_TOOL --> RAG_SVC
```

## C3 组件图 - 前端 (Angular 22)

**描述**: Angular 前端的组件层级结构

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#4A90D9', 'primaryTextColor': '#fff', 'primaryBorderColor': '#2E5090'}}}%%
flowchart TB
    subgraph FRONTEND["🌐 Angular 22 Web Application"]
        direction LR
        
        subgraph APP["📱 App Layer"]
            APP_ROOT["AppComponent\n根组件"]
            APP_CFG["app.config.ts\n应用配置"]
            APP_ROUTES["app.routes.ts\n路由映射"]
        end
        
        subgraph PAGES["📄 Page Components"]
            direction LR
            
            AI_INFRA["AIInfraPanelComponent\n路由 /ai-infra"]
            RAG_CHAT["RagChatComponent\n路由 /rag"]
            VISION["VisionPanelComponent\n路由 /vision"]
            AI_HUB["AiHubComponent\n路由 /aihubs"]
            
            subgraph AI_INFRA_TABS["AIInfraPanel Sub-Tabs"]
                MCP_TAB["mcp-tools-tab"]
                FC_TAB["fc-playground-tab"]
            end
            
            subgraph AI_HUB_TABS["AiHubComponent Sub-Tabs"]
                CHAT_TAB["chat-tab"]
                TTS_TAB["tts-tab"]
                IMG_TAB["image-gen-tab"]
            end
        end
        
        subgraph AGENTS["🤖 Agent Components"]
            AGENT_PANEL["AgentPanelComponent"]
            AGENT_CHAT["AgentChatComponent\nSSE 流式"]
            CHAT_MSG["ChatMessageComponent\nMarkdown 渲染"]
            TOOL_RESULT["ToolResultComponent"]
            STATUS_BADGE["StatusBadgeComponent"]
        end
        
        subgraph CORE["⚡ Core Services"]
            API_SVC["ApiService\n@Injectable"]
            FUNCALL_SVC["FunctionCallService\nSSE 解析"]
            MCP_SVC["McpService\nJSON-RPC"]
            NOTIF_SVC["NotificationService\nToast"]
            HTTP_ERR["HttpErrorInterceptor"]
        end
        
        subgraph SHARED["🔧 Shared Layer"]
            CARD["Card\nUI 组件"]
            BTN["Button\nUI 组件"]
            TOAST["Toast\n通知组件"]
            THEME_SVC["ThemeService"]
            I18N_SVC["I18nService\n5 种语言"]
        end
    end

    subgraph BACKEND["🔌 后端 & 浏览器"]
        BACKEND_API["AI Platform Backend\nREST/SSE :9000"]
        BROWSER["Browser APIs\nHttpClient, SSE"]
    end

    %% Routing
    APP_ROUTES -->|"路由 /ai-infra"| AI_INFRA
    APP_ROUTES -->|"路由 /rag"| RAG_CHAT
    APP_ROUTES -->|"路由 /vision"| VISION
    APP_ROUTES -->|"路由 /aihubs"| AI_HUB
    APP_ROOT --> APP_ROUTES

    %% Page -> Sub Tabs
    AI_INFRA --> MCP_TAB
    AI_INFRA --> FC_TAB
    AI_HUB --> CHAT_TAB
    AI_HUB --> TTS_TAB
    AI_HUB --> IMG_TAB

    %% Page -> Agent Components
    AI_INFRA --> AGENT_PANEL
    AI_INFRA --> AGENT_CHAT
    AI_INFRA --> STATUS_BADGE

    %% Agent Component Tree
    AGENT_PANEL --> AGENT_CHAT
    AGENT_CHAT --> CHAT_MSG
    AGENT_CHAT --> TOOL_RESULT
    AGENT_CHAT --> STATUS_BADGE

    %% Services
    AGENT_CHAT --> FUNCALL_SVC
    AGENT_CHAT --> API_SVC
    AGENT_CHAT --> MCP_SVC
    AGENT_CHAT --> NOTIF_SVC

    FUNCALL_SVC -->|"SSE 流 /api/mcp/function-call/stream"| BACKEND_API
    MCP_SVC -->|"JSON-RPC /api/mcp/*"| BACKEND_API
    API_SVC -->|"REST API proxy /api/* → :9000"| BACKEND_API

    RAG_CHAT --> API_SVC
    VISION --> API_SVC
    AI_HUB --> API_SVC

    NOTIF_SVC --> TOAST
    APP_ROOT --> THEME_SVC
    APP_ROOT --> I18N_SVC
    APP_ROOT --> HTTP_ERR
```

## C4 部署图

**描述**: 本地开发环境部署架构

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#4A90D9', 'primaryTextColor': '#fff', 'primaryBorderColor': '#2E5090'}}}%%
flowchart TB
    subgraph WORKSTATION["💻 Developer Workstation (localhost)"]
        direction LR
        
        subgraph DOCKER["🐳 Docker Engine"]
            PG["PostgreSQL + pgvector\npgvector/pgvector:pg17\n端口: 5432"]
        end
        
        subgraph JAVA_ENV["☕ JDK 25 (Gradle Build)"]
            BACKEND["Spring Boot Backend\neclipse-temurin:25-jre-alpine\n端口: 9000"]
        end
        
        subgraph NODE_ENV["📦 Node 22 + pnpm"]
            NG_DEV["Angular 22 Dev Server\nAngular CLI\n端口: 4200"]
        end
    end

    subgraph DEEP_SEEK["🔥 DeepSeek API"]
        DS["DeepSeek API\nREST API\nHTTPS"]
    end

    subgraph OLLAMA_SRV["🦙 Ollama (Local or Docker)"]
        OM["Ollama\nREST API\n端口: 11434"]
    end

    subgraph DALLE_SRV["🎨 DALL-E API (OpenAI)"]
        DALLE["DALL-E API\nREST API\nHTTPS"]
    end

    subgraph TTS_SRV["🔊 TTS Service (OpenAI)"]
        TTS["TTS Service\nREST API\nHTTPS"]
    end

    USER([👤 Developer / End User])

    USER -->|"HTTP"| NG_DEV
    NG_DEV -->|"HTTP/SSE /api/* → :9000"| BACKEND
    BACKEND -->|"JDBC"| PG
    BACKEND -.->|"LLM Chat + Embedding"| DS
    BACKEND -.->|"Embedding Generation"| OM
    BACKEND -.->|"Image Generation"| DALLE
    BACKEND -.->|"Text-to-Speech"| TTS
```

## 部署端口汇总

| 服务 | 端口 | 技术栈 |
|------|------|--------|
| Spring Boot Backend | **9000** | Java 25 / Spring Boot 4.0 / Spring AI 2.0 |
| PostgreSQL + pgvector | 5432 | PostgreSQL 17 / pgvector |
| Ollama (Embedding) | 11434 | nomic-embed-text |
| Angular Dev Server | 4200 | Angular 22 + TypeScript |
| DeepSeek API | HTTPS | LLM + Text Embedding |
| DALL-E API | HTTPS | 图像生成 |
| TTS Service | HTTPS | 语音合成 |
