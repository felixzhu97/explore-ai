# Text-to-Video Service

Production-ready text-to-video generation API with pluggable provider support, built on DDD architecture.

## Supported Providers


| Provider      | Status    | API Required | Notes                         |
| ------------- | --------- | ------------ | ----------------------------- |
| **Mock**      | Available | No           | For testing/development       |
| **Replicate** | Available | Yes          | Uses Kling model on Replicate |
| **Kling**     | Available | Yes          | Official Kling AI API         |
| **Pika**      | Available | Yes          | Pika Labs API                 |
| **Runway**    | Available | Yes          | Runway ML API                 |
| **Sora**      | Available | Yes          | OpenAI Sora API               |


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

## Architecture (DDD)

The video generation service follows Domain-Driven Design principles:

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer                              │
│              src/api/video.py (FastAPI Routes)              │
├─────────────────────────────────────────────────────────────┤
│                  Application Layer                          │
│  src/application/use_cases/                                │
│  ├── generate_video.py      # VideoGenerationUseCase        │
│  └── check_video_status.py  # CheckVideoStatusUseCase      │
│  src/application/dtos/video_dtos.py                         │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  src/domain/                                               │
│  ├── entities/video_task.py                                 │
│  │   ├── VideoTask        # Core entity with business logic │
│  │   └── VideoTaskStatus  # Enum: PENDING/PROCESSING/...   │
│  ├── value_objects/                                         │
│  │   └── video_config.py                                   │
│  └── services/                                             │
│      └── video_generation_service.py                       │
├─────────────────────────────────────────────────────────────┤
│                Infrastructure Layer                         │
│  src/providers/                                            │
│  ├── base.py          # BaseVideoProvider                  │
│  ├── interfaces.py    # Protocol definitions               │
│  ├── mock.py          # MockProvider (testing)            │
│  ├── kling.py         # KlingProvider                     │
│  ├── replicate.py     # ReplicateProvider                 │
│  ├── pika.py          # PikaProvider                      │
│  ├── runway.py        # RunwayProvider                     │
│  └── sora.py          # SoraProvider                      │
└─────────────────────────────────────────────────────────────┘
```

### Domain Entity: VideoTask

The `VideoTask` entity encapsulates business rules:

```python
@dataclass
class VideoTask:
    task_id: str           # Unique identifier
    prompt: str            # User's video description
    status: VideoTaskStatus  # PENDING/PROCESSING/COMPLETED/FAILED

    # URLs (available when completed)
    video_url: Optional[str]
    thumbnail_url: Optional[str]

    # Error handling
    error_message: Optional[str]

    # Timestamps
    created_at: datetime
    completed_at: Optional[datetime]
    processing_time_seconds: Optional[float]

    # State transitions
    def mark_processing() -> None
    def mark_completed(video_url, thumbnail_url) -> None
    def mark_failed(error_message) -> None

    @property
    def is_terminal(self) -> bool  # COMPLETED or FAILED
```

### Application Use Cases

#### GenerateVideoUseCase

```python
class GenerateVideoUseCase:
    def execute(request: VideoGenerationRequestDTO) -> VideoGenerationResponseDTO:
        # 1. Validate request
        # 2. Create VideoTask entity
        # 3. Delegate to domain service
        # 4. Return response DTO
```

#### CheckVideoStatusUseCase

```python
class CheckVideoStatusUseCase:
    def execute(task_id: str) -> VideoTaskStatusDTO:
        # 1. Query provider for status
        # 2. Return status DTO
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
  "task_id": "550e8400-e29b-41d4-a716-446655440000",
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

**Response (Pending):**

```json
{
  "task_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "pending",
  "video_url": null,
  "thumbnail_url": null,
  "error": null,
  "processing_time_seconds": null
}
```

**Response (Completed):**

```json
{
  "task_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed",
  "video_url": "https://cdn.example.com/videos/abc123.mp4",
  "thumbnail_url": "https://cdn.example.com/thumbnails/abc123.jpg",
  "error": null,
  "processing_time_seconds": 45.5
}
```

