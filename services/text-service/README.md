# Text-to-Text LLM Service

A unified API service for text generation using multiple LLM providers.

## Features

- **Multi-Provider Support**: OpenAI (GPT), Anthropic (Claude), Ollama (Local)
- **Text Completion**: Simple prompt -> completion
- **Chat Completion**: Multi-turn conversations with session management
- **Streaming Responses**: Server-Sent Events (SSE) for real-time generation
- **Configurable Parameters**: Temperature, max tokens, system prompts

## Quick Start

### Installation

```bash
cd services/text-service
pip install -e .
```

### Configuration

Create a `.env` file:

```env
# Default Provider
LLM_PROVIDER=openai
LLM_MODEL=gpt-4o-mini

# OpenAI (optional)
OPENAI_API_KEY=sk-your-key-here
OPENAI_BASE_URL=https://api.openai.com/v1

# Anthropic (optional)
ANTHROPIC_API_KEY=sk-ant-your-key-here

# Ollama (local, optional)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b
```

### Running

```bash
# Development
uvicorn src.main:app --reload --port 8004

# Production
uvicorn src.main:app --host 0.0.0.0 --port 8004
```

## API Endpoints

### Health & Info

- `GET /api/text/health` - Health check
- `GET /api/text/providers` - List available providers
- `GET /api/text/models` - List available models

### Text Completion

- `POST /api/text/complete` - Generate completion
- `POST /api/text/complete/stream` - Stream completion

### Chat

- `POST /api/text/chat` - Generate chat response
- `POST /api/text/chat/stream` - Stream chat response
- `GET /api/text/session/{session_id}` - Get session history
- `DELETE /api/text/session/{session_id}` - Clear session

### Utilities

- `POST /api/text/reset` - Reset LLM cache

## Usage Examples

### Python Client

```python
import requests

# Text completion
response = requests.post("http://localhost:8004/api/text/complete", json={
    "prompt": "Explain quantum computing in simple terms:",
    "temperature": 0.7,
    "max_tokens": 500,
})
print(response.json()["text"])

# Chat
response = requests.post("http://localhost:8004/api/text/chat", json={
    "messages": [
        {"role": "user", "content": "Hello, how are you?"}
    ],
    "session_id": "my-session"
})
print(response.json()["text"])
```

### JavaScript/TypeScript Client

```typescript
// Streaming completion
const response = await fetch("http://localhost:8004/api/text/complete/stream", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({ prompt: "Write a haiku:" })
});

const reader = response.body?.getReader();
while (reader) {
    const { done, value } = await reader.read();
    if (done) break;
    const text = new TextDecoder().decode(value);
    console.log(text);
}
```

### cURL

```bash
# Text completion
curl -X POST http://localhost:8004/api/text/complete \
    -H "Content-Type: application/json" \
    -d '{"prompt": "What is the meaning of life?", "temperature": 0.5}'

# Streaming chat
curl -X POST http://localhost:8004/api/text/chat/stream \
    -H "Content-Type: application/json" \
    -d '{"messages": [{"role": "user", "content": "Hello"}]}'
```

## API Schemas

### CompletionRequest

```json
{
    "prompt": "string (required)",
    "system_prompt": "string (optional)",
    "provider": "string (optional: openai|anthropic|ollama)",
    "model": "string (optional)",
    "temperature": 0.7,
    "max_tokens": 4096
}
```

### ChatRequest

```json
{
    "messages": [
        {"role": "user|assistant|system", "content": "string"}
    ],
    "system_prompt": "string (optional)",
    "session_id": "string (optional)",
    "provider": "string (optional)",
    "model": "string (optional)",
    "temperature": 0.7,
    "max_tokens": 4096
}
```

## Architecture

```
services/text-service/
├── src/
│   ├── main.py           # FastAPI application
│   ├── api/
│   │   ├── routes.py     # API endpoints
│   │   └── schemas.py    # Pydantic models
│   └── core/
│       ├── config.py     # Settings
│       └── llm_gateway.py # LLM abstraction
├── tests/
├── pyproject.toml
└── README.md
```

## License

MIT
