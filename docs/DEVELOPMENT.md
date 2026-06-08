# Development Guide

This guide covers local development setup, testing, and contribution workflow.

## Project Structure

```
ai-test/
├── apps/
│   ├── web/           # Angular frontend (TypeScript)
│   └── server/        # Express.js backend (TypeScript)
├── packages/
│   ├── config/        # Shared TypeScript config
│   ├── utils/         # Shared utilities
│   └── ai-providers/  # AI provider utilities
├── services/
│   ├── vision-service/      # Vision AI + Image Gen service (FastAPI, Python)
│   │   ├── src/
│   │   │   ├── api/         # API endpoints (vision, video, image_gen)
│   │   │   ├── providers/   # Video providers (sora, pika, runway, kling)
│   │   │   └── core/        # Core business logic
│   │   └── tests/
│   ├── ai_agents/           # Multi-agent orchestration (FastAPI, Python)
│   │   ├── agents/         # Agent implementations (10 agents)
│   │   ├── core/           # Core configurations
│   │   ├── graphs/         # LangGraph definitions
│   │   ├── tools/          # Agent tools (50+ tools)
│   │   └── main.py         # Entry point
│   ├── rag/                # RAG service (FastAPI, Python)
│   │   ├── src/
│   │   │   ├── api/        # API endpoints
│   │   │   ├── core/       # Core logic
│   │   │   ├── services/   # RAG chain
│   │   │   └── persistence/# Data persistence
│   │   └── tests/
│   ├── text-service/         # Text-to-Text LLM service (FastAPI, Python)
│   │   ├── src/
│   │   │   ├── api/        # API endpoints
│   │   │   └── core/       # LLM gateway
│   │   └── tests/
│   ├── tts-service/          # Text-to-Speech service (FastAPI, Python)
│   │   ├── src/
│   │   │   ├── providers/  # TTS providers (azure, google, elevenlabs, coqui)
│   │   │   ├── routers/    # API endpoints
│   │   │   └── utils/     # Audio utilities
│   │   └── tests/
│   └── media-gen/            # Local Stable Diffusion service (FastAPI, Python)
│       └── app.py           # App entry
├── docs/             # Documentation
└── scripts/         # Build/deployment scripts
```

## Prerequisites

- Node.js >= 20
- pnpm >= 9
- Python >= 3.9
- (Optional) CUDA-capable GPU for AI service

## Setup

### 1. Clone and Install

```bash
# Clone repository
git clone <repo-url>
cd ai-test

# Install all dependencies (root + workspaces)
pnpm install
```

### 2. Python Environment

The project has six Python-based microservices. Each service has its own virtual environment.

#### Vision Service

```bash
cd services/vision-service

# Create virtual environment
python3 -m venv .venv

# Activate
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate   # Windows

# Install dependencies
pip install -r requirements.txt

# Install dev dependencies
pip install pytest pytest-asyncio httpx
```

#### AI Agents Service

```bash
cd services/ai_agents

# Create virtual environment
python3 -m venv .venv

# Activate
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate   # Windows

# Install dependencies
pip install -r requirements.txt

# Install dev dependencies
pip install pytest pytest-asyncio httpx
```

#### RAG Service

```bash
cd services/rag

# Create virtual environment
python3 -m venv .venv

# Activate
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate   # Windows

# Install dependencies
pip install -r requirements.txt

# Install dev dependencies
pip install pytest pytest-asyncio httpx ruff
```

### 3. Environment Configuration

#### Vision Service

```bash
cd services/vision-service
cp .env.example .env
```

Edit `.env` as needed:

```env
# Device settings
DEVICE=cuda                    # 'cuda' for GPU, 'cpu' for CPU

# Model settings
YOLO_MODEL=yolo11n.pt
BLIP_MODEL=Salesforce/blip-image-captioning-large
OCR_LANG=ch

# Limits
MAX_IMAGE_SIZE=10485760        # 10MB
MAX_CONCURRENT_REQUESTS=4

# Paths
MODEL_CACHE_DIR=./models
```

#### AI Agents Service

```bash
cd services/ai_agents
cp .env.example .env  # If exists
```

Edit `.env` as needed:

