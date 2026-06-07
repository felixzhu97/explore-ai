# Quick Start

5 分钟启动 AI 服务。

## 前置要求

- Node.js >= 20
- pnpm >= 9
- Python >= 3.9

## 服务列表


| 服务             | 端口   | 说明                         |
| -------------- | ---- | -------------------------- |
| Vision Service | 8000 | 图像识别 (YOLO, BLIP, OCR)     |
| RAG Service    | 8001 | 文档问答、检索增强生成                |
| TTS Service    | 8005 | 语音合成                       |
| Text Service   | 8006 | 文本生成 (GPT, Claude, Ollama) |
| AI Agents      | 8003 | 多智能体编排                     |
| Media Gen      | 3456 | 本地 Stable Diffusion        |


## 启动方式

### 一键启动 (推荐)

```bash
pnpm install
pnpm start
```

### 手动启动

```bash
# Vision Service
cd services/vision-service
pip install -r requirements.txt
uvicorn src.main:app --port 8000

# RAG Service
cd services/rag
pip install -r requirements.txt
uvicorn src.main:app --port 8001

# TTS Service
cd services/tts-service
pip install -r requirements.txt
uvicorn src.main:app --port 8005

# Text Service
cd services/text-service
pip install -r requirements.txt
uvicorn src.main:app --port 8006

# AI Agents
cd services/ai_agents
pip install -r requirements.txt
uvicorn main:app --port 8003

# Media Gen
cd services/media-gen
pip install -r requirements.txt
uvicorn app:app --port 3456
```

## 环境配置

```bash
# 各服务复制环境变量文件
cd services/<service-name>
cp .env.example .env
```

## 常用 API

### 健康检查

```bash
curl http://localhost:8006/api/text/health  # Text
curl http://localhost:8005/tts/health       # TTS
curl http://localhost:8001/health           # RAG
curl http://localhost:8000/health           # Vision
curl http://localhost:8003/health           # AI Agents
curl http://localhost:3456/health           # Media Gen
```

### Text Service

```bash
# 文本补全
curl -X POST http://localhost:8006/api/text/complete \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello, world!"}'

# 对话
curl -X POST http://localhost:8006/api/text/chat \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Hi"}]}'
```

### TTS Service

```bash
# 语音合成
curl -X POST http://localhost:8005/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello!", "voice": "en-US-JennyNeural"}'
```

### RAG Service

```bash
# 上传文档
curl -X POST http://localhost:8001/api/documents/upload \
  -F "file=@document.pdf"

# 文档问答
curl -X POST http://localhost:8001/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "文档内容是什么?"}'
```

### Vision Service

```bash
# 目标检测
curl -X POST http://localhost:8000/vision/detect \
  -F "file=@image.jpg"

# 图像描述
curl -X POST http://localhost:8000/vision/caption \
  -F "file=@image.jpg"

# OCR 文字识别
curl -X POST http://localhost:8000/vision/ocr \
  -F "file=@image.jpg"
```

### Media Gen

```bash
# 生成图像
curl -X POST http://localhost:3456/image/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A beautiful sunset", "width": 512, "height": 512}'
```

## 常见问题

### 模块未找到

```bash
# 激活虚拟环境
source services/<service-name>/.venv/bin/activate
```

### GPU 未检测

```bash
# 检查 CUDA
python -c "import torch; print(torch.cuda.is_available())"
```

### 端口占用

```bash
# 使用环境变量修改端口
PORT=8080 uvicorn src.main:app --port 8080
```

## 更多信息

- [架构设计](./ARCHITECTURE.md)
- [API 参考](./API.md)
- [开发指南](./DEVELOPMENT.md)

