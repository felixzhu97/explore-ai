# AI-Explore

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-blue.svg)](https://docs.spring.io/spring-ai/reference/)

Demo / learning platform for conversational AI built with **Spring AI** and **Angular**: Chat, RAG, tools, vision, audio, and quality evaluation.

**Live:** [https://www.felixzhu.chat](https://www.felixzhu.chat)

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Usage](#usage)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Deployment](#deployment)
- [Documentation](#documentation)
- [License](#license)

## Features

| Area | Capability |
|------|------------|
| **Chat** | Multi-provider LLM, SSE streaming, session-friendly UX |
| **RAG** | Document upload, vector retrieval, optional multimodal (Ollama) |
| **Tools** | Weather, web search (Serper), datetime, and related tool calling |
| **Eval** | LLM-as-a-Judge; **Golden Suite** regression (Chat + RAG, test-only) |
| **Vision** | Local caption / detect / OCR (ONNX + Tesseract) |
| **Audio / Image** | TTS, whisper.cpp ASR, image generation |
| **Ops** | Actuator health, optional Datadog RUM, Render + Vercel deploy |

## Tech Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 25, Spring Boot 4.1, Spring AI 2.0 |
| Frontend | Angular 22, TypeScript, pnpm |
| Data | H2 (embedded) + Liquibase |
| Local AI | Ollama (embed / multimodal), whisper.cpp, ONNX models |

Architecture follows hexagonal-style layers: `web → application → domain ← infrastructure`.

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 25+ |
| Node.js | 20+ |
| pnpm | 8+ |
| Git | latest |

Optional: [Ollama](https://ollama.com/), [Tesseract](https://github.com/tesseract-ocr/tesseract), whisper.cpp — only if you use the matching features.

## Getting Started

```bash
git clone https://github.com/felixzhu97/explore-ai.git
cd explore-ai
```

### 1. Environment

```bash
cat > .env << EOF
DEEPSEEK_API_KEY=your-deepseek-key
OPENAI_API_KEY=your-openai-key      # optional
SERPER_API_KEY=your-serper-key      # optional (web search)
EOF
```

Load before running:

```bash
set -a && source .env && set +a
```

### 2. Backend

```bash
./gradlew bootRun
# → http://localhost:9000
curl -s http://localhost:9000/actuator/health
```

### 3. Frontend

```bash
cd src/main/web
pnpm install
pnpm start
# → http://localhost:4200
```

More detail: [docs/developer/QUICKSTART.md](docs/developer/QUICKSTART.md).

## Configuration

| Variable | Required | Purpose |
|----------|----------|---------|
| `DEEPSEEK_API_KEY` | Yes (default path) | Primary chat / eval models |
| `OPENAI_API_KEY` | No | OpenAI-compatible features |
| `SERPER_API_KEY` | No | Web search tool |
| `H2_URL` | No | Override DB URL (default `./data/explore-ai`) |
| `GOLDEN_EVAL_IT` | No | Set `true` to enable live Golden Eval IT |
| `VISION_MODELS_READY` | No | Set `true` when local vision models are downloaded |

Defaults live in `src/main/resources/application.yml`. Do not commit real secrets.

## Usage

Minimal API smoke check (backend must be running):

```bash
curl -X POST http://localhost:9000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

Full HTTP / SSE / WebSocket reference: [docs/developer/api.md](docs/developer/api.md).

### Vision (optional)

```bash
pnpm vision:models && pnpm vision:fixtures
./gradlew bootRun
pnpm vision:verify
```

## Testing

### Unit / default CI

```bash
./gradlew test
```

Default runs do **not** call paid LLM APIs for Golden Eval.

### Golden Eval (live quality regression)

Runs OpenAI Evals–style JSONL cases against **real** Chat / RAG generation, then scores with Spring AI official [RelevancyEvaluator / FactCheckingEvaluator](https://docs.spring.io/spring-ai/reference/api/testing.html).

| Item | Path |
|------|------|
| Suites | `src/main/resources/eval/golden/chat.jsonl`, `rag.jsonl` |
| RAG fixtures | `src/main/resources/eval/golden/fixtures/` |
| IT | `src/test/java/com/ai/eval/GoldenEvalIT.java` |
| Orchestration | `GoldenEvalUseCase` (no public REST; test-only) |

```bash
set -a && source .env && set +a

# Separate H2 file so bootRun does not lock the default DB
export H2_URL='jdbc:h2:file:./data/explore-ai-golden;AUTO_SERVER=TRUE'
export GOLDEN_EVAL_IT=true

./gradlew test --tests 'com.ai.eval.GoldenEvalIT' --rerun-tasks
```

**Where to read the report**

Stdout summary (also embedded in Gradle reports):

```text
Golden RAG: total=3 passed=3 failed=0 passRate=1.00
Golden CHAT: total=4 passed=1 failed=3 passRate=0.25
```

| Artifact | Location |
|----------|----------|
| JUnit XML | `build/test-results/test/TEST-com.ai.eval.GoldenEvalIT.xml` |
| HTML | `build/reports/tests/test/index.html` → class `GoldenEvalIT` |

**How to interpret**

- Pass rate = share of cases where relevancy passes, and factuality passes when context is present (quality gate — not “intent accuracy”).
- RAG usually tracks retrieval grounding; Chat cases with `metadata.contexts` fact-check against that context **without** injecting it into generation — low Chat pass rate can be an expected product-knowledge signal.
- The IT asserts a valid report shape; it does **not** require `passRate == 1.0`.

Extend suites by appending JSONL lines (`id`, `input`, `ideal`, `metadata`). See [OpenAI Evals](https://github.com/openai/evals) format and Spring AI [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html).

### Vision IT

```bash
VISION_MODELS_READY=true ./gradlew test --tests com.ai.vision.VisionFunctionalVerificationIT
```

## Project Structure

```
explore-ai/
├── src/main/java/com/ai/{module}/   # Business modules (flat layers)
│   ├── web/                         # Controllers + Request/Response
│   ├── application/                 # UseCases
│   ├── domain/                      # Entities, VOs, Repository interfaces
│   └── infrastructure/              # Adapters (shallow)
├── src/main/web/                    # Angular app
├── src/main/resources/
│   ├── application.yml
│   └── eval/golden/                 # Golden Suite JSONL + fixtures
├── docs/                            # API, C4, Glossary, user stories
├── render.yaml                      # Render Blueprint
└── vercel.json                      # Frontend + /api proxy
```

## Deployment

| Target | Role |
|--------|------|
| [Render Free](https://render.com/docs/free) | Backend via [`render.yaml`](render.yaml); health: `/actuator/health` |
| [Vercel](https://vercel.com) | Frontend; [`vercel.json`](vercel.json) rewrites `/api/*` to Render |

Notes for Free tier: idle sleep, no durable disk, keep Datadog javaagent off (RAM). Optional RUM on Vercel: `DD_APPLICATION_ID`, `DD_CLIENT_TOKEN`.

## Documentation

| Doc | Link |
|-----|------|
| Quick start | [docs/developer/QUICKSTART.md](docs/developer/QUICKSTART.md) |
| API | [docs/developer/api.md](docs/developer/api.md) |
| C4 model | [docs/developer/c4-model/](docs/developer/c4-model/) |
| Glossary | [docs/Glossary.md](docs/Glossary.md) |
| User story map | [docs/product-owner/User-Story-Map.md](docs/product-owner/User-Story-Map.md) |

![C1 Context](docs/developer/c4-model/png/C1-Context.png)

![C2 Container](docs/developer/c4-model/png/C2-Container.png)

## License

[MIT](LICENSE) © 2026 Felix
