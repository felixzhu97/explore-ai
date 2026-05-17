# Architecture Overview

## System Architecture

The AI-Test Platform is a full-stack application with a microservices-inspired architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│                    (React + Vite SPA)                           │
│                       Port: 5173                                │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                           │
│                   (Express.js Server)                           │
│                     Ports: 3000-3001                            │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AI Agents Service Layer                       │
│               (Python + FastAPI + LangGraph)                     │
│                      Port: 8003                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Supervisor Agent                       │   │
│  │         (Central Coordinator - Task Routing)            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                    │                                             │
│    ┌───────┬───────┼───────┬───────┬───────┬───────┬───────┐  │
│    ▼       ▼       ▼       ▼       ▼       ▼       ▼       ▼  │
│ ┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐
│ │K8s  ││Vec  ││RAG  ││Pipe ││LLM  ││AI   ││Feat ││Moni ││Model│
│ │     ││torDB││     ││line ││Ops  ││Ops  ││ure  ││tor  ││     │
│ └─────┘└─────┘└─────┘└─────┘└─────┘└─────┘└─────┘└─────┘└─────┘
│    │       │       │       │       │       │       │       │     │
│    └───────┴───────┴───────┴───────┴───────┴───────┴───────┴─────┘
│                              │                                    │
│                    ┌─────────┴─────────┐                           │
│                    ▼                   ▼                          │
│            ┌───────────┐       ┌───────────────┐                  │
│            │ HTTP Tools│       │ System Tools │                  │
│            │ (API调用) │       │ (命令执行)   │                  │
│            └───────────┘       └───────────────┘                  │
│                              │                                    │
│                    ┌─────────┴─────────┐                          │
│                    ▼                   ▼                          │
│            ┌───────────┐       ┌───────────────┐                 │
│            │ LangGraph │       │ 50+ Tools    │                 │
│            │ Workflows │       │ (Specialized) │                 │
│            └───────────┘       └───────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Vision Service Layer                           │
│                 (FastAPI + Python)                               │
│                      Port: 8002                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │     YOLO     │  │     BLIP     │  │      PaddleOCR       │  │
│  │   Detector   │  │  Captioner   │  │      Processor       │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                  │
┌─────────────────────────────────────────────────────────────────┐
│                     RAG Service Layer                            │
│                 (FastAPI + Python)                               │
│                      Port: 8001                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   Document   │  │   Embedding  │  │      LLM Gateway     │  │
│  │    Loader   │  │    Model     │  │ (OpenAI/Claude/Ollama)│  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   Persistence Layer                        │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │  │
│  │  │Cache Manager │  │Session Store │  │Doc Metadata   │  │  │
│  │  │(Redis/Memory)│  │  (SQLite)    │  │  (SQLite)    │  │  │
│  │  └──────────────┘  └──────────────┘  └───────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
```

## AI Agents Multi-Agent System

### Architecture Overview

The AI Agents service uses a **Supervisor-based routing pattern** where a central Supervisor Agent coordinates specialized agents:

```
┌─────────────────────────────────────────────────────────────┐
│                    Supervisor Agent                          │
│            (Central Coordinator & Router)                   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Intent Detection → Agent Selection → Delegation    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  K8s Agent    │   │  VectorDB     │   │  RAG Agent    │
│  (Cluster)    │   │  (Embeddings)  │   │  (Documents)  │
└───────────────┘   └───────────────┘   └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                              ▼
                    ┌───────────────┐
                    │   Tools       │
                    ├───────────────┤
                    │ - HTTP Tools  │
                    │ - System Tools│
                    │ - K8s Tools   │
                    │ - Monitoring  │
                    │ - AIOps Tools│
                    │ - Pipeline   │
                    │ - Feature    │
                    │   Store      │
                    └───────────────┘
                              │
                              ▼
                    ┌───────────────┐
                    │ LangGraph     │
                    │ Workflows     │
                    ├───────────────┤
                    │ - RAG Graph   │
                    │ - LLMOps Graph│
                    │ - AIOps Graph │
                    └───────────────┘