```env
# Ollama LLM Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2

# Service Configuration
PORT=8003
LOG_LEVEL=INFO
```

#### RAG Service

```bash
cd services/rag
cp .env.example .env
```

Edit `.env` as needed:

```env
# Qdrant Vector Store
QDRANT_HOST=localhost
QDRANT_PORT=6333
COLLECTION_NAME=documents

# LLM Configuration
LLM_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2

# Embedding Model
EMBEDDING_MODEL=BAAI/bge-m3
HF_ENDPOINT=

# Service Configuration
PORT=8001
LOG_LEVEL=INFO
```

#### Text Service

```bash
cd services/text-service
cp .env.example .env
```

Edit `.env` as needed:

```env
# Default LLM Provider
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o-mini

# OpenAI (optional)
OPENAI_API_KEY=sk-your-key-here

# Anthropic (optional)
ANTHROPIC_API_KEY=sk-ant-your-key-here

# Ollama (local, optional)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b

# Service Configuration
HOST=0.0.0.0
PORT=8004
LOG_LEVEL=INFO
```

#### TTS Service

```bash
cd services/tts-service
cp .env.example .env
```

Edit `.env` as needed:

```env
# TTS Provider (azure, google, elevenlabs, coqui, edge)
TTS_PROVIDER=azure

# Azure Cognitive Services (if using Azure)
AZURE_SPEECH_KEY=your-key
AZURE_SPEECH_REGION=eastus

# Google Cloud TTS (if using Google)
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json

# ElevenLabs (if using ElevenLabs)
ELEVENLABS_API_KEY=your-api-key

# Coqui TTS (if using Coqui)
COQUI_MODEL_PATH=/path/to/model

# Service Configuration
HOST=0.0.0.0
PORT=8004
```

#### Media Gen Service

```bash
cd services/media-gen
cp .env.example .env
```

Edit `.env` as needed:

```env
# Service Configuration
MEDIA_GEN_PORT=3456

# Stable Diffusion Model
SD_MODEL=runwayml/stable-diffusion-v1-5

# Device: auto, cpu, cuda, mps
MEDIA_GEN_DEVICE=auto
```

# Embedding Model
EMBEDDING_MODEL=BAAI/bge-m3
HF_ENDPOINT=

# Service Configuration
PORT=8001
LOG_LEVEL=INFO
```

## Running Services

### Service Ports


| Service        | Port | Description                                      |
| -------------- | ---- | ----------------------------------------------- |
| Vision Service | 8000 | Image recognition (YOLO, BLIP, OCR), Image Gen, Video |
| AI Agents      | 8003 | Multi-agent orchestration                          |
| RAG Service    | 8001 | Retrieval-augmented generation                     |
| Text Service   | 8006 | Text generation (GPT, Claude, Ollama)             |
| TTS Service    | 8005 | Text-to-Speech (Azure, Google, ElevenLabs, Coqui) |
| Media Gen      | 3456 | Local Stable Diffusion (CPU/GPU)                  |
| Web Frontend   | 4200 | Angular frontend                                                |
| Backend Server | 3000 | Express.js backend                              |
| Qdrant        | 6333 | Vector database                                 |
| Ollama         | 11434 | Local LLM (optional)                           |


### Vision Service (Port 8000)

```bash
cd services/vision-service
source .venv/bin/activate

# Run with hot reload
uvicorn src.main:app --reload --port 8000

# Or with explicit host
uvicorn src.main:app --host 0.0.0.0 --port 8000
```

### AI Agents Service (Port 8003)

```bash
cd services/ai_agents
source .venv/bin/activate

# Run with hot reload
uvicorn main:app --reload --port 8003

# Or with explicit host
uvicorn main:app --host 0.0.0.0 --port 8003
```

**Available Agents:**

- Supervisor: Orchestrates other agents
- RAG Agent: Document retrieval and Q&A
- LLMOps Agent: Model lifecycle management
- AIOps Agent: AI infrastructure operations
- Pipeline Agent: Data pipeline management
- Feature Store Agent: Feature engineering
- K8s Agent: Kubernetes operations
- Monitoring Agent: System monitoring
- Vector DB Agent: Vector database operations
- Model Agent: ML model management

### RAG Service (Port 8001)

```bash
cd services/rag
source .venv/bin/activate

