# Media Generation Service

Local Text-to-Image generation service using Stable Diffusion.

## Quick Start

```bash
cd services/media-gen
cp .env.example .env

# Install dependencies
pip install -r requirements.txt

# Run the service
python app.py
```

## Configuration


| Variable           | Default                          | Description                          |
| ------------------ | -------------------------------- | ------------------------------------ |
| `MEDIA_GEN_PORT`   | `8015`                            | Service port                         |
| `SD_MODEL`         | `runwayml/stable-diffusion-v1-5` | HuggingFace model ID                 |
| `MEDIA_GEN_DEVICE` | `auto`                           | Device: `auto`, `cpu`, `cuda`, `mps` |


## API Endpoints

### Health Check

```
GET /health
```

### Generate Image

```
POST /image/generate
Content-Type: application/json

{
  "prompt": "A beautiful sunset over mountains",
  "negative_prompt": "blurry, low quality",
  "width": 512,
  "height": 512,
  "num_inference_steps": 25,
  "guidance_scale": 7.5,
  "seed": 42,
  "num_images": 1
}
```

### Clear Cache

```
POST /cache/clear
```

## Usage Example

```bash
# Health check
curl http://localhost:8015/health

# Generate image
curl -X POST http://localhost:8015/image/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A cat sitting on a windowsill", "width": 512, "height": 512}'
```