```

### Agent Descriptions


| Agent             | Responsibility         | Key Capabilities                                     |
| ----------------- | ---------------------- | ---------------------------------------------------- |
| **Supervisor**    | Central coordinator    | Intent detection, task routing, result aggregation   |
| **K8s**           | Kubernetes management  | Pod/Service/Deployment operations, scaling           |
| **VectorDB**      | Vector database ops    | Embeddings, similarity search, collection management |
| **RAG**           | Document retrieval     | Knowledge base management, document indexing         |
| **Pipeline**      | Workflow orchestration | DAG execution, step management                       |
| **LLMOps**        | LLM operations         | Training, fine-tuning, evaluation                    |
| **AIOps**         | Intelligent operations | Anomaly detection, root cause analysis               |
| **Feature Store** | Feature engineering    | Feature registration, materialization                |
| **Monitoring**    | Observability          | Metrics, logs, alerting                              |
| **Model**         | ML model lifecycle     | Version control, deployment, inference               |
| **TTS**           | Text-to-speech         | Speech synthesis, voice management, streaming         |
| **Video**         | Video generation       | Text-to-video, status tracking, provider management  |


### Routing Configuration

The Supervisor uses keyword-based routing to delegate tasks:

```
用户输入 → Supervisor → 关键词匹配 → 专业智能体

