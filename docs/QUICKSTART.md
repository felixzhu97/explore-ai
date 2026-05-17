# Quick Start Guide

Get the AI services running in 5 minutes.

## Prerequisites

- Node.js >= 20
- pnpm >= 9
- Python >= 3.9
- (Optional) NVIDIA GPU with CUDA for faster inference

## Service Overview

This project includes 6 AI microservices:


| Service        | Port | Description                                  |
| -------------- | ---- | -------------------------------------------- |
| RAG Service    | 8001 | Retrieval-augmented generation, document Q&A |
| Vision Service | 8000 | Image recognition (YOLO, BLIP, OCR)          |
| Vision CPU     | 8002 | Vision Service (CPU-only variant)            |
| AI Agents      | 8003 | Multi-agent orchestration                    |
| Text Service   | 8006 | Text generation (GPT, Claude, Ollama)       |
| TTS Service    | 8005 | Speech synthesis (Azure, Google, ElevenLabs) |
| Media Gen      | 3456 | Local Stable Diffusion                      |


## Option 1: Run with Docker (Recommended)

The fastest way to get everything running.

### 1. Start AI Services

```bash
# Start all services with Docker Compose
cd services/vision-service && docker compose up ai -d
cd services/ai_agents && docker compose up -d
cd services/rag && docker compose up -d
```

Or start individual services:

```bash
# Vision Service (with GPU)
cd services/vision-service && docker compose up ai -d

# AI Agents Service
cd services/ai_agents && docker compose up -d

# RAG Service
cd services/rag && docker compose up -d
```

### 2. Start the Web Frontend

```bash
cd apps/web
pnpm install
pnpm dev
```

The web app will be available at `http://localhost:5173`.

## Option 2: Run Locally

For development with hot reloading.

### 1. Install Dependencies

```bash
# Install Node.js dependencies
pnpm install

# Vision Service
cd services/vision-service
python3 -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt

# AI Agents Service
cd ../ai_agents
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# RAG Service
cd ../rag
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Text Service
cd ../text-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# TTS Service
cd ../tts-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Media Gen Service
cd ../media-gen
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 2. Configure Environment

For each service, copy the example env file:

```bash
# Vision Service
cd services/vision-service
cp .env.example .env

# AI Agents Service
cd ../ai_agents
cp .env.example .env

# RAG Service
cd ../rag
cp .env.example .env

# Text Service
cd ../text-service
cp .env.example .env

# TTS Service
cd ../tts-service
cp .env.example .env

# Media Gen Service
cd ../media-gen
cp .env.example .env
```

### 3. Download AI Models (Vision Service)

The first run will automatically download required models. You can pre-download them:

```bash
# YOLO model (downloaded automatically by ultralytics)
python -c "from ultralytics import YOLO; YOLO('yolo11n.pt')"

# BLIP model (downloaded automatically by transformers)
python -c "from transformers import AutoProcessor, BlipForConditionalGeneration; \
    AutoProcessor.from_pretrained('Salesforce/blip-image-captioning-large'); \
    BlipForConditionalGeneration.from_pretrained('Salesforce/blip-image-captioning-large')"
```

### 4. Start Services

#### Vision Service (Port 8000)

Terminal 1:

```bash
cd services/vision-service
source .venv/bin/activate
uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload
```

#### AI Agents Service (Port 8003)

Terminal 2:

```bash
cd services/ai_agents
source .venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8003 --reload
```

#### RAG Service (Port 8001)

Terminal 3:

```bash
cd services/rag
source .venv/bin/activate
uvicorn src.main:app --host 0.0.0.0 --port 8001 --reload
```

#### Web Frontend

Terminal 4:

```bash
pnpm dev
```

#### Backend Server (Optional)

Terminal 5:

```bash
cd apps/server
pnpm dev
```

## Usage

### Web Interface

1. Open `http://localhost:5173` in your browser
2. Drag and drop an image or click to select
3. Choose an analysis task:
  - **Caption** - Generate image description
  - **Detect** - Find objects in image
  - **OCR** - Extract text from image
  - **Analyze** - Run all tasks
