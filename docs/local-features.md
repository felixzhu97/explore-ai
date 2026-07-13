# Local-Only Features

Optional modules are controlled by LaunchDarkly feature flags (`LAUNCHDARKLY_SDK_KEY` on Railway, `LAUNCHDARKLY_CLIENT_SIDE_ID` on Vercel).

| Module | Flag Key | Local setup |
|--------|----------|-------------|
| Vision (ONNX) | `module-vision` | `pnpm vision:models` then `pnpm vision:verify` |
| Audio ASR (whisper.cpp) | `module-audio-asr` | whisper.cpp on port 8178 |
| MCP Server | `module-mcp` | Spring AI MCP (local) |
| Chat Eval | `module-eval` | LLM-as-Judge endpoints |

## Cloud-minimal (Railway + Vercel)

Production LaunchDarkly environment defaults (or YAML fallback when SDK key is absent):

- Disabled: Vision ONNX, whisper ASR WebSocket, MCP, Eval
- Enabled: Chat, RAG, Tools, Image generation, TTS

## Verify vision locally

```bash
pnpm vision:models
./gradlew bootRun
pnpm vision:verify
```

Scripts live under [`scripts/`](../scripts/).

**Note:** Toggling `module-vision` or `module-audio-asr` for heavy infrastructure (ONNX / whisper beans) requires an application restart to take effect.