路由关键词映射:
├── "vector", "embedding", "search"    → VectorDB Agent
├── "k8s", "kubernetes", "pod", "cluster" → K8s Agent
├── "monitor", "metric", "alert"        → Monitoring Agent
├── "model", "deploy", "ml", "version"  → Model Agent
├── "rag", "document", "knowledge"     → RAG Agent
├── "llmops", "train", "evaluate"      → LLMOps Agent
├── "feature", "materialize"            → Feature Store Agent
├── "pipeline", "workflow", "dag"       → Pipeline Agent
├── "aiops", "anomaly", "incident"      → AIOps Agent
├── "tts", "speech", "voice", "synthesize" → TTS Agent
├── "video", "generate", "animation"    → Video Agent
```

### Tools Structure

#### HTTP Tools (`http_tools.py`)

Generic HTTP API call functionality:

- **http_request**: Make REST API calls with configurable method, headers, and body

#### System Tools (`system_tools.py`)

Local system command execution:

- **execute_command**: Run shell commands (kubectl, docker, git, etc.)

#### Specialized Tools


| Tool                     | Purpose                    | Key Functions                                                                                        |
| ------------------------ | -------------------------- | ---------------------------------------------------------------------------------------------------- |
| `k8s_tools.py`           | Kubernetes API operations  | list_pods, get_pod_logs, scale_deployment, create_service                                            |
| `vector_tools.py`        | Vector database operations | create_collection, upsert_vectors, search, get_collection_info                                       |
| `monitoring_tools.py`    | Prometheus/Grafana queries | query_metrics, get_prometheus_alerts, get_grafana_dashboard                                          |
| `model_tools.py`         | MLflow integration         | register_model, get_model_versions, deploy_model, predict                                            |
| `llmops_tools.py`        | Experiment tracking        | create_experiment, log_metrics, compare_experiments                                                  |
| `aiops_tools.py`         | Intelligent operations     | detect_anomaly, list_incidents, create_incident, root_cause_analysis, search_logs, get_system_health |
| `rag_tools.py`           | Document operations        | search_documents, ingest_document, delete_document, list_collections                                 |
| `pipeline_tools.py`      | Workflow management        | create_pipeline, run_pipeline, list_pipelines, get_run_status, add_step, cancel_run                  |
| `feature_store_tools.py` | Feature engineering        | create_feature_group, register_feature, get_feature_vector, materialize_features, write_features     |
| `tts_tools.py`           | Text-to-speech operations | tts_synthesize, tts_list_voices, tts_stream, tts_get_providers                                         |
| `video_tools.py`         | Video generation operations | video_generate, video_generate_advanced, video_check_status, video_get_providers                      |


##### AIOps Tools Detail (`aiops_tools.py`)

Comprehensive intelligent operations tools for incident management and system health:


| Function                    | Description                                               |
| --------------------------- | --------------------------------------------------------- |
| `aiops_detect_anomaly`      | Detect anomalies in metrics with configurable sensitivity |
| `aiops_list_incidents`      | List incidents with status/severity filtering             |
| `aiops_create_incident`     | Create new incidents with affected systems                |
| `aiops_get_system_health`   | Get overall system health overview                        |
| `aiops_root_cause_analysis` | Perform RCA for incidents with recommendations            |
| `aiops_search_logs`         | Search logs across services                               |
| `aiops_acknowledge_alert`   | Acknowledge alerts                                        |


##### Pipeline Tools Detail (`pipeline_tools.py`)

ML/DevOps pipeline orchestration tools:


| Function            | Description                                    |
| ------------------- | ---------------------------------------------- |
| `create_pipeline`   | Define multi-step workflows with DAG structure |
| `run_pipeline`      | Execute pipelines with parameters              |
| `list_pipelines`    | List available pipelines                       |
| `get_pipeline`      | Get pipeline details and step configuration    |
| `get_run_status`    | Check pipeline execution status                |
| `list_runs`         | List pipeline runs with filters                |
| `cancel_run`        | Cancel running pipelines                       |
| `add_pipeline_step` | Extend existing pipelines                      |
| `delete_pipeline`   | Remove unused pipelines                        |


##### Feature Store Tools Detail (`feature_store_tools.py`)

Feature engineering and management tools:


| Function                | Description                                   |
| ----------------------- | --------------------------------------------- |
| `create_feature_group`  | Create feature groups with entity definitions |
| `register_feature`      | Register new features in groups               |
| `get_feature_vector`    | Retrieve feature vectors for ML models        |
| `write_features`        | Store computed feature values                 |
| `materialize_features`  | Backfill historical features for training     |
| `list_feature_groups`   | List available feature collections            |
| `create_transformation` | Define custom feature engineering logic       |


##### TTS Tools Detail (`tts_tools.py`)

Text-to-speech synthesis tools for voice generation:


| Function            | Description                                           |
| ------------------- | ----------------------------------------------------- |
| `tts_synthesize`    | Convert text to speech with configurable voice/speed  |
| `tts_list_voices`   | List available TTS voices with language filtering     |
| `tts_stream`        | Stream synthesized speech in real-time                |
| `tts_get_providers` | Get information about available TTS providers          |


##### Video Tools Detail (`video_tools.py`)

Video generation tools for text-to-video synthesis:


| Function                  | Description                                              |
| ------------------------- | -------------------------------------------------------- |
| `video_generate`          | Generate video from text description                     |
| `video_generate_advanced` | Generate video with advanced parameters (style, seed)    |
| `video_check_status`      | Check video generation task status and retrieve results  |
| `video_get_providers`     | Get information about available video generation providers |


#### LangGraph Workflows (`graphs/`)

Advanced multi-stage workflows for complex operations:


| Graph             | Purpose                                             |
| ----------------- | --------------------------------------------------- |
| `rag_graph.py`    | RAG workflows: simple, multi-hop, hybrid, iterative |
| `llmops_graph.py` | LLM operations and experiment tracking workflows    |
| `aiops_graph.py`  | AIOps incident response workflows                   |
| `video_graph.py`  | Video generation workflows with status tracking      |


## Component Details

### 1. Web Frontend (`apps/web`)

A single-page application built with React 18 and Vite, designed with Apple-style aesthetics.

**Responsibilities:**

- User interface for agent interactions
- Agent panel management (K8s, Monitoring, VectorDB, AIInfra, etc.)
- Real-time chat with streaming responses
- Multi-language internationalization (i18n)

**Tech Stack:**

- React 18
- Vite (bundler)
- TypeScript
- Emotion CSS-in-JS

**Key Components:**

- `components/panels/` - Agent panels (K8sPanel, VectorDBPanel, MonitoringPanel, AIInfraPanel, etc.)
- `components/agents/` - Chat components (AgentChat, ChatMessage, ToolResult, StatusBadge)
- `components/panels/AIInfraPanel.tsx` - AI Infrastructure management panel
- `components/AIHub.tsx` - AI capabilities hub with Chat, Image Generation, and TTS tabs
- `i18n/` - Internationalization with support for English, Chinese, Japanese, French, Spanish
- `theme.ts` - Centralized theme system with colors, typography, spacing, shadows
- `GlobalStyles.tsx` - Global CSS styles

**i18n Support:**

- Browser language auto-detection
- Persistent language preference (localStorage)
- Supported languages: en, zh, ja, fr, es
- React Context-based provider pattern

**Theme System:**

- Apple-inspired design tokens
- Color palette: primary, surface, text, semantic colors
- Typography: SF Pro Display/Text, responsive font sizes
- Shadows, spacing, border radius, transitions
- CSS custom properties export

### 2. Backend Server (`apps/server`)

An Express.js server providing utility endpoints.

**Responsibilities:**

- Health check endpoint
- Random ID generation
- Utility functions (clamp, delay)

### 3. AI Agents Service (`services/ai_agents`)

The core multi-agent orchestration service.

**Responsibilities:**

- Supervisor-based agent coordination
- Task routing and delegation
- Tool execution and result aggregation
- LangGraph workflow management

**Tech Stack:**

- FastAPI + Python 3.10+
- LangChain / LangGraph
- Ollama (local LLM)

**Features:**

- 12 specialized agents (Supervisor, K8s, VectorDB, RAG, Pipeline, LLMOps, AIOps, Feature Store, Monitoring, Model, TTS, Video)
- 58+ specialized tools across all agents
- LangGraph workflows for complex multi-stage operations
- HTTP and system command tools
- Streaming responses via SSE
- Agent-to-agent delegation

### 4. Vision Service (`services/vision-service`)

Computer vision and image generation capabilities.

**Responsibilities:**

- Image processing and validation
- Model inference (YOLO, BLIP, PaddleOCR)
- Image generation (Stable Diffusion)
- Video generation (Sora, Pika, Runway, Kling)
- REST API endpoints

### 5. RAG Service (`services/rag`)

Retrieval-Augmented Generation service with enhanced persistence layer.

**Responsibilities:**

- Document ingestion and processing
- Semantic vector search with Qdrant
- LLM-powered question answering
- Conversation history management
- Multi-layer caching for performance

**Persistence Layer:**


| Component              | Description                                          |
| ---------------------- | ---------------------------------------------------- |
| `cache_manager.py`     | Multi-layer cache manager with Redis/Memory backends |
| `session_store.py`     | SQLite-based session persistence for chat history    |
| `document_metadata.py` | Document metadata tracking and indexing history      |


**Cache Manager (`cache_manager.py`):**

- LLM response caching (TTL: 1 hour)
- Retrieval result caching (TTL: 30 minutes)
- Embedding caching (TTL: 24 hours)
- Supports Redis and in-memory backends
- Hash-based cache keys for efficient lookups
- Cache statistics and invalidation APIs

**Session Store (`session_store.py`):**

- SQLite-based chat session persistence
- Session CRUD operations with title/message tracking
- Full-text search across sessions
- Session export to JSON
- Automatic cleanup of old sessions (configurable retention)

**Document Metadata Store (`document_metadata.py`):**

- Document record tracking (title, source, size, status)
- Chunk-level metadata management
- Indexing history with duration tracking
- Version control for incremental updates
- Status tracking: pending, indexing, completed, failed

### 6. Text Service (`services/text-service`)

Text-to-Text LLM service with multi-provider support.

**Responsibilities:**

- Text completion with multiple LLM providers
- Chat completion with session management
- Streaming responses (SSE)
- Provider abstraction (OpenAI, Anthropic, Ollama)

**Tech Stack:**

- FastAPI + Python
- LangChain / OpenAI / Anthropic SDK
- Ollama (local LLM)

**Features:**

- Multi-provider support (OpenAI GPT, Anthropic Claude, Ollama)
- Text completion and chat completion
- Streaming responses via SSE
- Session-based conversation history

### 7. TTS Service (`services/tts-service`)

Text-to-Speech service with multiple provider support.

**Responsibilities:**

- Speech synthesis with multiple providers
- Voice listing and management
- Streaming audio output

**Tech Stack:**

- FastAPI + Python
- Azure Cognitive Services / Google Cloud TTS / ElevenLabs / Coqui

**Features:**

- Multi-provider support (Azure, Google, ElevenLabs, Coqui)
- Multi-language voice synthesis
- Streaming audio output

### 8. Media Gen Service (`services/media-gen`)

Local Text-to-Image generation using Stable Diffusion.

**Responsibilities:**

- Text-to-image generation
- Model caching and memory management
- Local inference (no external API required)

**Tech Stack:**

- FastAPI + Python
- Diffusers library
- Stable Diffusion models

**Features:**

- Local inference (privacy-preserving)
- Configurable steps, guidance scale, seed
- CUDA/MPS/CPU device support

## Data Flow

### AI Agents Request Flow

```
User Input → React App → HTTP POST → AI Agents Service (8003)
                                          │
                                          ▼
                                    Supervisor Agent
                                          │
                                          ▼
                              Intent Detection & Routing
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
              K8s Agent              VectorDB Agent        Monitoring Agent
                    │                     │                     │
                    ▼                     ▼                     ▼
              K8s Tools             Vector Tools          Monitoring Tools
                    │                     │                     │
                    └─────────────────────┼─────────────────────┘
                                          │
                                          ▼
                                    Result Aggregation
                                          │
                                          ▼
                              Streaming Response (SSE)
                                          │
                                          ▼
                                    React App Display
