# Local-Only Features

These modules require local infrastructure and are disabled in the cloud (`application-prod.yml` / Vercel production build).

| Module | Flag | Local setup |
|--------|------|-------------|
| Vision (ONNX) | `APP_MODULE_VISION=true` | `pnpm vision:models` then `pnpm vision:verify` |
| Audio ASR (whisper.cpp) | `APP_MODULE_AUDIO_ASR=true` | whisper.cpp on port 8178 |
| MCP Server | `APP_MODULE_MCP=true` | Spring AI MCP (local) |
| Chat Eval | `APP_MODULE_EVAL=true` | LLM-as-Judge endpoints |

## Cloud-minimal (Railway + Vercel)

Enabled by default in production:

- Chat, RAG, Tools, Image generation, TTS

Disabled in production:

- Vision ONNX, whisper ASR WebSocket, MCP, Eval

## Verify vision locally

```bash
pnpm vision:models
./gradlew bootRun
pnpm vision:verify
```

Scripts live under [`scripts/`](../scripts/).
