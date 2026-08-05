# AI-Explore

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-blue.svg)](https://docs.spring.io/spring-ai/reference/)

Demo / learning platform for conversational AI built with **Spring AI** and **Angular**: Chat, RAG, tool calling, and quality evaluation.

**Live:** [https://www.felixzhu.chat](https://www.felixzhu.chat)

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Testing](#testing)
- [Documentation](#documentation)
- [Deployment](#deployment)
- [License](#license)

## Features

| Area | Capability |
|------|------------|
| **Chat** | Multi-provider LLM, SSE streaming, session-friendly UX |
| **RAG** | Document upload, vector retrieval (local Ollama embeddings) |
| **Tool Calling** | Weather, web search (Serper), datetime, and related tools |
| **Eval** | LLM-as-a-Judge; Golden Suite regression (Chat + RAG, test-only) |

Optional modules (vision, audio, image generation, MCP, agents, metrics, and more) are documented in the [User Story Map](docs/product-owner/User-Story-Map.md) and [Quick Start](docs/developer/QUICKSTART.md).

## Tech Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 25, Spring Boot 4.1, Spring AI 2.0 |
| Frontend | Angular 22, TypeScript, pnpm |
| Data | H2 (embedded) + Liquibase |
| Local AI | Ollama (`qwen3-embedding:0.6b` for RAG) |

Architecture: `web → application → domain ← infrastructure`. See [C4 model](docs/developer/c4-model/) and [Glossary](docs/Glossary.md).

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 25+ |
| Node.js | 20+ |
| pnpm | 8+ |
| Git | latest |

Optional for RAG: [Ollama](https://ollama.com/) and `ollama pull qwen3-embedding:0.6b` (re-ingest documents after changing embedding models).

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

```bash
set -a && source .env && set +a
```

### 2. Backend

```bash
./gradlew bootRun
# → http://localhost:9000
curl -s http://localhost:9000/actuator/health
```

Smoke check:

```bash
curl -X POST http://localhost:9000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
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
| `GOLDEN_EVAL_IT` | No | Set `true` for live Golden Eval IT ([guide](docs/developer/golden-eval.md)) |

Defaults live in `src/main/resources/application.yml`. Do not commit real secrets.

## Testing

```bash
./gradlew test
```

Live Golden Eval (paid/local LLM calls): see [docs/developer/golden-eval.md](docs/developer/golden-eval.md).

## Documentation

| Doc | Link |
|-----|------|
| Quick start | [docs/developer/QUICKSTART.md](docs/developer/QUICKSTART.md) |
| Golden Eval | [docs/developer/golden-eval.md](docs/developer/golden-eval.md) |
| API | [docs/developer/api.md](docs/developer/api.md) |
| C4 model | [docs/developer/c4-model/](docs/developer/c4-model/) |
| Glossary | [docs/Glossary.md](docs/Glossary.md) |
| User story map | [docs/product-owner/User-Story-Map.md](docs/product-owner/User-Story-Map.md) |

![C1 Context](docs/developer/c4-model/png/C1-Context.png)

## Deployment

| Target | Role |
|--------|------|
| [Render Free](https://render.com/docs/free) | Backend via [`render.yaml`](render.yaml); health: `/actuator/health` |
| [Vercel](https://vercel.com) | Frontend; [`vercel.json`](vercel.json) rewrites `/api/*` to Render |

Free tier notes: idle sleep, no durable disk, keep Datadog javaagent off (RAM). Optional RUM on Vercel: `DD_APPLICATION_ID`, `DD_CLIENT_TOKEN`.

## License

[MIT](LICENSE) © 2026 Felix