```

## Directory Structure

```
ai-test/
├── apps/
│   ├── web/                    # React frontend
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── agents/     # Agent chat components
│   │   │   │   │   ├── AgentChat.tsx
│   │   │   │   │   ├── ChatMessage.tsx
│   │   │   │   │   ├── ToolResult.tsx
│   │   │   │   │   └── StatusBadge.tsx
│   │   │   │   ├── panels/     # Agent management panels
│   │   │   │   │   ├── K8sPanel.tsx
│   │   │   │   │   ├── VectorDBPanel.tsx
│   │   │   │   │   ├── MonitoringPanel.tsx
│   │   │   │   │   ├── AIInfraPanel.tsx      # AI Infrastructure
│   │   │   │   │   ├── AIOpsPanel.tsx
│   │   │   │   │   ├── LLMOpsPanel.tsx
│   │   │   │   │   ├── ModelPanel.tsx
│   │   │   │   │   └── SupervisorPanel.tsx
│   │   │   │   └── AIHub.tsx  # AI Capabilities Hub (Chat, Image, TTS)
│   │   │   ├── i18n/          # Internationalization
│   │   │   │   ├── index.tsx   # i18n provider
│   │   │   │   └── locales.ts   # Translation strings
│   │   │   ├── theme.ts        # Theme system
│   │   │   ├── GlobalStyles.tsx # Global styles
│   │   │   └── App.tsx
│   │   └── package.json
│   └── server/                 # Express.js server
│       ├── src/
│       │   └── index.ts
│       └── package.json
├── packages/
│   ├── config/                 # Shared TypeScript config
│   └── utils/                  # Shared utilities
├── services/
│   ├── ai_agents/              # AI Agents service (new architecture)
│   │   ├── agents/             # 12 specialized agents
│   │   │   ├── supervisor.py   # Central coordinator
│   │   │   ├── k8s_agent.py    # Kubernetes management
│   │   │   ├── vector_db_agent.py  # Vector operations
│   │   │   ├── rag_agent.py    # RAG operations
│   │   │   ├── pipeline_agent.py # Pipeline orchestration
│   │   │   ├── llmops_agent.py # LLM operations
│   │   │   ├── aiops_agent.py  # AI operations
│   │   │   ├── feature_store_agent.py # Feature engineering
│   │   │   ├── monitoring_agent.py # Observability
│   │   │   ├── model_agent.py   # ML model lifecycle
│   │   │   ├── tts_agent.py     # Text-to-speech synthesis
│   │   │   └── video_agent.py   # Video generation
│   │   ├── core/               # Base classes, prompts, schemas
│   │   │   ├── base.py         # Agent base class
│   │   │   ├── prompts.py      # Prompt templates
│   │   │   └── schemas.py      # Pydantic schemas
│   │   ├── tools/              # All tool implementations
│   │   │   ├── http_tools.py   # HTTP API calls
│   │   │   ├── system_tools.py # Shell commands
│   │   │   ├── k8s_tools.py    # Kubernetes operations
│   │   │   ├── vector_tools.py # VectorDB operations
│   │   │   ├── monitoring_tools.py # Prometheus/Grafana
│   │   │   ├── model_tools.py  # MLflow integration
│   │   │   ├── llmops_tools.py # Experiment tracking
│   │   │   ├── aiops_tools.py  # Incident management
│   │   │   ├── rag_tools.py    # Document operations
│   │   │   ├── pipeline_tools.py # Workflow management
│   │   │   ├── feature_store_tools.py # Feature engineering
│   │   │   ├── tts_tools.py    # Text-to-speech synthesis
│   │   │   └── video_tools.py   # Video generation
│   │   ├── graphs/             # LangGraph workflows
│   │   │   ├── rag_graph.py    # RAG workflows
│   │   │   ├── llmops_graph.py # LLM operations
│   │   │   ├── aiops_graph.py # AIOps workflows
│   │   │   └── video_graph.py  # Video generation workflows
│   │   ├── examples/           # Example usage code
│   │   │   └── video_agent_examples.py  # Video Agent usage examples
│   │   └── main.py             # FastAPI app entry
│   ├── vision-service/         # Vision AI service
│   │   ├── src/
│   │   │   ├── main.py        # FastAPI app entry
│   │   │   ├── api/
│   │   │   │   └── vision.py  # Vision API routes
│   │   │   ├── models/
│   │   │   │   ├── yolo_detector.py
│   │   │   │   ├── blip_captioner.py
│   │   │   │   └── paddle_ocr.py
│   │   │   ├── schemas/
│   │   │   │   └── vision.py  # Pydantic models
│   │   │   └── core/
│   │   │       └── config.py  # Settings
│   │   └── ...
│   └── rag/                    # RAG service
│       ├── src/
│       │   ├── main.py         # FastAPI app entry
│       │   ├── api/
│       │   │   ├── documents.py # Document API
│       │   │   └── chat.py     # Chat API
│       │   ├── core/
│       │   │   ├── llm_gateway.py   # LLM abstraction
│       │   │   ├── embedding.py    # Embedding model
│       │   │   └── vector_store.py # Qdrant integration
│       │   ├── services/
│       │   │   ├── ingestion.py    # Document ingestion
│       │   │   └── rag_chain.py    # RAG chain
│       │   ├── persistence/        # Persistence layer
│       │   │   ├── cache_manager.py      # Multi-layer cache
│       │   │   ├── session_store.py      # Session persistence
│       │   │   └── document_metadata.py  # Document metadata
│       │   └── document_loader/
│       │       └── loader.py       # Document loaders
│       └── ...
└── docs/                       # Documentation
```

## API Reference

### AI Agents Service (Port 8003)


| Method | Endpoint                          | Description               |
| ------ | --------------------------------- | ------------------------- |
| `GET`  | `/health`                         | Health check              |
| `GET`  | `/agents`                         | List all available agents |
| `POST` | `/api/agents/supervisor/invoke`   | Invoke supervisor (chat)  |
| `POST` | `/api/agents/{agent_name}/invoke` | Invoke specific agent     |


### Vision Service (Port 8002)


| Method | Endpoint              | Description                      |
| ------ | --------------------- | -------------------------------- |
| `GET`  | `/health`             | Health check                     |
| `GET`  | `/`                   | Service info                     |
| `POST` | `/vision/detect`      | Object detection (YOLO)          |
| `POST` | `/vision/caption`     | Image captioning (BLIP)          |
| `POST` | `/vision/ocr`         | Text extraction (PaddleOCR)      |
| `POST` | `/vision/analyze`     | Combined analysis                |
| `POST` | `/image-gen/generate` | Text-to-image (Stable Diffusion) |
| `POST` | `/video/generate`     | Text/image to video              |


### Text Service (Port 8004)


| Method | Endpoint              | Description           |
| ------ | --------------------- | --------------------- |
| `GET`  | `/api/text/health`    | Health check          |
| `GET`  | `/api/text/providers` | List LLM providers    |
| `GET`  | `/api/text/models`    | List available models |
| `POST` | `/api/text/complete`  | Text completion       |
| `POST` | `/api/text/chat`      | Chat completion       |


### TTS Service (Port 8004+)


| Method | Endpoint          | Description           |
| ------ | ----------------- | --------------------- |
| `GET`  | `/tts/health`     | Health check          |
| `GET`  | `/tts/voices`     | List available voices |
| `GET`  | `/tts/providers`  | List TTS providers    |
| `POST` | `/tts/synthesize` | Synthesize speech     |
| `POST` | `/tts/stream`     | Stream speech         |


### RAG Service (Port 8001)


| Method | Endpoint            | Description      |
| ------ | ------------------- | ---------------- |
| `POST` | `/documents/ingest` | Ingest documents |
| `POST` | `/chat`             | Chat with RAG    |


## Configuration

### AI Agents Configuration

```env
# Ollama LLM Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b
```

### Vision Service Configuration

```env
DEVICE=cuda                    # 'cuda' or 'cpu'
YOLO_MODEL=yolo11n.pt          # YOLO model path
BLIP_MODEL=Salesforce/blip-image-captioning-large
OCR_LANG=ch                    # OCR languages
MAX_IMAGE_SIZE=10485760        # 10MB max file size
MODEL_CACHE_DIR=./models       # Model cache location
MAX_CONCURRENT_REQUESTS=4      # Request queue limit
```

## Deployment Options

1. **Docker Compose (Recommended)**
  - GPU variant for production
  - CPU variant for development/low-resource environments
2. **Kubernetes**
  - Use GPU node pools for AI service
  - Scale horizontally with load balancer
3. **Cloud Services**
  - AWS: ECS + EKS with GPU instances
  - GCP: Cloud Run with GPU
  - Azure: Container Instances with GPU

