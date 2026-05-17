# Service Ports Configuration

This document defines the standard port assignments for all AI Test platform services.

## Port Assignment Table


| Service        | Port | Protocol | Description                                          |
| -------------- | ---- | -------- | ---------------------------------------------------- |
| Vision Service | 8000 | HTTP     | Image recognition, OCR, captioning, video generation |
| AI Agents      | 8003 | HTTP     | Multi-agent orchestration with tool support          |
| RAG Service    | 8010 | HTTP     | Retrieval-augmented generation with Qdrant           |
| TTS Service    | 8013 | HTTP     | Text-to-Speech synthesis                             |
| Text Service   | 8006 | HTTP     | Text generation with LLM providers                   |
| Media Gen      | 8015 | HTTP     | Standalone image generation (Stable Diffusion)       |


### Supporting Services


| Service      | Port      | Description             |
| ------------ | --------- | ----------------------- |
| Qdrant       | 6333/6334 | Vector database for RAG |
| Ollama       | 11434     | Local LLM inference     |
| Redis        | 6379      | Optional cache backend  |
| Web Frontend | 5173      | Vite dev server         |


## Environment Variables

### Frontend (apps/web/.env)

```env
VITE_TEXT_SERVICE_URL=http://localhost:8006
VITE_VISION_SERVICE_URL=http://localhost:8000
VITE_SPEECH_SERVICE_URL=http://localhost:8013
VITE_RAG_SERVICE_URL=http://localhost:8010
VITE_AI_AGENTS_URL=http://localhost:8003
```

### Backend Service Defaults

Each service reads from its own `.env` file. Key port variables:

**text-service/src/core/config.py**

```python
PORT: int = 8006
```

**vision-service/src/main.py**

```python
port=8000
```

**rag/src/config.py**

```python
PORT: int = 8010
```

**tts-service/src/config.py**

```python
port: int = Field(default=8013, description="Server port")
```

**ai_agents/core/config.py**

```python
PORT: int = 8003
```

**media-gen/app.py**

```python
MEDIA_GEN_PORT = int(os.getenv("MEDIA_GEN_PORT", "8015"))
```

## API Endpoints Summary

### Text Service (8006)

- `GET /api/text/health` - Health check
- `GET /api/text/providers` - List LLM providers
- `GET /api/text/models` - List available models
- `POST /api/text/chat` - Chat completion
- `POST /api/text/chat/stream` - Streaming chat
- `POST /api/text/complete` - Text completion

### Vision Service (8000)

- `GET /health` - Health check
- `POST /vision/detect` - Object detection
- `POST /vision/caption` - Image captioning
- `POST /vision/ocr` - OCR text extraction
- `POST /vision/analyze` - Multi-task analysis
- `POST /image-gen/generate` - Text-to-image (SDXL/SD3)
- `POST /image-gen/variation` - Image variation
- `POST /image-gen/upscale` - AI upscaling
- `POST /video/generate` - Video generation

### RAG Service (8010)

- `GET /health` - Health check
- `GET /documents/` - List documents
- `POST /documents/upload` - Upload document
- `POST /documents/ingest-url` - Ingest from URL
- `DELETE /documents/{doc_id}` - Delete document
- `POST /chat/` - RAG chat query
- `POST /chat/stream` - Streaming RAG chat

### TTS Service (8013)

- `GET /tts/health` - Health check
- `GET /tts/voices` - List voices
- `GET /tts/providers` - List TTS providers
- `POST /tts/synthesize` - Synthesize speech
- `POST /tts/stream` - Stream speech

### AI Agents (8003)

- `GET /health` - Health check
- `GET /agents` - List available agents
- `POST /api/agents/supervisor/invoke` - Invoke supervisor agent
- `POST /api/agents/rag_agent/invoke` - Invoke RAG agent

## Docker Compose Ports

When running with Docker, ports are mapped as follows:

```yaml
# vision-service
8000:8000

# ai-agents
8003:8003

# rag
8010:8010
6333:6333  # Qdrant
6334:6334  # Qdrant GRPC

# tts-service
8013:8013

# text-service
8006:8006

# media-gen
8015:8015
```

## Common Issues

1. **Port Already in Use**
  ```bash
   # Find process using port
   lsof -i :8000

   # Kill if needed
   kill -9 <PID>
  ```
2. **CORS Errors**
  All services have CORS configured to allow `*`. If issues persist, check that the service is running.
3. **Wrong Port in Frontend**
  Copy `.env.example` to `.env` in `apps/web/` and verify all `VITE_`* URLs match the running services.
4. **Service-to-Service Communication**
  When services call each other, they use hardcoded localhost URLs. Ensure all services are running before testing inter-service features.