# Run with hot reload
uvicorn src.main:app --reload --port 8001

# Or with explicit host
uvicorn src.main:app --host 0.0.0.0 --port 8001
```

**Prerequisites for RAG Service:**

- Qdrant vector store running (or disable via config)
- Ollama or OpenAI API for LLM

```bash
# Run Qdrant with Docker (optional)
docker run -p 6333:6333 qdrant/qdrant
```

### Text Service (Port 8006)

```bash
cd services/text-service
source .venv/bin/activate

# Run with hot reload
uvicorn src.main:app --reload --port 8006

# Or with explicit host
uvicorn src.main:app --host 0.0.0.0 --port 8006
```

### TTS Service (Port 8005)

```bash
cd services/tts-service
source .venv/bin/activate

# Run with hot reload
python -m uvicorn src.main:app --reload

# Or with explicit host and port
python -m uvicorn src.main:app --host 0.0.0.0 --port 8005
```

### Media Gen Service (Port 3456)

```bash
cd services/media-gen
source .venv/bin/activate

# Run the service
python app.py
```

### Web Frontend (Angular)

```bash
cd apps/web-angular
pnpm install
pnpm start
```

### Backend Server (Node.js)

```bash
cd apps/server
pnpm dev
```

### All Services (Monorepo)

```bash
# Run all services in parallel
pnpm dev

# Or run specific service
pnpm --filter @ai-test/web dev
pnpm --filter @ai-test/server dev
```

### Multi-Service Development

For local development with all services:

```bash
# Terminal 1: Vision Service
cd services/vision-service && source .venv/bin/activate && uvicorn src.main:app --reload --port 8000

# Terminal 2: AI Agents Service
cd services/ai_agents && source .venv/bin/activate && uvicorn main:app --reload --port 8003

# Terminal 3: RAG Service
cd services/rag && source .venv/bin/activate && uvicorn src.main:app --reload --port 8001

# Terminal 4: Web Frontend (Angular)
cd apps/web-angular && pnpm start

# Terminal 5: Backend Server (optional)
cd apps/server && pnpm dev
```

## Testing

### Vision Service Tests

```bash
cd services/vision-service
source .venv/bin/activate

# Run all tests
python -m pytest tests/ -v

# Run specific test file
python -m pytest tests/test_api.py -v

# Run with coverage
python -m pytest tests/ -v --cov=src --cov-report=html
```

### AI Agents Tests

```bash
cd services/ai_agents
source .venv/bin/activate

# Run all tests
python -m pytest tests/ -v

# Run specific test file
python -m pytest tests/test_api.py -v
```

### RAG Service Tests

```bash
cd services/rag
source .venv/bin/activate

# Run all tests
python -m pytest tests/ -v

# Run with coverage
python -m pytest tests/ -v --cov=src --cov-report=html

# Lint code
ruff check src/
```

### TypeScript Tests (Web/Server)

```bash
# Run all workspace tests
pnpm test

# Run specific package tests
pnpm --filter @ai-test/web test
pnpm --filter @ai-test/server test
```

### Test Structure

```
services/vision-service/tests/
├── conftest.py           # Shared fixtures
├── __init__.py
├── test_api.py          # API endpoint tests
├── test_config.py       # Configuration tests
└── test_schemas.py     # Schema validation tests
```

### Writing Tests

#### API Tests

```python
import pytest
from fastapi.testclient import TestClient
from src.main import app

@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c

def test_detect_endpoint(client, sample_image_bytes):
    response = client.post(
        "/vision/detect",
        files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        data={"conf": 0.5}
    )
    assert response.status_code == 200
    data = response.json()
    assert "detections" in data
```

#### Mocking Models

```python
from unittest.mock import AsyncMock, MagicMock, patch

@pytest.fixture
def mock_yolo():
    mock = MagicMock()
    mock.detect = AsyncMock(return_value=DetectionResponse(...))
    return mock

@pytest.fixture
def client(mock_yolo):
    with patch("src.api.vision._yolo", mock_yolo):
        # Test code
        pass
```

## Linting

### TypeScript

```bash
# Lint all packages
pnpm lint

