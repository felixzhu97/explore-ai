# Text-to-Video Service

Production-ready text-to-video generation API with pluggable provider support.

## Supported Providers

| Provider | Status | API Required | Notes |
|----------|--------|--------------|-------|
| **Mock** | Available | No | For testing/development |
| **Replicate** | Available | Yes | Uses Kling model on Replicate |
| **Kling** | Available | Yes | Official Kling AI API |

## Quick Start

### 1. Configuration

Copy `.env.example` to `.env` and configure:

```bash
# For development/testing (no API key needed)
VIDEO_PROVIDER=mock

# For production with Kling AI
VIDEO_PROVIDER=kling
KLING_API_KEY=your_kling_api_key
KLING_API_SECRET=your_kling_api_secret

# For production with Replicate
VIDEO_PROVIDER=replicate
REPLICATE_API_TOKEN=your_replicate_token
```

### 2. Run the Service

```bash
cd services/vision-service
pip install -e .
uvicorn src.main:app --reload --port 8000
```

## API Endpoints

### Generate Video

```
POST /video/generate
```

**Request Body:**

```json
{
  "prompt": "A serene lake at sunset with ducks swimming",
  "negative_prompt": "blurry, low quality",
  "duration": 5,
  "aspect_ratio": "16:9",
  "fps": 24,
  "quality": "high",
  "model": "kling-v1-5"
}
```

**Response:**

```json
{
  "task_id": "mock_task_a1b2c3d4e5f6",
  "status": "pending",
  "message": "Video generation started",
  "created_at": "2026-05-17T09:45:00"
}
```

### Advanced Generation

```
POST /video/generate/advanced
```

**Request Body:**

```json
{
  "prompt": "A robot dancing in a futuristic city",
  "style": "cinematic",
  "cfg_scale": 7.5,
  "motion_intensity": 1.2,
  "seed": 42,
  "duration": 10
}
```

### Check Status

```
GET /video/status/{task_id}
```

**Response (completed):**

```json
{
  "task_id": "mock_task_a1b2c3d4e5f6",
  "status": "completed",
  "video_url": "https://example.com/videos/abc123.mp4",
  "thumbnail_url": "https://example.com/thumbnails/abc123.jpg",
  "processing_time_seconds": 5.0
}
```

## Usage Examples

### cURL

```bash
# Generate video
curl -X POST http://localhost:8000/video/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A cat playing piano in moonlight", "duration": 5}'

# Check status
curl http://localhost:8000/video/status/{task_id}
```

### Python (httpx)

```python
import httpx

async def generate_video():
    async with httpx.AsyncClient() as client:
        # Create task
        response = await client.post(
            "http://localhost:8000/video/generate",
            json={
                "prompt": "A serene mountain landscape with flowing water",
                "duration": 5,
                "aspect_ratio": "16:9",
                "quality": "high"
            }
        )
        task_data = response.json()
        task_id = task_data["task_id"]

        # Poll status
        import asyncio
        for _ in range(30):
            await asyncio.sleep(2)
            status_response = await client.get(
                f"http://localhost:8000/video/status/{task_id}"
            )
            status = status_response.json()
            if status["status"] in ("completed", "failed"):
                print(f"Video URL: {status.get('video_url')}")
                break
```

### Python (SDK)

```python
from src.providers import get_provider

async def generate():
    provider = get_provider()

    result = await provider.generate_video(
        prompt="A sunset over the ocean",
        duration=5,
        aspect_ratio="16:9"
    )

    task_id = result["task_id"]
    print(f"Task ID: {task_id}")
```

## Parameters

### VideoGenerateRequest

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `prompt` | string | required | Text description of the video |
| `negative_prompt` | string | null | Elements to avoid |
| `duration` | int | 5 | 5-10 seconds |
| `aspect_ratio` | enum | 16:9 | 16:9, 9:16, 1:1, 4:3 |
| `fps` | int | 24 | 24-60 fps |
| `quality` | enum | high | standard, high |
| `model` | enum | kling-v1.5 | kling-v1.0, kling-v1.5 |
| `callback_url` | string | null | Webhook URL for async notification |

### Advanced Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `style` | enum | none | realistic, animation, cinematic, abstract |
| `seed` | int | null | Random seed for reproducibility |
| `cfg_scale` | float | 7.5 | Prompt adherence strength (1-20) |
| `motion_intensity` | float | 1.0 | Motion strength (0.1-2.0) |

## Adding a New Provider

1. Create `src/providers/your_provider.py`:

```python
from typing import Optional
from .base import BaseVideoProvider

class YourVideoProvider(BaseVideoProvider):
    @property
    def provider_name(self) -> str:
        return "your_provider"

    async def generate_video(self, prompt: str, **kwargs) -> dict:
        # Implement API call
        return {"task_id": "...", "status": "pending"}

    async def get_task_status(self, task_id: str) -> dict:
        # Implement status check
        return {"task_id": task_id, "status": "..."}
```

2. Update `src/core/video_config.py`:

```python
class VideoProvider(str, Literal):
    YOUR_PROVIDER = "your_provider"  # Add this
```

3. Update `src/providers/__init__.py`:

```python
from .your_provider import YourVideoProvider

providers = {
    VideoProvider.YOUR_PROVIDER: YourVideoProvider,
    # ...
}
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    FastAPI App                       │
├─────────────────────────────────────────────────────┤
│  /video/generate    /video/generate/advanced        │
│  /video/status/{id}                               │
├─────────────────────────────────────────────────────┤
│                 Video API Router                     │
├─────────────────────────────────────────────────────┤
│              Video Provider Factory                  │
│  ┌─────────┐  ┌──────────┐  ┌────────┐  ┌──────┐  │
│  │  Mock   │  │ Replicate │  │ Kling  │  │ ...  │  │
│  └─────────┘  └──────────┘  └────────┘  └──────┘  │
├─────────────────────────────────────────────────────┤
│                  External APIs                       │
└─────────────────────────────────────────────────────┘
```
