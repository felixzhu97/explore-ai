# AI Vision Service - Documentation

Welcome to the AI Vision Service documentation. This monorepo provides a full-stack AI-powered image analysis platform with object detection, image captioning, and OCR capabilities.

## Documentation Index

| Document | Description |
|----------|-------------|
| [Quick Start](./QUICKSTART.md) | Get up and running in 5 minutes |
| [Architecture](./ARCHITECTURE.md) | System design and component overview |
| [API Reference](./API.md) | AI service REST API endpoints |
| [Development](./DEVELOPMENT.md) | Local development setup and workflow |

## Project Overview

The AI Vision Service is a TypeScript/Python monorepo that combines:

- **React Frontend** (`apps/web`) - User interface for image upload and results display
- **Express.js Server** (`apps/server`) - Backend utility endpoints
- **Python AI Service** (`services/vision-service`) - FastAPI-based vision AI with YOLO, BLIP, and PaddleOCR

## Features

| Feature | Model | Use Case |
|---------|-------|----------|
| Object Detection | YOLO11n | Identify and locate objects in images |
| Image Captioning | BLIP | Generate natural language descriptions |
| OCR | PaddleOCR | Extract text from images |
| Combined Analysis | All models | Run all tasks on a single image |

## Tech Stack

### Frontend
- React 18
- Vite
- TypeScript

### Backend
- Node.js / Express.js
- Python / FastAPI

### AI Models
- [Ultralytics YOLO](https://github.com/ultralytics/ultralytics)
- [HuggingFace BLIP](https://huggingface.co/Salesforce/blip-image-captioning-large)
- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)

## Quick Links

- [GitHub Repository](https://github.com)
- [API Documentation](./API.md)
- [Development Guide](./DEVELOPMENT.md)