# Lint specific package
pnpm --filter @ai-test/web lint
pnpm --filter @ai-test/server lint
```

### Python

```bash
cd services/vision-service
# Uses pyproject.toml configuration
# Install linter: pip install ruff
ruff check src/
```

## Building

### Web Frontend

```bash
cd apps/web-angular
pnpm build
```

### Backend Server

```bash
cd apps/server
pnpm build
```

### AI Service (Docker)

```bash
cd services/vision-service

# Build image
docker build -t ai-service:latest .

# Run with Docker Compose
docker compose up ai
```

## Docker Development

### GPU Variant (Recommended for AI)

```bash
cd services/vision-service
docker compose up ai
```

### CPU Variant

```bash
docker compose up ai-cpu
```

### Custom Configuration

```bash
# Override environment
docker run -e DEVICE=cpu -e MAX_CONCURRENT_REQUESTS=2 \
  -p 8000:8000 ai-service:latest
```

## Code Style

### TypeScript

- Use TypeScript strict mode
- Prefer `const` over `let`
- Use explicit types for function parameters
- Use named exports for better IDE support

### Python

- Follow PEP 8
- Use type hints
- Use async/await for I/O operations
- Use `loguru` for logging

## Debugging

### Python AI Service

Enable debug mode:

```bash
# In .env
LOG_LEVEL=DEBUG
```

Or run with verbose output:

```bash
uvicorn src.main:app --reload --log-level debug
```

### Angular Frontend

Enable Angular DevTools and check network tab for API calls.

### API Requests

Test with curl or Postman:

```bash
# Health check
curl http://localhost:8000/health

# Upload image
curl -X POST http://localhost:8000/vision/caption \
  -F "file=@test-image.jpg" \
  -v
```

## Adding New Features

### 1. New API Endpoint

```python
# services/vision-service/src/api/vision.py
@router.post("/new-endpoint")
async def new_endpoint(
    file: UploadFile = File(...),
):
    image = await load_image(file)
    # Process image
    return {"result": "..."}
```

### 2. New Schema

```python
# services/vision-service/src/schemas/vision.py
class NewResponse(BaseModel):
    field1: str
    field2: int
```

### 3. New Test

```python
# services/vision-service/tests/test_api.py
def test_new_endpoint(self, client, sample_image_bytes):
    response = client.post("/vision/new-endpoint", ...)
    assert response.status_code == 200
```

## Troubleshooting

### Port Already in Use

Find and kill the process:

```bash
# macOS/Linux
lsof -i :8000
kill <PID>

# Windows
netstat -ano | findstr :8000
taskkill /PID <PID> /F
```

### Python Import Errors

Make sure you're in the virtual environment:

```bash
source services/vision-service/.venv/bin/activate
which python
```

### Model Download Issues

Set HuggingFace token:

```bash
export HF_TOKEN=your_token_here
```

Or login:

```bash
pip install huggingface_hub
huggingface-cli login
```

### Node.js Build Errors

Clear caches and reinstall:

```bash
rm -rf node_modules
rm pnpm-lock.yaml
pnpm install
```

### CUDA/GPU Issues

Verify CUDA:

```bash
python -c "import torch; print(torch.cuda.is_available())"
```

If using Docker, ensure NVIDIA Container Toolkit is installed:

```bash
docker run --rm --gpus all nvidia/cuda:11.8-base-ubuntu22.04 nvidia-smi
```

## Common Tasks

### Update Dependencies

```bash
# Node.js
pnpm up --latest

# Python
pip install --upgrade -r requirements.txt
```

### Add New Workspace Package

1. Create package in `packages/` or `apps/`
2. Add to `pnpm-workspace.yaml`
3. Add workspace reference in package.json

### Run Specific Tests

```bash
# Single test file
python -m pytest tests/test_api.py -v

# Single test
python -m pytest tests/test_api.py::TestDetectEndpoint::test_detect_success -v

# Tests matching pattern
python -m pytest -k "detect" -v
```

## Resources

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Angular Documentation](https://angular.dev/)
- [Ultralytics YOLO](https://docs.ultralytics.com/)
- [HuggingFace Transformers](https://huggingface.co/docs/transformers/)
- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)

