# AI-Explore

基于 Spring AI + Angular 的 AI 应用平台，支持 RAG 文档问答、Tool Calling、图像生成、语音合成和视觉分析。

## 核心功能

| 功能 | 描述 | 技术亮点 |
|------|------|---------|
| **AI 对话** | 多 Provider (OpenAI/Anthropic/Ollama) 切换，SSE 流式输出 | Markdown 渲染，会话管理 |
| **RAG 文档问答** | PDF/TXT 文档上传，向量检索增强生成 | 流式响应，来源引用，**本地 Ollama 视觉理解** |
| **Tool Calling** | 天气查询、文档搜索、Web 搜索 | 自动工具选择 |
| **图像生成** | DALL-E/FLUX 图像生成 | 多尺寸支持 |
| **语音合成 (TTS)** | 多语言多音色，语速调节 | 实时预览，下载 MP3 |
| **视觉分析** | 图像描述、物体检测、OCR 文字识别 | 拖拽上传，图片缩放，**本地 Ollama qwen3.5 驱动** |

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端 | Java 25 + Spring Boot 4.1 |
| AI | Spring AI 2.0 (DeepSeek / OpenAI / Ollama) |
| 本地视觉 | Ollama qwen3.5:35b (开源多模态模型) |
| 本地 Embedding | Ollama nomic-embed-text (768 维) |
| 前端 | Angular 22 + TypeScript |
| 数据库 | H2 嵌入式 + Liquibase |
| 部署 | Docker Compose (可选) |

## 快速启动

```bash
# 1. 配置 API Keys
cat > .env << EOF
DEEPSEEK_API_KEY=your-deepseek-key
OPENAI_API_KEY=your-openai-key
SERPER_API_KEY=your-serper-key   # 可选，Web 搜索用
EOF

# 2. 启动后端 (H2 自动建表)
./gradlew bootRun

# 3. 新终端启动前端
cd src/main/web && pnpm install && pnpm start
```

访问 http://localhost:4200

## API 示例

### AI 对话

```bash
curl -X POST http://localhost:9000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

### RAG 文档问答

```bash
# 上传文档
curl -X POST http://localhost:9000/api/rag/documents/upload \
  -F "file=@manual.pdf" -F "title=用户手册"

# 流式问答
curl -X POST http://localhost:9000/api/rag/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"question": "产品的保修期是多久？"}'

# 多模态问答（带图片）
curl -X POST http://localhost:9000/api/rag/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"question": "这张图表说明了什么？", "images": ["data:image/png;base64,iVBORw0KG..."]}'
```

### Tool Calling (天气 + Web 搜索)

```bash
curl -X POST http://localhost:9000/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "今天北京的天气怎么样？"}'

# 需要实时信息时自动调用 Web 搜索
curl -X POST http://localhost:9000/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the latest AI news today?"}'
```

### 图像生成

```bash
curl -X POST http://localhost:9000/api/images/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A beautiful sunset over the ocean", "width": 1024, "height": 1024}'
```

### 语音合成

```bash
curl -X POST http://localhost:9000/api/audio/speak \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello, welcome to AI Explore!", "voice": "en-US"}' \
  --output speech.mp3
```

## 项目结构

```
ai-explore/
├── src/main/java/com/ai/
│   ├── chat/           # AI 对话
│   ├── rag/            # RAG 文档问答
│   ├── tools/          # Tool Calling (天气/搜索)
│   ├── image/          # 图像生成
│   ├── audio/          # 语音合成
│   └── mcp/            # MCP Server/Client
│
├── src/main/web/       # Angular 前端
│   └── app/
│       ├── rag/        # RAG 页面
│       ├── vision/     # 视觉分析
│       └── ai-hub/     # AI Hub (对话/TTS/图像)
│
└── docs/c4/           # C4 架构图
```

## 文档

- [API 文档](./docs/api.md)
- [C4 架构图](./docs/c4/)
- [快速入门](./docs/QUICKSTART.md)