## Usage Examples

### cURL

```bash
# Generate video
curl -X POST http://localhost:8000/video/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A cat playing piano in moonlight", "duration": 5}'

# Advanced generation
curl -X POST http://localhost:8000/video/generate/advanced \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A sunset over the ocean", "style": "cinematic", "duration": 10}'

# Check status
curl http://localhost:8000/video/status/{task_id}
```

### Python (httpx)

```python
import httpx
import asyncio

async def generate_video():
    async with httpx.AsyncClient(timeout=60.0) as client:
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
        print(f"Task created: {task_id}")

        # Poll status
        for _ in range(30):
            await asyncio.sleep(2)
            status_response = await client.get(
                f"http://localhost:8000/video/status/{task_id}"
            )
            status = status_response.json()
            print(f"Status: {status['status']}")

            if status["status"] in ("completed", "failed"):
                if status["video_url"]:
                    print(f"Video URL: {status['video_url']}")
                else:
                    print(f"Error: {status['error']}")
                break

asyncio.run(generate_video())
```

### Python (Domain Layer)

```python
from src.domain.entities.video_task import VideoTask, VideoTaskStatus

# Create a video task
task = VideoTask(prompt="A beautiful sunset")

# State transitions
task.mark_processing()
task.mark_completed(
    video_url="https://example.com/video.mp4",
    thumbnail_url="https://example.com/thumb.jpg"
)

print(f"Task {task.task_id} completed in {task.processing_time_seconds}s")
```

## Parameters

### VideoGenerateRequest


| Parameter         | Type   | Default    | Description                        |
| ----------------- | ------ | ---------- | ---------------------------------- |
| `prompt`          | string | required   | Text description of the video      |
| `negative_prompt` | string | null       | Elements to avoid                  |
| `duration`        | int    | 5          | 5-10 seconds                       |
| `aspect_ratio`    | enum   | 16:9       | 16:9, 9:16, 1:1, 4:3               |
| `fps`             | int    | 24         | 24-60 fps                          |
| `quality`         | enum   | high       | standard, high                     |
| `model`           | enum   | kling-v1-5 | kling-v1-0, kling-v1-5             |
| `callback_url`    | string | null       | Webhook URL for async notification |


### Advanced Options


| Parameter          | Type  | Default | Description                               |
| ------------------ | ----- | ------- | ----------------------------------------- |
| `style`            | enum  | none    | realistic, animation, cinematic, abstract |
| `seed`             | int   | null    | Random seed for reproducibility           |
| `cfg_scale`        | float | 7.5     | Prompt adherence strength (1-20)          |
| `motion_intensity` | float | 1.0     | Motion strength (0.1-2.0)                 |


## Adding a New Provider

1. Create `src/providers/your_provider.py` implementing `BaseVideoProvider`:

```python
from typing import Optional, Dict, Any
from .base import BaseVideoProvider

class YourVideoProvider(BaseVideoProvider):
    @property
    def provider_name(self) -> str:
        return "your_provider"

    async def generate_video(
        self,
        prompt: str,
        **kwargs
    ) -> Dict[str, Any]:
        # Implement API call
        return {"task_id": "...", "status": "pending"}

    async def get_task_status(self, task_id: str) -> Dict[str, Any]:
        # Implement status check
        return {"task_id": task_id, "status": "..."}
```

1. Update `src/providers/__init__.py`:

```python
from .your_provider import YourVideoProvider

providers = {
    "your_provider": YourVideoProvider,
    # ...
}
```

1. The provider will be auto-discovered and available via `VIDEO_PROVIDER=your_provider`.

## Provider Factory

Providers are managed through dependency injection:

```python
from src.core.di import get_video_provider

@router.post("/video/generate")
async def generate_video(
    request: VideoGenerateRequest,
    provider: BaseVideoProvider = Depends(get_video_provider)
):
    result = await provider.generate_video(prompt=request.prompt, ...)
    return result
```

