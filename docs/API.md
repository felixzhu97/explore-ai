# API Reference

REST API documentation for the AI Vision Service.

## Base URL

```
http://localhost:8000
```

## Endpoints Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `GET` | `/` | Service info |
| `POST` | `/vision/detect` | Object detection |
| `POST` | `/vision/caption` | Image captioning |
| `POST` | `/vision/ocr` | Text extraction (OCR) |
| `POST` | `/vision/analyze` | Combined analysis |

---

## Health Check

### `GET /health`

Check if the service is running.

**Response:**

```json
{
  "status": "ok"
}
```

**Example:**

```bash
curl http://localhost:8000/health
```

---

## Service Info

### `GET /`

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

## Object Detection

### `POST /vision/detect`

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

| Field | Type | Description |
|-------|------|-------------|
| `task` | string | Always `"detect_objects"` |
| `model` | string | YOLO model name |
| `detections` | array | List of detected objects |
| `detections[].class_name` | string | Object class label |
| `detections[].confidence` | float | Confidence score (0-1) |
| `detections[].bbox` | array | Bounding box [x1, y1, x2, y2] |
| `image_width` | int | Image width in pixels |
| `image_height` | int | Image height in pixels |
| `processing_time_ms` | float | Processing time in milliseconds |

**Example:**

```bash
curl -X POST http://localhost:8000/vision/detect \
  -F "file=@image.jpg" \
  -F "conf=0.5"
```

---

## Image Captioning

### `POST /vision/caption`

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

| Field | Type | Description |
|-------|------|-------------|
| `task` | string | Always `"caption_image"` |
| `model` | string | BLIP model name |
| `caption` | string | Generated caption |
| `processing_time_ms` | float | Processing time in milliseconds |

**Example:**

```bash
curl -X POST http://localhost:8000/vision/caption \
  -F "file=@image.jpg"
```

---

## OCR (Text Extraction)

### `POST /vision/ocr`

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

| Field | Type | Description |
|-------|------|-------------|
| `task` | string | Always `"extract_text"` |
| `model` | string | Always `"PaddleOCR"` |
| `results` | array | List of detected text blocks |
| `results[].text` | string | Extracted text |
| `results[].confidence` | float | Confidence score (0-1) |
| `results[].bbox` | array | Bounding box [[x1,y1], [x2,y2], [x3,y3], [x4,y4]] |
| `full_text` | string | All extracted text joined with newlines |
| `processing_time_ms` | float | Processing time in milliseconds |

**Example:**

```bash
curl -X POST http://localhost:8000/vision/ocr \
  -F "file=@document.jpg"
```

---

## Combined Analysis

### `POST /vision/analyze`

Run multiple AI tasks on a single image.

**Request:**

- **Content-Type:** `multipart/form-data`
- **Body:**
  - `file` (required): Image file (JPEG, PNG, etc.)
  - `task` (required): Task type (query parameter)

**Task Types:**

| Task | Description | Response Keys |
|------|-------------|---------------|
| `caption_image` | Generate caption | `caption` |
| `detect_objects` | Detect objects | `detections` |
| `extract_text` | Extract text | `results` |
| `analyze_image` | Run all tasks | `caption`, `detections`, `ocr` |

**Example - Caption Only:**

```bash
curl -X POST "http://localhost:8000/vision/analyze?task=caption_image" \
  -F "file=@image.jpg"
```

**Response:**

```json
{
  "task": "caption_image",
  "model": "Salesforce/blip-image-captioning-large",
  "caption": "A beautiful sunset over the ocean",
  "processing_time_ms": 245.3
}
```

**Example - Object Detection:**

```bash
curl -X POST "http://localhost:8000/vision/analyze?task=detect_objects" \
  -F "file=@image.jpg"
```

**Response:**

```json
{
  "task": "detect_objects",
  "model": "yolo11n.pt",
  "detections": [...],
  "image_width": 800,
  "image_height": 600,
  "processing_time_ms": 48.1
}
```

**Example - Full Analysis:**

```bash
curl -X POST "http://localhost:8000/vision/analyze?task=analyze_image" \
  -F "file=@image.jpg"
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

---

## Rate Limits

- **Concurrent Requests:** 4 (configurable via `MAX_CONCURRENT_REQUESTS`)
- **Max File Size:** 10MB (configurable via `MAX_IMAGE_SIZE`)

---

## Client Examples

### Python with `requests`

```python
import requests

# Object Detection
with open("image.jpg", "rb") as f:
    response = requests.post(
        "http://localhost:8000/vision/detect",
        files={"file": f}
    )
print(response.json())
```

### JavaScript with `fetch`

```javascript
const formData = new FormData();
formData.append("file", imageFile);

const response = await fetch("http://localhost:8000/vision/caption", {
  method: "POST",
  body: formData,
});

const data = await response.json();
console.log(data.caption);
```

### cURL

```bash
# Caption
curl -X POST http://localhost:8000/vision/caption \
  -F "file=@photo.jpg"

# Detection with confidence
curl -X POST http://localhost:8000/vision/detect \
  -F "file=@photo.jpg" \
  -F "conf=0.5"

# OCR
curl -X POST http://localhost:8000/vision/ocr \
  -F "file=@document.jpg"

# Full analysis
curl -X POST "http://localhost:8000/vision/analyze?task=analyze_image" \
  -F "file=@photo.jpg"
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DEVICE` | `cuda` | Computation device (`cuda` or `cpu`) |
| `YOLO_MODEL` | `yolo11n.pt` | YOLO model path |
| `BLIP_MODEL` | `Salesforce/blip-image-captioning-large` | BLIP model name |
| `OCR_LANG` | `ch` | PaddleOCR language |
| `MAX_IMAGE_SIZE` | `10485760` | Max file size in bytes (10MB) |
| `MODEL_CACHE_DIR` | `./models` | Model cache directory |
| `MAX_CONCURRENT_REQUESTS` | `4` | Max concurrent requests |
