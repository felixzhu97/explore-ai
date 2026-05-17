# API Reference

REST API documentation for the AI-Test Platform services.

---

## Table of Contents

- [AI Agents Service (Port 8003)](#ai-agents-service-port-8003)
- [Vision Service (Port 8002)](#vision-service-port-8002)
- [Text Service (Port 8004)](#text-service-port-8004)
- [TTS Service (Port 8004)](#tts-service-port-8004)
- [RAG Service (Port 8001)](#rag-service-port-8001)

---

## AI Agents Service (Port 8003)

Base URL: `http://localhost:8003`

### Endpoints Overview


| Method | Endpoint                          | Description                                  |
| ------ | --------------------------------- | -------------------------------------------- |
| `GET`  | `/health`                         | Health check                                 |
| `GET`  | `/agents`                         | List all available agents                    |
| `POST` | `/api/agents/supervisor/invoke`   | Invoke supervisor agent (main chat endpoint) |
| `POST` | `/api/agents/{agent_name}/invoke` | Invoke a specific agent directly             |


---

### Health Check

#### `GET /health`

Check if the AI Agents service is running.

**Response:**

```json
{
  "status": "ok",
  "service": "ai_agents",
  "agents_initialized": true,
  "available_agents": ["vector", "kubernetes", "monitoring", "model", "rag", "llmops", "feature_store", "pipeline", "aiops"]
}
```

**Response Fields:**


| Field                | Type    | Description                     |
| -------------------- | ------- | ------------------------------- |
| `status`             | string  | Always `"ok"` if healthy        |
| `service`            | string  | Service identifier              |
| `agents_initialized` | boolean | Whether agents are loaded       |
| `available_agents`   | array   | List of initialized agent names |


---

### List Agents

#### `GET /agents`

Get information about all available agents.

**Response:**

```json
{
  "agents": [
    {
      "name": "vector",
      "description": "Handles vector database operations",
      "status": "online"
    },
    {
      "name": "kubernetes",
      "description": "Kubernetes cluster management",
      "status": "online"
    },
    {
      "name": "monitoring",
      "description": "Handles observability, metrics, logs, and alerting",
      "status": "online"
    },
    {
      "name": "model",
      "description": "Manages ML model lifecycle, deployment, and versioning",
      "status": "online"
    },
    {
      "name": "rag",
      "description": "Document retrieval and knowledge base management",
      "status": "online"
    },
    {
      "name": "llmops",
      "description": "ML model lifecycle management",
      "status": "online"
    },
    {
      "name": "feature_store",
      "description": "Feature engineering and management",
      "status": "online"
    },
    {
      "name": "pipeline",
      "description": "ML/DevOps workflow orchestration",
      "status": "online"
    },
    {
      "name": "aiops",
      "description": "Intelligent operations and anomaly detection",
      "status": "online"
    }
  ]
}
```

---

### Chat with Supervisor (Main Endpoint)

#### `POST /api/agents/supervisor/invoke`

Invoke the Supervisor agent to handle user queries. This is the main chat endpoint that routes to appropriate specialized agents.

**Request:**

```json
{
  "messages": [
    {
      "role": "user",
      "content": "Show me the status of my kubernetes pods"
    }
  ]
}
```

**Request Fields:**


| Field                | Type   | Required | Description                           |
| -------------------- | ------ | -------- | ------------------------------------- |
| `messages`           | array  | Yes      | List of conversation messages         |
| `messages[].role`    | string | Yes      | Message role (`user`, `assistant`)    |
| `messages[].content` | string | Yes      | Message content                       |
| `agent_name`         | string | No       | Specific agent to route to (optional) |


**Response:**

Server-Sent Events (SSE) streaming response:

```
event: message
data: Starting analysis...

event: message
data: Routing to: kubernetes
...

event: message
data: Your kubernetes cluster has 3 pods running:
- nginx-deployment-abc123 (Running)
- redis-xyz789 (Running)
- api-gateway-def456 (Running)

event: tool_output
data: Agent 'kubernetes' completed

data: [DONE]
```

**Event Types:**


| Event         | Description                  |
| ------------- | ---------------------------- |
| `message`     | Text response from the agent |
| `tool_call`   | Tool invocation details      |
| `tool_output` | Result from tool execution   |
| `error`       | Error message                |
| `[DONE]`      | End of stream marker         |


---

### Invoke Specific Agent

#### `POST /api/agents/{agent_name}/invoke`

Invoke a specific agent directly without going through the Supervisor.

**Path Parameters:**


| Parameter    | Type   | Description                                             |
| ------------ | ------ | ------------------------------------------------------- |
| `agent_name` | string | Agent name (e.g., `vector`, `kubernetes`, `monitoring`) |


**Request:**

```json
{
  "messages": [
    {
      "role": "user",
      "content": "Search for similar documents about machine learning"
    }
  ]
}
```

**Response:**

Server-Sent Events (SSE) streaming response (same format as supervisor endpoint).

**Available Agents:**


| Agent Name      | Description                             |
| --------------- | --------------------------------------- |
| `vector`        | Vector database operations              |
| `kubernetes`    | Kubernetes cluster management           |
| `monitoring`    | Metrics, logs, and alerting             |
| `model`         | ML model lifecycle management           |
| `rag`           | Document retrieval and knowledge base   |
| `llmops`        | LLM operations and experiments          |
| `feature_store` | Feature engineering                     |
| `pipeline`      | Workflow orchestration                  |
| `aiops`         | Anomaly detection and incident response |


---

### Example: Using cURL

```bash
# Health check
curl http://localhost:8003/health

# List agents
curl http://localhost:8003/agents

# Chat with Supervisor
curl -X POST http://localhost:8003/api/agents/supervisor/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "What pods are running in my cluster?"}
    ]
  }'

# Query specific agent
curl -X POST http://localhost:8003/api/agents/vector/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "Show me recent embeddings"}
    ]
  }'
```

---

## Vision Service (Port 8002)

Base URL: `http://localhost:8002`

### Endpoints Overview


| Method | Endpoint              | Description                  |
| ------ | --------------------- | ---------------------------- |
| `GET`  | `/health`             | Health check                  |
| `GET`  | `/`                   | Service info                  |
| `POST` | `/vision/detect`      | Object detection (YOLO)       |
| `POST` | `/vision/caption`     | Image captioning (BLIP)       |
| `POST` | `/vision/ocr`         | Text extraction (PaddleOCR)   |
| `POST` | `/vision/analyze`     | Combined analysis             |
| `POST` | `/image-gen/generate` | Text-to-image (Stable Diffusion) |
| `POST` | `/image-gen/variation` | Image variation             |
| `POST` | `/image-gen/upscale`  | Image upscaling               |
| `POST` | `/video/generate`     | Text/image to video          |


---

### Health Check

#### `GET /health`

Check if the service is running.

**Response:**

```json
{
  "status": "ok"
}
```

---

### Service Info

#### `GET /`

Get service information and available endpoints.

**Response:**

```json
{
  "name": "AI Vision Service",
  "version": "0.1.0",
  "endpoints": {
    "health": "/health",
    "detect": "/vision/detect",
    "caption": "/vision/caption",
    "ocr": "/vision/ocr",
    "analyze": "/vision/analyze"
  }
}
```

---

### Object Detection

#### `POST /vision/detect`

Detect objects in an image using YOLO.

**Request:**

- **Content-Type:** `multipart/form-data`
- **Body:**
  - `file` (required): Image file (JPEG, PNG, etc.)
  - `conf` (optional): Confidence threshold (default: 0.25)

**Response:**

```json
{
  "task": "detect_objects",
  "model": "yolo11n.pt",
  "detections": [
    {
      "class_name": "person",
      "confidence": 0.92,
      "bbox": [120, 50, 400, 600]
    },
    {
      "class_name": "car",
      "confidence": 0.85,
      "bbox": [500, 300, 700, 500]
    }
  ],
  "image_width": 800,
  "image_height": 600,
  "processing_time_ms": 45.2
}
```

**Response Fields:**


| Field                     | Type   | Description                     |
| ------------------------- | ------ | ------------------------------- |
| `task`                    | string | Always `"detect_objects"`       |
| `model`                   | string | YOLO model name                 |
| `detections`              | array  | List of detected objects        |
| `detections[].class_name` | string | Object class label              |
| `detections[].confidence` | float  | Confidence score (0-1)          |
| `detections[].bbox`       | array  | Bounding box [x1, y1, x2, y2]   |
| `image_width`             | int    | Image width in pixels           |
| `image_height`            | int    | Image height in pixels          |
| `processing_time_ms`      | float  | Processing time in milliseconds |


**Example:**

```bash
curl -X POST http://localhost:8002/vision/detect \
  -F "file=@image.jpg" \
  -F "conf=0.5"
```

---

### Image Captioning

#### `POST /vision/caption`

Generate a natural language description of an image using BLIP.

**Request:**

- **Content-Type:** `multipart/form-data`
- **Body:**
  - `file` (required): Image file (JPEG, PNG, etc.)

**Response:**

```json
{
  "task": "caption_image",
  "model": "Salesforce/blip-image-captioning-large",
  "caption": "A person hiking in the mountains during sunset",
  "processing_time_ms": 230.5
}
```

**Response Fields:**


| Field                | Type   | Description                     |
| -------------------- | ------ | ------------------------------- |
| `task`               | string | Always `"caption_image"`        |
| `model`              | string | BLIP model name                 |
| `caption`            | string | Generated caption               |
| `processing_time_ms` | float  | Processing time in milliseconds |


**Example:**

```bash
curl -X POST http://localhost:8002/vision/caption \
  -F "file=@image.jpg"
```

---

### OCR (Text Extraction)

#### `POST /vision/ocr`

Extract text from an image using PaddleOCR.

**Request:**

- **Content-Type:** `multipart/form-data`
- **Body:**
  - `file` (required): Image file (JPEG, PNG, etc.)

**Response:**

```json
{
  "task": "extract_text",
  "model": "PaddleOCR",
  "results": [
    {
      "text": "Hello World",
      "confidence": 0.95,
      "bbox": [[10, 10], [100, 10], [100, 30], [10, 30]]
    },
    {
      "text": "Document Text",
      "confidence": 0.92,
      "bbox": [[10, 50], [200, 50], [200, 70], [10, 70]]
    }
  ],
  "full_text": "Hello World\nDocument Text",
  "processing_time_ms": 85.3
}
```

**Response Fields:**


| Field                  | Type   | Description                                       |
| ---------------------- | ------ | ------------------------------------------------- |
| `task`                 | string | Always `"extract_text"`                           |
| `model`                | string | Always `"PaddleOCR"`                              |
| `results`              | array  | List of detected text blocks                      |
| `results[].text`       | string | Extracted text                                    |
| `results[].confidence` | float  | Confidence score (0-1)                            |
| `results[].bbox`       | array  | Bounding box [[x1,y1], [x2,y2], [x3,y3], [x4,y4]] |
| `full_text`            | string | All extracted text joined with newlines           |
| `processing_time_ms`   | float  | Processing time in milliseconds                   |


**Example:**

```bash
curl -X POST http://localhost:8002/vision/ocr \
  -F "file=@document.jpg"
```

---

### Combined Analysis

#### `POST /vision/analyze`

Run multiple AI tasks on a single image.

**Request:**

- **Content-Type:** `multipart/form-data`
- **Body:**
  - `file` (required): Image file (JPEG, PNG, etc.)
  - `task` (required): Task type (query parameter)

**Task Types:**


| Task             | Description      | Response Keys                  |
| ---------------- | ---------------- | ------------------------------ |
| `caption_image`  | Generate caption | `caption`                      |
| `detect_objects` | Detect objects   | `detections`                   |
| `extract_text`   | Extract text     | `results`                      |
| `analyze_image`  | Run all tasks    | `caption`, `detections`, `ocr` |


**Example - Full Analysis:**

```bash
curl -X POST "http://localhost:8002/vision/analyze?task=analyze_image" \
  -F "file=@photo.jpg"
```

**Response:**

```json
{
  "caption": {
    "model": "Salesforce/blip-image-captioning-large",
    "caption": "A person reading a book",
    "processing_time_ms": 245.3
  },
  "detections": {
    "model": "yolo11n.pt",
    "detections": [...],
    "processing_time_ms": 48.1
  },
  "ocr": {
    "model": "PaddleOCR",
    "results": [...],
    "full_text": "Book title here",
    "processing_time_ms": 85.3
  }
}
```

---

## Text Service (Port 8004)

Base URL: `http://localhost:8004`

Text-to-Text LLM service with multi-provider support (OpenAI, Anthropic, Ollama).

### Endpoints Overview


| Method | Endpoint               | Description              |
| ------ | --------------------- | ------------------------ |
| `GET`  | `/api/text/health`     | Health check             |
| `GET`  | `/api/text/providers`   | List LLM providers       |
| `GET`  | `/api/text/models`     | List available models     |
| `POST` | `/api/text/complete`    | Text completion          |
| `POST` | `/api/text/complete/stream` | Stream completion    |
| `POST` | `/api/text/chat`       | Chat completion          |
| `POST` | `/api/text/chat/stream` | Stream chat completion  |
| `GET`  | `/api/text/session/{session_id}` | Get session history |
| `DELETE` | `/api/text/session/{session_id}` | Clear session |

---

### Health Check

#### `GET /api/text/health`

**Response:**

```json
{
  "status": "ok",
  "provider": "openai",
  "model": "gpt-4o-mini",
  "version": "0.1.0"
}
```

---

### List Providers

#### `GET /api/text/providers`

**Response:**

```json
[
  {
    "name": "openai",
    "display_name": "OpenAI",
    "models": ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo"],
    "status": "available"
  },
  {
    "name": "anthropic",
    "display_name": "Anthropic Claude",
    "models": ["claude-sonnet-4-20250514", "claude-opus-4-20250514"],
    "status": "available"
  },
  {
    "name": "ollama",
    "display_name": "Ollama (Local)",
    "models": ["qwen2.5:7b", "llama3.2"],
    "status": "available"
  }
]
```

---

### Text Completion

#### `POST /api/text/complete`

**Request:**

```json
{
  "prompt": "Explain quantum computing in simple terms:",
  "system_prompt": "You are a helpful assistant.",
  "provider": "openai",
  "model": "gpt-4o-mini",
  "temperature": 0.7,
  "max_tokens": 500
}
```

**Response:**

```json
{
  "text": "Quantum computing is a type of computation...",
  "provider": "openai",
  "model": "gpt-4o-mini",
  "usage": {"latency_ms": 1234},
  "finish_reason": "stop"
}
```

---

### Chat Completion

#### `POST /api/text/chat`

**Request:**

```json
{
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello, how are you?"}
  ],
  "session_id": "optional-session-id",
  "provider": "openai",
  "temperature": 0.7
}
```

**Response:**

```json
{
  "text": "I'm doing well, thank you for asking! How can I help you today?",
  "provider": "openai",
  "model": "gpt-4o-mini",
  "session_id": "session-uuid",
  "usage": {"latency_ms": 567, "history_length": 3},
  "finish_reason": "stop"
}
```

---

## TTS Service (Port 8004)

Base URL: `http://localhost:8004`

Text-to-Speech service with multiple provider support (Azure, Google, ElevenLabs, Coqui).

### Endpoints Overview


| Method | Endpoint           | Description              |
| ------ | ------------------ | ------------------------ |
| `GET`  | `/tts/health`       | Health check             |
| `GET`  | `/tts/voices`       | List available voices    |
| `GET`  | `/tts/providers`     | List TTS providers       |
| `POST` | `/tts/synthesize`    | Synthesize speech        |
| `POST` | `/tts/stream`       | Stream speech            |

---

### Health Check

#### `GET /tts/health`

**Response:**

```json
{
  "status": "ok",
  "tts_engine": "edge",
  "ai_agents_connected": false,
  "version": "0.1.0"
}
```

---

### List Voices

#### `GET /tts/voices`

**Response:**

```json
{
  "voices": [
    {"voice_id": "en-US-JennyNeural", "name": "Jenny", "language": "en-US"},
    {"voice_id": "en-GB-SoniaNeural", "name": "Sonia", "language": "en-GB"},
    {"voice_id": "zh-CN-XiaoxiaoNeural", "name": "Xiaoxiao", "language": "zh-CN"}
  ]
}
```

---

### Synthesize Speech

#### `POST /tts/synthesize`

**Request:**

```json
{
  "text": "Hello, world!",
  "voice": "en-US-JennyNeural",
  "language": "en-US",
  "speed": 1.0,
  "pitch": 0,
  "output_format": "mp3"
}
```

**Response:** Audio binary (mp3/wav/ogg)

---

## RAG Service (Port 8001)

Base URL: `http://localhost:8001`

Production RAG service with Qdrant vector store for document retrieval and knowledge base management.

### Endpoints Overview

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| `GET` | `/health` | Health check |
| `GET` | `/` | Service info |
| `POST` | `/documents/upload` | Upload and ingest a document |
| `POST` | `/documents/ingest-url` | Ingest document from URL |
| `GET` | `/documents/` | List all documents |
| `GET` | `/documents/{doc_id}/stats` | Get document statistics |
| `DELETE` | `/documents/{doc_id}` | Delete a document |
| `POST` | `/chat/` | Chat with RAG (non-streaming) |
| `POST` | `/chat/stream` | Chat with RAG (streaming) |
| `GET` | `/chat/history/{session_id}` | Get chat history |
| `DELETE` | `/chat/history/{session_id}` | Clear chat history |
| `POST` | `/chat/ingest-text` | Ingest raw text directly |
| `POST` | `/reload` | Reload configuration |
| `GET` | `/cache/stats` | Get cache statistics |
| `POST` | `/cache/clear` | Clear all caches |

---

### Health Check

#### `GET /health`

Check service health and connectivity status.

**Response:**

```json
{
  "status": "ok",
  "qdrant_connected": true,
  "embedding_model": "nomic-embed-text",
  "llm_provider": "ollama"
}
```

**Response Fields:**

| Field | Type | Description |
| ----- | ---- | ----------- |
| `status` | string | `"ok"` if healthy, `"degraded"` if Qdrant unavailable |
| `qdrant_connected` | boolean | Whether Qdrant vector store is connected |
| `embedding_model` | string | Name of the embedding model |
| `llm_provider` | string | LLM provider (`ollama`, `openai`, etc.) |

---

### Service Info

#### `GET /`

Get service information and available endpoints.

**Response:**

```json
{
  "name": "RAG Service",
  "version": "0.2.0",
  "description": "Production RAG service with Qdrant vector store",
  "endpoints": {
    "health": "/health",
    "documents": {
      "upload": "POST /documents/upload",
      "ingest_url": "POST /documents/ingest-url",
      "list": "GET /documents/",
      "list_from_db": "GET /documents/database",
      "stats": "GET /documents/{doc_id}/stats",
      "delete": "DELETE /documents/{doc_id}"
    },
    "chat": {
      "query": "POST /chat/",
      "stream": "POST /chat/stream",
      "history": "GET /chat/history/{session_id}",
      "ingest_text": "POST /chat/ingest-text"
    }
  },
  "config": {
    "llm_provider": "ollama",
    "llm_model": "qwen2.5:7b",
    "embedding_model": "nomic-embed-text"
  }
}
```

---

### Document Management

#### Upload Document

##### `POST /documents/upload`

Upload and ingest a document file (PDF, Markdown, Text).

**Request:**

- **Content-Type:** `multipart/form-data`
- **Body:**
  - `file` (required): Document file (PDF, MD, TXT)
  - `title` (optional): Document title (defaults to filename)
  - `collection` (optional): Collection name

**Supported File Types:**

| Extension | Source Type |
| --------- | ----------- |
| `.md`, `.markdown` | Markdown |
| `.pdf` | PDF |
| `.txt`, `.text` | Text |

**Response:**

```json
{
  "doc_id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "document.pdf",
  "chunks": 15,
  "status": "success"
}
```

**Response Fields:**

| Field | Type | Description |
| ----- | ---- | ----------- |
| `doc_id` | string | Unique document identifier |
| `filename` | string | Original filename |
| `chunks` | int | Number of text chunks created |
| `status` | string | `"success"` or `"failed"` |

**Example:**

```bash
curl -X POST http://localhost:8001/documents/upload \
  -F "file=@document.pdf" \
  -F "title=My Document"
```

---

#### Ingest URL

##### `POST /documents/ingest-url`

Ingest a document from a web URL.

**Query Parameters:**

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| `url` | string | Yes | URL to ingest |
| `title` | string | No | Document title |

**Response:**

```json
{
  "doc_id": "550e8400-e29b-41d4-a716-446655440001",
  "filename": "https://example.com/article",
  "chunks": 8,
  "status": "success"
}
```

**Example:**

```bash
curl -X POST "http://localhost:8001/documents/ingest-url?url=https://example.com/doc&title=Article"
```

---

#### List Documents

##### `GET /documents/`

List all uploaded documents.

**Response:**

```json
{
  "documents": [
    {
      "doc_id": "550e8400-e29b-41d4-a716-446655440000",
      "filename": "document.pdf",
      "total_chunks": 15,
      "source": "pdf",
      "uploaded_at": "2024-01-15T10:30:00Z"
    }
  ],
  "total": 1
}
```

**Response Fields:**

| Field | Type | Description |
| ----- | ---- | ----------- |
| `documents` | array | List of document records |
| `documents[].doc_id` | string | Document identifier |
| `documents[].filename` | string | Document filename |
| `documents[].total_chunks` | int | Number of chunks |
| `documents[].source` | string | Source type (`pdf`, `markdown`, `web`, `text`) |
| `documents[].uploaded_at` | string | Upload timestamp |
| `total` | int | Total number of documents |

---

#### Document Statistics

##### `GET /documents/{doc_id}/stats`

Get detailed statistics for a specific document.

**Path Parameters:**

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `doc_id` | string | Document identifier |

**Response:**

```json
{
  "doc_id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "document.pdf",
  "title": "My Document",
  "total_chunks": 15,
  "source": "pdf",
  "status": "completed",
  "created_at": "2024-01-15T10:30:00Z",
  "indexed_at": "2024-01-15T10:30:05Z",
  "file_size": 245678,
  "vector_stats": {
    "total_vectors": 15,
    "collection_name": "rag_collection"
  }
}
```

---

#### Delete Document

##### `DELETE /documents/{doc_id}`

Delete a document and all its associated vectors.

**Path Parameters:**

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `doc_id` | string | Document identifier |

**Response:**

```json
{
  "status": "success",
  "message": "Document 550e8400-e29b-41d4-a716-446655440000 deleted"
}
```

**Example:**

```bash
curl -X DELETE http://localhost:8001/documents/550e8400-e29b-41d4-a716-446655440000
```

---

### Chat with RAG

#### Non-Streaming Chat

##### `POST /chat/`

Query the knowledge base and get a response with sources.

**Request:**

```json
{
  "query": "What is the main topic of the documents?",
  "session_id": "optional-session-id",
  "doc_ids": ["doc-id-1", "doc-id-2"],
  "top_k": 5,
  "temperature": 0.7
}
```

**Request Fields:**

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| `query` | string | Yes | - | User question |
| `session_id` | string | No | Auto-generated | Session identifier for conversation continuity |
| `doc_ids` | array | No | All docs | Filter to specific documents |
| `top_k` | int | No | 5 | Number of context chunks to retrieve (1-20) |
| `temperature` | float | No | 0.7 | LLM temperature (0-2) |

**Response:**

```json
{
  "answer": "Based on the documents, the main topic is artificial intelligence...",
  "sources": [
    {
      "text": "Document excerpt text...",
      "score": 0.95,
      "metadata": {
        "source": "pdf",
        "filename": "document.pdf",
        "doc_id": "550e8400-e29b-41d4-a716-446655440000"
      }
    }
  ],
  "session_id": "session-uuid-here",
  "model": "qwen2.5:7b",
  "processing_time_ms": 1234.5
}
```

**Response Fields:**

| Field | Type | Description |
| ----- | ---- | ----------- |
| `answer` | string | Generated response |
| `sources` | array | Retrieved context chunks |
| `sources[].text` | string | Source text excerpt |
| `sources[].score` | float | Relevance score (0-1) |
| `sources[].metadata` | object | Source metadata |
| `session_id` | string | Session identifier |
| `model` | string | LLM model used |
| `processing_time_ms` | float | Processing time |

---

#### Streaming Chat

##### `POST /chat/stream`

Query the knowledge base with streaming response.

**Request:** Same as non-streaming chat.

**Response:** Server-Sent Events (SSE) stream.

```
data: Based
data:  on
data:  the
data:  documents
data: , the
data:  main
...

data: [DONE]

event: sources
data: [{"text": "...", "score": 0.95, "metadata": {...}}]

event: meta
data: {"processing_time_ms": 1234.5, "model": "qwen2.5:7b"}
```

**Event Types:**

| Event | Description |
| ----- | ----------- |
| `data: ...` | Response text chunks |
| `data: [DONE]` | End of text stream |
| `event: sources` | Retrieved source documents |
| `event: meta` | Response metadata |

**Response Headers:**

| Header | Description |
| ------ | ----------- |
| `X-Session-Id` | Session identifier |

**Example:**

```bash
curl -X POST http://localhost:8001/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"query": "What is machine learning?"}' \
  -N
```

---

#### Get Chat History

##### `GET /chat/history/{session_id}`

Retrieve chat history for a session.

**Path Parameters:**

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `session_id` | string | Session identifier |

**Response:**

```json
{
  "session_id": "session-uuid-here",
  "messages": [
    {
      "role": "user",
      "content": "What is AI?",
      "timestamp": 1705312200.123,
      "sources": []
    },
    {
      "role": "assistant",
      "content": "AI stands for Artificial Intelligence...",
      "timestamp": 1705312205.456,
      "sources": [
        {
          "text": "Source excerpt...",
          "score": 0.92,
          "metadata": {}
        }
      ]
    }
  ],
  "total": 2
}
```

---

#### Clear Chat History

##### `DELETE /chat/history/{session_id}`

Delete all messages for a session.

**Path Parameters:**

| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `session_id` | string | Session identifier |

**Response:**

```json
{
  "status": "success",
  "message": "History for session session-uuid-here cleared"
}
```

---

#### Ingest Text Directly

##### `POST /chat/ingest-text`

Ingest raw text as a document without uploading a file.

**Request Parameters:**

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| `text` | string | Yes | Text content to ingest |
| `title` | string | No | Document title (default: "Text Document") |
| `doc_id` | string | No | Custom document ID |

**Response:**

```json
{
  "doc_id": "550e8400-e29b-41d4-a716-446655440002",
  "title": "My Notes",
  "chunks": 3,
  "status": "success"
}
```

**Example:**

```bash
curl -X POST "http://localhost:8001/chat/ingest-text?title=MyNotes" \
  -H "Content-Type: application/json" \
  -d '{"text": "This is the text content to ingest into the knowledge base."}'
```

---

### Configuration Endpoints

#### Reload Configuration

##### `POST /reload`

Reload configuration from environment variables.

**Response:**

```json
{
  "status": "success",
  "config": {
    "llm_provider": "ollama",
    "llm_model": "qwen2.5:7b",
    "embedding_model": "nomic-embed-text"
  }
}
```

---

#### Cache Statistics

##### `GET /cache/stats`

Get statistics for all caches and stores.

**Response:**

```json
{
  "cache": {
    "hits": 150,
    "misses": 50,
    "hit_rate": 0.75
  },
  "documents": {
    "total": 10,
    "by_source": {
      "pdf": 5,
      "markdown": 3,
      "web": 2
    }
  },
  "sessions": {
    "total": 25,
    "total_messages": 150
  }
}
```

---

#### Clear Cache

##### `POST /cache/clear`

Clear all caches.

**Response:**

```json
{
  "status": "success",
  "message": "Cache cleared"
}
```

---

### Python Examples

```python
import requests

# Upload a document
with open("document.pdf", "rb") as f:
    response = requests.post(
        "http://localhost:8001/documents/upload",
        files={"file": f},
        data={"title": "My Document"}
    )
print(response.json())

# List documents
response = requests.get("http://localhost:8001/documents/")
print(response.json())

# Chat query
response = requests.post(
    "http://localhost:8001/chat/",
    json={
        "query": "What is the main topic?",
        "top_k": 5
    }
)
print(response.json())

# Streaming chat
with requests.post(
    "http://localhost:8001/chat/stream",
    json={"query": "Explain this topic"},
    stream=True
) as r:
    for line in r.iter_lines():
        if line:
            print(line.decode())

# Get chat history
response = requests.get("http://localhost:8001/chat/history/session-123")
print(response.json())

# Ingest text directly
response = requests.post(
    "http://localhost:8001/chat/ingest-text",
    params={"title": "My Notes"},
    json={"text": "Content to ingest..."}
)
print(response.json())
```

---

### JavaScript Examples

```javascript
// Upload document
const formData = new FormData();
formData.append("file", fileStream);
formData.append("title", "Document Title");

const uploadRes = await fetch("http://localhost:8001/documents/upload", {
  method: "POST",
  body: formData,
});
const uploadData = await uploadRes.json();

// Chat query
const chatRes = await fetch("http://localhost:8001/chat/", {
  method: "POST",
  headers: {"Content-Type": "application/json"},
  body: JSON.stringify({
    query: "What is the content about?",
    top_k: 5
  }),
});
const chatData = await chatRes.json();

// Streaming chat
const streamRes = await fetch("http://localhost:8001/chat/stream", {
  method: "POST",
  headers: {"Content-Type": "application/json"},
  body: JSON.stringify({ query: "Explain this" }),
});

const reader = streamRes.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  console.log(decoder.decode(value));
}

// Get history
const historyRes = await fetch("http://localhost:8001/chat/history/session-123");
const history = await historyRes.json();
```

---

## Error Responses

### 400 Bad Request

Invalid image or file too large.

```json
{
  "detail": "Image too large (max 10MB)"
}
```

```json
{
  "detail": "Invalid image file"
}
```

### 422 Unprocessable Entity

Missing required fields.

```json
{
  "detail": [
    {
      "loc": ["body", "file"],
      "msg": "Field required",
      "type": "missing"
    }
  ]
}
```

### 500 Internal Server Error

Model or processing error.

```json
{
  "detail": "Model loading failed"
}
```

### 503 Service Unavailable

AI Agents service not initialized.

```json
{
  "detail": "Agents not initialized"
}
```

---

## Rate Limits

### Vision Service

- **Concurrent Requests:** 4 (configurable via `MAX_CONCURRENT_REQUESTS`)
- **Max File Size:** 10MB (configurable via `MAX_IMAGE_SIZE`)

---

## Environment Variables

### AI Agents Service


| Variable          | Default                  | Description       |
| ----------------- | ------------------------ | ----------------- |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL`    | `qwen2.5:7b`             | Ollama model name |


### Vision Service


| Variable                  | Default                                  | Description                          |
| ------------------------- | ---------------------------------------- | ------------------------------------ |
| `DEVICE`                  | `cuda`                                   | Computation device (`cuda` or `cpu`) |
| `YOLO_MODEL`              | `yolo11n.pt`                             | YOLO model path                      |
| `BLIP_MODEL`              | `Salesforce/blip-image-captioning-large` | BLIP model name                      |
| `OCR_LANG`                | `ch`                                     | PaddleOCR language                   |
| `MAX_IMAGE_SIZE`          | `10485760`                               | Max file size in bytes (10MB)        |
| `MODEL_CACHE_DIR`         | `./models`                               | Model cache directory                |
| `MAX_CONCURRENT_REQUESTS` | `4`                                      | Max concurrent requests              |


---

## Client Examples

### Python with `requests`

```python
import requests

# AI Agents - Chat with Supervisor
response = requests.post(
    "http://localhost:8003/api/agents/supervisor/invoke",
    json={"messages": [{"role": "user", "content": "Hello"}]}
)
print(response.text)

# Vision - Object Detection
with open("image.jpg", "rb") as f:
    response = requests.post(
        "http://localhost:8002/vision/detect",
        files={"file": f}
    )
print(response.json())
```

### JavaScript with `fetch`

```javascript
// AI Agents - Chat
const response = await fetch("http://localhost:8003/api/agents/supervisor/invoke", {
  method: "POST",
  headers: {"Content-Type": "application/json"},
  body: JSON.stringify({
    messages: [{role: "user", content: "Show me kubernetes pods"}]
  })
});

// Vision - Caption
const formData = new FormData();
formData.append("file", imageFile);

const visionResponse = await fetch("http://localhost:8002/vision/caption", {
  method: "POST",
  body: formData,
});

const data = await visionResponse.json();
console.log(data.caption);
```

### cURL

```bash
# AI Agents
curl http://localhost:8003/health
curl http://localhost:8003/agents

# Vision
curl -X POST http://localhost:8002/vision/caption \
  -F "file=@photo.jpg"

curl -X POST http://localhost:8002/vision/detect \
  -F "file=@photo.jpg" \
  -F "conf=0.5"

# RAG - Upload document
curl -X POST http://localhost:8001/documents/upload \
  -F "file=@document.pdf" \
  -F "title=My Document"

# RAG - Ingest from URL
curl -X POST "http://localhost:8001/documents/ingest-url?url=https://example.com/doc"

# RAG - List documents
curl http://localhost:8001/documents/

# RAG - Delete document
curl -X DELETE http://localhost:8001/documents/{doc_id}

# RAG - Chat query
curl -X POST http://localhost:8001/chat/ \
  -H "Content-Type: application/json" \
  -d '{"query": "What is machine learning?"}'

# RAG - Streaming chat
curl -X POST http://localhost:8001/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"query": "Explain this topic"}'

# RAG - Get chat history
curl http://localhost:8001/chat/history/{session_id}

# RAG - Ingest text directly
curl -X POST "http://localhost:8001/chat/ingest-text?title=MyNotes" \
  -H "Content-Type: application/json" \
  -d '{"text": "Content to ingest"}'

# RAG - Clear chat history
curl -X DELETE http://localhost:8001/chat/history/{session_id}

# RAG - Cache stats
curl http://localhost:8001/cache/stats

# RAG - Reload config
curl -X POST http://localhost:8001/reload
```
```

