# Quick Start Guide

Get the AI Vision Service running in 5 minutes.

## Prerequisites

- Node.js >= 20
- pnpm >= 9
- Python >= 3.9
- (Optional) NVIDIA GPU with CUDA for faster inference

## Option 1: Run with Docker (Recommended)

The fastest way to get everything running.

### 1. Start the AI Service

```bash
cd services/vision-service

# With GPU support (requires NVIDIA Container Toolkit)
docker compose up ai

# Or CPU only
docker compose up ai-cpu
```

The AI service will be available at `http://localhost:8000`.

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

# Install Python dependencies
cd services/vision-service
python3 -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Configure Environment

```bash
cd services/vision-service
cp .env.example .env
```

Edit `.env` if needed:

```env
DEVICE=cuda          # or 'cpu' for CPU-only inference
YOLO_MODEL=yolo11n.pt
BLIP_MODEL=Salesforce/blip-image-captioning-large
OCR_LANG=ch
MAX_IMAGE_SIZE=10485760  # 10MB in bytes
```

### 3. Download AI Models

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

Terminal 1 - AI Service:
```bash
cd services/vision-service
source .venv/bin/activate
uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload
```

Terminal 2 - Web Frontend:
```bash
pnpm dev
```

Terminal 3 - Backend Server (optional):
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

#### Object Detection

```bash
curl -X POST http://localhost:8000/vision/detect \
  -F "file=@your-image.jpg" \
  -F "conf=0.25"
```

#### Image Captioning

```bash
curl -X POST http://localhost:8000/vision/caption \
  -F "file=@your-image.jpg"
```

#### OCR

```bash
curl -X POST http://localhost:8000/vision/ocr \
  -F "file=@your-image.jpg"
```

#### Combined Analysis

```bash
# As query parameter
curl -X POST "http://localhost:8000/vision/analyze?task=caption_image" \
  -F "file=@your-image.jpg"
```

## Troubleshooting

### "No module named 'torch'"

Activate the virtual environment:

```bash
source services/vision-service/.venv/bin/activate
```

### GPU not detected

1. Verify NVIDIA drivers are installed:
```bash
nvidia-smi
```

2. Check CUDA availability in Python:
```bash
python -c "import torch; print(torch.cuda.is_available())"
```

3. If using Docker, ensure `--gpus all` flag is used or NVIDIA Container Toolkit is installed.

### Port already in use

Change the port by setting the `PORT` environment variable:

```bash
PORT=8001 uvicorn src.main:app --port 8001
```

### Model download fails

Set up HuggingFace credentials for gated models:

```bash
pip install huggingface_hub
huggingface-cli login
```

## Next Steps

- [Architecture Overview](./ARCHITECTURE.md) - Understand the system design
- [API Reference](./API.md) - Explore all endpoints
- [Development Guide](./DEVELOPMENT.md) - Set up for contributions