4. Click "Analyze Image" and view results

### API Usage

#### RAG Service (Port 8001)

```bash
# Health check
curl http://localhost:8001/health

# Upload document
curl -X POST http://localhost:8001/api/documents/upload \
  -F "file=@document.pdf"

# Chat with documents
curl -X POST http://localhost:8001/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is this document about?"}'
```

#### Vision Service (Port 8000)

```bash
# Health check
curl http://localhost:8000/health

# Object Detection
curl -X POST http://localhost:8000/vision/detect \
  -F "file=@your-image.jpg" \
  -F "conf=0.25"

# Image Captioning
curl -X POST http://localhost:8000/vision/caption \
  -F "file=@your-image.jpg"

# OCR
curl -X POST http://localhost:8000/vision/ocr \
  -F "file=@your-image.jpg"

# Combined Analysis
curl -X POST "http://localhost:8000/vision/analyze?task=caption_image" \
  -F "file=@your-image.jpg"
```

#### AI Agents Service (Port 8003)

```bash
# Health check
curl http://localhost:8003/health

# List available agents
curl http://localhost:8003/agents

# Invoke supervisor agent (streaming)
curl -X POST http://localhost:8003/api/agents/supervisor/invoke \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Help me manage my ML pipeline"}]}'

# Invoke specific agent
curl -X POST http://localhost:8003/api/agents/rag_agent/invoke \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Search for documents about ML"}]}'
```

#### Text Service (Port 8006)

```bash
# Health check
curl http://localhost:8006/api/text/health

# List providers
curl http://localhost:8006/api/text/providers

# Text completion
curl -X POST http://localhost:8006/api/text/complete \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Explain quantum computing:", "temperature": 0.7}'

# Chat
curl -X POST http://localhost:8006/api/text/chat \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Hello"}]}'
```

#### TTS Service (Port 8013)

```bash
# Health check
curl http://localhost:8013/tts/health

# List voices
curl http://localhost:8013/tts/voices

# Synthesize speech
curl -X POST http://localhost:8013/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello world!", "voice": "en-US-JennyNeural"}'
```

#### Media Gen Service (Port 8015)

```bash
# Health check
curl http://localhost:8015/health

# Generate image
curl -X POST http://localhost:8015/image/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A cat sitting on a windowsill", "width": 512, "height": 512}'
```

## Troubleshooting

### "No module named 'torch'"

Activate the virtual environment for the appropriate service:

```bash
# Vision Service
source services/vision-service/.venv/bin/activate

# AI Agents Service
source services/ai_agents/.venv/bin/activate

# RAG Service
source services/rag/.venv/bin/activate
```

### GPU not detected

1. Verify NVIDIA drivers are installed:

```bash
nvidia-smi
```

1. Check CUDA availability in Python:

```bash
python -c "import torch; print(torch.cuda.is_available())"
```

1. If using Docker, ensure `--gpus all` flag is used or NVIDIA Container Toolkit is installed.

### Port already in use

Change the port by setting the `PORT` environment variable or command line:

```bash
# Vision Service (default 8000)
PORT=8000 uvicorn src.main:app --port 8000

# Vision Service CPU (default 8002)
PORT=8002 uvicorn src.main:app --port 8002

# AI Agents Service (default 8003)
PORT=8003 uvicorn main:app --port 8003

# Text Service (default 8006)
PORT=8006 uvicorn src.main:app --port 8006

# TTS Service (default 8005)
PORT=8005 python -m uvicorn src.main:app --port 8005

# Media Gen Service (default 3456)
PORT=3456 python app.py
```

### Model download fails

Set up HuggingFace credentials for gated models:

```bash
pip install huggingface_hub
huggingface-cli login
```

### Ollama not available (AI Agents / RAG)

If using local Ollama models:

```bash
# Start Ollama server
ollama serve

# Pull a model
ollama pull llama3.2
```

## Next Steps

- [Architecture Overview](./ARCHITECTURE.md) - Understand the system design
- [API Reference](./API.md) - Explore all endpoints
- [Development Guide](./DEVELOPMENT.md) - Set up for contributions

