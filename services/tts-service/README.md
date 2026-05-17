# Text-to-Speech (TTS) Service

## Overview

This module provides a unified interface for text-to-speech synthesis, supporting multiple providers:

- **Azure Cognitive Services TTS** - High-quality neural voices, multi-language support
- **Google Cloud Text-to-Speech** - Natural-sounding voices, WaveNet voices
- **ElevenLabs** - Ultra-realistic AI voices with emotion control
- **Coqui TTS (Local)** - Open-source local TTS for privacy-focused deployments

## Architecture

```
services/tts-service/
├── src/
│   ├── __init__.py
│   ├── main.py                    # FastAPI application
│   ├── config.py                  # Configuration management
│   ├── providers/
│   │   ├── __init__.py
│   │   ├── base.py               # Base provider interface
│   │   ├── azure_tts.py          # Azure TTS implementation
│   │   ├── google_tts.py         # Google TTS implementation
│   │   ├── elevenlabs_tts.py     # ElevenLabs implementation
│   │   └── coqui_tts.py         # Coqui local TTS
│   ├── schemas.py                # Pydantic models
│   ├── routers/
│   │   ├── __init__.py
│   │   └── tts.py               # TTS API endpoints
│   └── utils/
│       ├── __init__.py
│       └── audio.py             # Audio utilities
├── tests/
├── requirements.txt
├── .env.example
└── README.md
```

## Quick Start

### 1. Install Dependencies

```bash
cd services/tts-service
pip install -r requirements.txt
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your API keys
```

### 3. Run the Service

```bash
python -m uvicorn src.main:app --reload --port 8005
```

## Environment Variables


| Variable                         | Description                                      | Required   |
| -------------------------------- | ------------------------------------------------ | ---------- |
| `TTS_PROVIDER`                   | Provider name (azure, google, elevenlabs, coqui) | Yes        |
| `AZURE_SPEECH_KEY`               | Azure Cognitive Services subscription key        | Azure      |
| `AZURE_SPEECH_REGION`            | Azure region (e.g., eastus)                      | Azure      |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to GCP service account JSON                 | Google     |
| `ELEVENLABS_API_KEY`             | ElevenLabs API key                               | ElevenLabs |
| `COQUI_MODEL_PATH`               | Path to local Coqui TTS model                    | Coqui      |


## API Endpoints

### POST /tts/synthesize

Synthesize text to speech.

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

### POST /tts/stream

Stream synthesized speech in real-time.

### GET /tts/voices

List available voices for the current provider.

### GET /tts/providers

List all available TTS providers.

## Usage Examples

### Python Client

```python
import httpx

# Sync request
response = httpx.post(
    "http://localhost:8013/tts/synthesize",
    json={
        "text": "Hello from the AI Test platform!",
        "voice": "en-US-JennyNeural",
        "speed": 1.0
    }
)

with open("output.mp3", "wb") as f:
    f.write(response.content)
```

### Streaming

```python
import httpx

async with httpx.AsyncClient(timeout=60.0) as client:
    async with client.stream(
        "POST",
        "http://localhost:8013/tts/stream",
        json={"text": "Streaming synthesis demo"}
    ) as response:
        async for chunk in response.aiter_bytes(chunk_size=8192):
            # Process audio chunks
            audio_buffer.write(chunk)
```

### Direct Provider Usage

```python
from services.tts_service.providers import get_provider

# Get configured provider
provider = get_provider("azure")

# Synthesize
audio = provider.synthesize(
    text="Hello!",
    voice="en-US-JennyNeural"
)

# Stream
for chunk in provider.stream("Hello!"):
    # Process chunk
    pass

# List voices
voices = provider.list_voices()
```

