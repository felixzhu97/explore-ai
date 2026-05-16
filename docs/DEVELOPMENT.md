# Development Guide

This guide covers local development setup, testing, and contribution workflow.

## Project Structure

```
ai-test/
├── apps/
│   ├── web/           # React frontend (TypeScript + Vite)
│   └── server/        # Express.js backend (TypeScript)
├── packages/
│   ├── config/        # Shared TypeScript config
│   └── utils/         # Shared utilities
├── services/
│   └── vision-service/           # Python AI service (FastAPI)
└── docs/             # Documentation
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

```bash
cd services/vision-service

# Create virtual environment
python3 -m venv .venv

# Activate virtual environment
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate   # Windows

# Install dependencies
pip install -r requirements.txt

# Install dev dependencies for testing
pip install pytest pytest-asyncio httpx
```

### 3. Environment Configuration

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

## Running Services

### AI Service (Python)

```bash
cd services/vision-service
source .venv/bin/activate

# Run with hot reload
uvicorn src.main:app --reload --port 8000

# Or with explicit host
uvicorn src.main:app --host 0.0.0.0 --port 8000
```

### Web Frontend (Node.js)

```bash
cd apps/web
pnpm dev
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

## Testing

### Python Tests (AI Service)

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
cd apps/web
pnpm build

# Preview production build
pnpm preview
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

### React Frontend

Enable React DevTools and check network tab for API calls.

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
- [React Documentation](https://react.dev/)
- [Ultralytics YOLO](https://docs.ultralytics.com/)
- [HuggingFace Transformers](https://huggingface.co/docs/transformers/)
- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)

