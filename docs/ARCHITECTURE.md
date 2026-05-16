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
│  │    Loader    │  │    Model     │  │ (OpenAI/Claude/Ollama)│  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
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
                    │   Tools       │
                    └───────────────┘
```

### Agent Descriptions

| Agent | Responsibility | Key Capabilities |
|-------|---------------|------------------|
| **Supervisor** | Central coordinator | Intent detection, task routing, result aggregation |
| **K8s** | Kubernetes management | Pod/Service/Deployment operations, scaling |
| **VectorDB** | Vector database ops | Embeddings, similarity search, collection management |
| **RAG** | Document retrieval | Knowledge base management, document indexing |
| **Pipeline** | Workflow orchestration | DAG execution, step management |
| **LLMOps** | LLM operations | Training, fine-tuning, evaluation |
| **AIOps** | Intelligent operations | Anomaly detection, root cause analysis |
| **Feature Store** | Feature engineering | Feature registration, materialization |
| **Monitoring** | Observability | Metrics, logs, alerting |
| **Model** | ML model lifecycle | Version control, deployment, inference |

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
└── "aiops", "anomaly", "incident"      → AIOps Agent
```

### Tools Structure

#### HTTP Tools (`http_tools.py`)

Generic HTTP API call functionality:

- **http_request**: Make REST API calls with configurable method, headers, and body

#### System Tools (`system_tools.py`)

Local system command execution:

- **execute_command**: Run shell commands (kubectl, docker, git, etc.)

#### Specialized Tools

| Tool | Purpose |
|------|---------|
| `k8s_tools.py` | Kubernetes API operations |
| `vector_tools.py` | Vector database operations |
| `monitoring_tools.py` | Prometheus/Grafana queries |
| `model_tools.py` | MLflow integration |
| `llmops_tools.py` | Experiment tracking |
| `aiops_tools.py` | Log analysis, anomaly detection |
| `rag_tools.py` | Document operations |
| `pipeline_tools.py` | Workflow management |
| `feature_store_tools.py` | Feast integration |

## Component Details

### 1. Web Frontend (`apps/web`)

A single-page application built with React 18 and Vite.

**Responsibilities:**
- User interface for agent interactions
- Agent panel management (K8s, Monitoring, VectorDB, etc.)
- Real-time chat with streaming responses

**Tech Stack:**
- React 18
- Vite (bundler)
- TypeScript
- Emotion CSS-in-JS

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
- 10 specialized agents
- HTTP and system command tools
- Streaming responses via SSE
- Agent-to-agent delegation

### 4. Vision Service (`services/vision-service`)

Computer vision capabilities.

**Responsibilities:**
- Image processing and validation
- Model inference (YOLO, BLIP, PaddleOCR)
- REST API endpoints

### 5. RAG Service (`services/rag`)

Retrieval-Augmented Generation service.

**Responsibilities:**
- Document ingestion and processing
- Semantic vector search with Qdrant
- LLM-powered question answering
- Conversation history management

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
│   │   │   │   └── panels/     # K8sPanel, VectorDBPanel, etc.
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
│   │   ├── agents/             # 10 specialized agents
│   │   │   ├── supervisor.py   # Central coordinator
│   │   │   ├── k8s_agent.py    # Kubernetes management
│   │   │   ├── vector_db_agent.py  # Vector operations
│   │   │   └── ...
│   │   ├── core/               # Base classes, prompts, schemas
│   │   ├── tools/              # All tool implementations
│   │   │   ├── http_tools.py   # HTTP API calls
│   │   │   ├── system_tools.py # Shell commands
│   │   │   └── ...
│   │   ├── graphs/             # LangGraph workflows
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
│       │   └── services/
│       │       ├── ingestion.py    # Document ingestion
│       │       └── rag_chain.py    # RAG chain
│       └── ...
└── docs/                       # Documentation
```

## API Reference

### AI Agents Service (Port 8003)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `GET` | `/agents` | List all available agents |
| `POST` | `/api/agents/supervisor/invoke` | Invoke supervisor (chat) |
| `POST` | `/api/agents/{agent_name}/invoke` | Invoke specific agent |

### Vision Service (Port 8002)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/vision/detect` | Object detection |
| `POST` | `/vision/caption` | Image captioning |
| `POST` | `/vision/ocr` | Text extraction (OCR) |

### RAG Service (Port 8001)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/documents/ingest` | Ingest documents |
| `POST` | `/chat` | Chat with RAG |

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
