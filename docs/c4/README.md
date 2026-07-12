# C4 模型文档

使用 PlantUML 绘制的 C4 架构模型，描述 AI Chat & Agent Platform 的完整架构。

## 文件


| 文件                           | 层级  | 说明                                                 |
| ---------------------------- | --- | -------------------------------------------------- |
| `C1-Context.puml`            | C1  | 系统上下文图                                             |
| `C2-Container.puml`          | C2  | 容器图（11 个子域）                                        |
| `C3-Component-Backend.puml`  | C3  | 后端组件图（Web / Application / Domain / Infrastructure） |
| `C3-Component-Frontend.puml` | C3  | 前端组件图（App / Pages / Services / Shared）             |
| `C4-Deployment.puml`         | C4  | 本地开发环境部署图（端口 9000）                                 |


---

## C1 - 系统上下文图

C1-Context

---

## C2 - 容器图

C2-Container

---

## C3 - 后端组件图

C3-Component-Backend

---

## C3 - 前端组件图

C3-Component-Frontend

---

## C4 - 部署图

C4-Deployment

---

## 技术栈

### 后端 (AI Platform Backend)

- **运行时**: Spring Boot 4.0 / Java 25 / Spring AI 2.0
- **端口**: **9000** (统一)
- **子域**: Chat / RAG / RAG ETL / Tools / Analysis / Eval / Image / **Vision** / Audio (TTS+ASR) / MCP Server / MCP Client
- **向量库**: H2 嵌入式向量 (默认) / PostgreSQL + pgvector (可选)
- **外部服务**: DeepSeek API (LLM) / Ollama (本地 Embedding + 多模态 RAG) / whisper.cpp (本地 ASR) / DALL-E API (图像生成) / TTS Service (语音合成) / Serper.dev (Web 搜索) / Tesseract (本地 OCR)

### Vision 子域（图像分析）

独立 `/vision` 页面，**不经过 Ollama**：


| 能力      | API                        | 适配器                 | 依赖                             |
| ------- | -------------------------- | ------------------- | ------------------------------ |
| Caption | `POST /api/vision/caption` | `OnnxBlipCaptioner` | `models/blip_*.onnx`           |
| Detect  | `POST /api/vision/detect`  | `OnnxYoloDetector`  | `models/yolov8n.onnx`          |
| OCR     | `POST /api/vision/ocr`     | `Tess4jOcrEngine`   | Tesseract + `models/tessdata/` |
| Health  | `GET /api/vision/health`   | —                   | 各 Provider 就绪状态                |


> **区分**: **Image Analysis** (`/vision`, `/api/vision/`*) vs **Vision Chat** (RAG 流式多模态，Ollama qwen3.5)

### 前端 (Web Frontend)

- **框架**: Angular 22 + TypeScript
- **路由**: `/chat` (AI Hub) / `/generate` (TTS/图像) / `/rag` / `/vision`
- **端口**: 4200 (dev server, proxy `/api` → `:9000`)

## 部署端口汇总


| 服务                            | 端口             |
| ----------------------------- | -------------- |
| Spring Boot Backend           | **9000**       |
| H2 Embedded                   | 内嵌 (默认)        |
| PostgreSQL + pgvector         | 5432 (可选)      |
| Ollama (Embedding/RAG Vision) | 11434          |
| whisper.cpp (ASR)             | 8178           |
| Tesseract OCR                 | 系统安装 (JNA)     |
| Vision ONNX Models            | `models/` 本地文件 |
| Angular Dev Server            | 4200           |
| DeepSeek API                  | HTTPS          |
| DALL-E API                    | HTTPS          |
| TTS Service                   | HTTPS          |
| Serper.dev API                | HTTPS          |


## 模型配置


| 用途                | 模型                | 维度/说明              |
| ----------------- | ----------------- | ------------------ |
| LLM Chat          | deepseek-v4-flash | DeepSeek API       |
| Embedding         | mxbai-embed-large | 1024 维 (Ollama)    |
| Vision Chat (RAG) | qwen3.5           | Ollama 多模态对话       |
| Caption           | BLIP base ONNX    | ONNX Runtime 本地    |
| Detect            | YOLOv8n ONNX      | COCO 80 类          |
| OCR               | eng + chi_sim     | Tesseract tessdata |
| ASR               | whisper-base      | whisper.cpp 本地     |
| Image Gen         | dall-e-3          | OpenAI API         |
| TTS               | gpt-4o-mini-tts   | OpenAI API         |


## 查看与更新

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- 重新生成 PNG：

```bash
cd docs/c4 && plantuml -o png *.puml
```

## 相关文档

- [领域术语表](../Domain-Glossary.md) — Ubiquitous Language 与代码映射
- [沃德利地图](../Wardley-Map.md)
- [用户故事地图](../User-Story-Map.md)
- [API 文档](../api.md)

