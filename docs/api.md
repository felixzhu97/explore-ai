# AI Service API Documentation

## Base URL

```bash
BASE_URL="http://localhost:9000"
```

---

## Table of Contents

1. [Health Check](#health-check)
2. [Chat API](#chat-api)
3. [Session API](#session-api)
4. [RAG API](#rag-api)
5. [Tool Calling API](#tool-calling-api)
6. [Image Generation API](#image-generation-api)
7. [Audio/TTS API](#audio-tts-api)
8. [Audio/ASR API](#audio-asr-api)
9. [MCP Server API](#mcp-server-api)
10. [MCP Client API](#mcp-client-api)
11. [Chat Evaluation API](#chat-evaluation-api)
12. [Error Codes](#error-codes)

---

## Health Check

### Health Check

Check if the service is running properly.

```bash
curl -X GET "${BASE_URL}/api/health"
```

**Response Example**

```json
{
  "status": "UP"
}
```

---

## Chat API

### Chat with AI

Send a chat message and receive an AI response. Supports session context for multi-turn conversations.

```bash
curl -X POST "${BASE_URL}/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, please introduce the history of Beijing"
  }'
```

**Request Body**


| Field       | Type   | Required | Description                             |
| ----------- | ------ | -------- | --------------------------------------- |
| `message`   | string | Yes      | Chat message (max 10,000 characters)    |
| `sessionId` | string | No       | Session ID for multi-turn conversations |


**Response Example**

```json
{
  "response": "Beijing is the capital of China with over 3,000 years of history. It served as the capital for multiple dynasties and is home to world cultural heritage sites like the Forbidden City, Temple of Heaven, and Great Wall.",
  "sessionId": "abc123",
  "messageId": "msg-uuid",
  "timestamp": "2026-06-19T03:30:00Z"
}
```

---

### Continue Session Chat

Continue a previous conversation using a session ID.

```bash
curl -X POST "${BASE_URL}/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Please tell me more about the history of the Forbidden City",
    "sessionId": "abc123"
  }'
```

---

### Simple Chat (Legacy Endpoint)

Simple chat without session context (legacy API).

```bash
curl -X POST "${BASE_URL}/api/chat/simple" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello!"
  }'
```

**Response Example**

```json
{
  "response": "Hello! I'm happy to assist you. How can I help you today?"
}
```

---

### Text Analysis

Analyze text and return structured results including summary, sentiment, key points, and entity recognition.

```bash
curl -X POST "${BASE_URL}/api/chat/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "The food at this restaurant was delicious, the service was excellent, and the atmosphere was elegant. It was a very enjoyable dining experience.",
    "language": "en"
  }'
```

**Request Body**


| Field      | Type   | Required | Description                          |
| ---------- | ------ | -------- | ------------------------------------ |
| `text`     | string | Yes      | Text to analyze                      |
| `language` | string | No       | Analysis language (e.g., "en", "zh") |


**Response Example**

```json
{
  "summary": "This is a positive review about a restaurant dining experience.",
  "sentiment": "POSITIVE",
  "keyPoints": [
    "Delicious food",
    "Excellent service",
    "Elegant atmosphere"
  ],
  "entities": [
    "restaurant"
  ],
  "language": "en"
}
```

**Sentiment Values**


| Value      | Description        |
| ---------- | ------------------ |
| `POSITIVE` | Positive sentiment |
| `NEUTRAL`  | Neutral sentiment  |
| `NEGATIVE` | Negative sentiment |


---

## Session API

### Create Session

Create a new chat session.

```bash
curl -X POST "${BASE_URL}/api/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Travel Consultation"
  }'
```

**Request Body**


| Field   | Type   | Required | Description                            |
| ------- | ------ | -------- | -------------------------------------- |
| `title` | string | No       | Session title (default: "New Session") |


**Response Example**

```json
{
  "sessionId": "abc123",
  "title": "Travel Consultation",
  "messageCount": 0,
  "createdAt": "2026-06-19T03:30:00Z",
  "lastActivityAt": "2026-06-19T03:30:00Z"
}
```

---

### Get All Sessions

Get a list of all chat sessions.

```bash
curl -X GET "${BASE_URL}/api/sessions"
```

**Response Example**

```json
[
  {
    "sessionId": "abc123",
    "title": "Travel Consultation",
    "messageCount": 5,
    "createdAt": "2026-06-19T03:30:00Z",
    "lastActivityAt": "2026-06-19T04:00:00Z"
  }
]
```

---

### Get Session Messages

Get message history for a specific session.

```bash
curl -X GET "${BASE_URL}/api/sessions/{sessionId}/messages"
```

**Path Parameters**


| Parameter   | Type   | Description |
| ----------- | ------ | ----------- |
| `sessionId` | string | Session ID  |


**Response Example**

```json
{
  "sessionId": "abc123",
  "messages": [
    {
      "id": "msg-uuid-1",
      "text": "Hello, please introduce the attractions in Hangzhou",
      "role": "user",
      "timestamp": "2026-06-19T03:30:00Z"
    },
    {
      "id": "msg-uuid-2",
      "text": "Hangzhou is the capital of Zhejiang Province. West Lake is the most famous attraction, along with Lingyin Temple, Songcheng, and other notable tourist destinations.",
      "role": "assistant",
      "timestamp": "2026-06-19T03:30:01Z"
    }
  ],
  "totalCount": 2
}
```

---

### Delete Session

Delete a chat session and all its messages.

```bash
curl -X DELETE "${BASE_URL}/api/sessions/{sessionId}"
```

**Path Parameters**


| Parameter   | Type   | Description |
| ----------- | ------ | ----------- |
| `sessionId` | string | Session ID  |


**Response**

`204 No Content`

---

## RAG API

### Get Document List

Get all documents in the knowledge base.

```bash
curl -X GET "${BASE_URL}/api/rag/documents"
```

**Response Example**

```json
{
  "documents": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Product User Manual",
      "status": "PROCESSED",
      "createdAt": "2026-06-19T03:30:00Z",
      "chunkCount": 15
    }
  ]
}
```

---

### Upload Document

Upload a document to the knowledge base. Supports TXT and PDF files.

```bash
curl -X POST "${BASE_URL}/api/rag/documents/upload" \
  -F "file=@user_manual.txt" \
  -F "title=Product User Manual"
```

**Form Parameters**


| Parameter | Type   | Required | Description                        |
| --------- | ------ | -------- | ---------------------------------- |
| `file`    | file   | Yes      | File to upload (TXT or PDF)        |
| `title`   | string | No       | Document title (default: filename) |


**Response Example**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Product User Manual",
  "status": "PROCESSED",
  "chunkCount": 15,
  "createdAt": "2026-06-19T03:30:00Z"
}
```

---

### Upload PDF Document

```bash
curl -X POST "${BASE_URL}/api/rag/documents/upload" \
  -F "file=@technical_document.pdf" \
  -F "title=Technical Documentation"
```

---

### Delete Document

Delete a document from the knowledge base.

```bash
curl -X DELETE "${BASE_URL}/api/rag/documents/{id}"
```

**Path Parameters**


| Parameter | Type | Description |
| --------- | ---- | ----------- |
| `id`      | UUID | Document ID |


**Response**

`204 No Content`

---

### RAG Chat (Streaming)

AI chat based on document retrieval (streaming response).

```bash
curl -X POST "${BASE_URL}/api/rag/chat/stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "question": "What is the warranty period for this product?",
    "docIds": [],
    "topK": 5
  }'
```

**Request Body**


| Field         | Type    | Required | Description                                        |
| ------------- | ------- | -------- | -------------------------------------------------- |
| `question`    | string  | Yes      | Question text                                      |
| `docIds`      | array   | No       | Document IDs to search (empty = all)               |
| `topK`        | integer | No       | Number of document chunks to retrieve (default: 5) |
| `temperature` | number  | No       | AI temperature parameter (default: 0.7)            |
| `sessionId`   | string  | No       | Session ID                                         |


**Response Example (SSE)**

```
event: message
data: According to the product user manual,
data: this product is covered by
data: a two-year
data: full warranty.

event: sources
data: [{"text":"...warranty period is two years...","score":0.95,"metadata":{"source":"user_manual.pdf"}}]
```

---

### RAG Chat with Specific Documents

Q&A only for specific documents.

```bash
curl -X POST "${BASE_URL}/api/rag/chat/stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "question": "Summarize the main content of this document",
    "docIds": ["550e8400-e29b-41d4-a716-446655440000", "660e8400-e29b-41d4-a716-446655440001"],
    "topK": 3
  }'
```

---

## Tool Calling API

### Get Weather

Get current weather for a specified city.

```bash
curl -X GET "${BASE_URL}/api/tools/weather?city=Beijing"
```

**Query Parameters**


| Parameter | Type   | Required | Description |
| --------- | ------ | -------- | ----------- |
| `city`    | string | Yes      | City name   |


**Response Example**

```json
{
  "city": "Beijing",
  "temperature": "25°C",
  "condition": "Sunny",
  "humidity": "45%"
}
```

---

### Get Weather Forecast

Get weather forecast for a specified city.

```bash
curl -X GET "${BASE_URL}/api/tools/weather/forecast?city=Shanghai&days=7"
```

**Query Parameters**


| Parameter | Type    | Required | Description             |
| --------- | ------- | -------- | ----------------------- |
| `city`    | string  | Yes      | City name               |
| `days`    | integer | No       | Number of forecast days |


**Response Example**

```json
{
  "city": "Shanghai",
  "forecast": [
    {"day": 1, "condition": "Sunny", "high": "28°C", "low": "22°C"},
    {"day": 2, "condition": "Cloudy", "high": "26°C", "low": "20°C"},
    {"day": 3, "condition": "Light Rain", "high": "24°C", "low": "19°C"}
  ]
}
```

---

### Search Documents

Search documents in the knowledge base.

```bash
curl -X GET "${BASE_URL}/api/tools/documents/search?query=product features"
```

**Query Parameters**


| Parameter | Type   | Required | Description                                 |
| --------- | ------ | -------- | ------------------------------------------- |
| `query`   | string | Yes      | Search keyword                              |
| `docIds`  | string | No       | Comma-separated document IDs to limit scope |


**Response Example**

```json
{
  "results": [
    {
      "text": "This product has the following core features: smart recognition, automatic backup, multi-platform sync...",
      "score": 0.95,
      "source": "Product Features"
    }
  ]
}
```

---

### Search in Specific Documents

Search only within specific documents.

```bash
curl -X GET "${BASE_URL}/api/tools/documents/search?query=warranty&docIds=uuid1,uuid2"
```

---

### List Knowledge Base Documents

List all documents available for search.

```bash
curl -X GET "${BASE_URL}/api/tools/documents/list"
```

**Response Example**

```json
{
  "documents": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Product User Manual",
      "status": "AVAILABLE"
    }
  ]
}
```

---

### Tool Calling Chat (Non-Streaming)

AI chat with function calling (non-streaming response).

```bash
curl -X POST "${BASE_URL}/api/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How is the weather in Beijing today?"
  }'
```

**Request Body**


| Field      | Type   | Required | Description                    |
| ---------- | ------ | -------- | ------------------------------ |
| `question` | string | Yes      | Question text                  |
| `docIds`   | array  | No       | Document IDs for RAG retrieval |


**Response Example**

```json
{
  "answer": "The weather in Beijing today is sunny with a temperature of 25 degrees Celsius, perfect for outdoor activities.",
  "toolCalls": ["getWeather"]
}
```

---

### Web Search Chat

Chat that triggers web search for real-time information (news, current events, etc.).

```bash
curl -X POST "${BASE_URL}/api/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the latest AI news today?"
  }'
```

**Request Body**


| Field      | Type   | Required | Description                   |
| ---------- | ------ | -------- | ----------------------------- |
| `question` | string | Yes      | Question requiring web search |


**Response Example**

```json
{
  "answer": "Here's the latest AI news: [AI news summary with sources]",
  "toolCalls": ["searchWeb"]
}
```

**Available Tools**


| Tool              | Description                              | API Key Required |
| ----------------- | ---------------------------------------- | ---------------- |
| `getWeather`      | Get current weather for a city           | No               |
| `getForecast`     | Get weather forecast                     | No               |
| `searchDocuments` | Search knowledge base documents          | No               |
| `listDocuments`   | List all knowledge base documents        | No               |
| `searchWeb`       | Search the web for real-time information | SERPER_API_KEY   |


---

### Document Search Chat

AI chat with function calling (streaming response).

```bash
curl -X POST "${BASE_URL}/api/tools/chat/stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "question": "How is the weather in Beijing today?",
    "docIds": []
  }'
```

**Response Example (SSE)**

```
data: Based on the query,
data: the weather in Beijing today is
data: sunny,
data: with a temperature of 25 degrees Celsius.
```

---

### Document Search Chat

Chat that triggers document search functionality.

```bash
curl -X POST "${BASE_URL}/api/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Find documents about artificial intelligence"
  }'
```

---

## Image Generation API

### Generate Image

Generate an image from a text prompt.

```bash
curl -X POST "${BASE_URL}/api/images/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "A beautiful sunset over the ocean",
    "model": "dall-e-3",
    "quality": "standard",
    "width": 1024,
    "height": 1024,
    "n": 1
  }'
```

**Request Body**


| Field     | Type    | Required | Description                                      |
| --------- | ------- | -------- | ------------------------------------------------ |
| `prompt`  | string  | Yes      | Image description (max 4,000 characters)         |
| `model`   | string  | No       | Model to use (e.g., "dall-e-3", "dall-e-2")      |
| `quality` | string  | No       | Image quality: "standard" or "hd"                |
| `width`   | integer | No       | Image width in pixels (256, 512, 1024, or 1792)  |
| `height`  | integer | No       | Image height in pixels (256, 512, 1024, or 1792) |
| `n`       | integer | No       | Number of images to generate (1-10, default: 1)  |


**Response Example**

```json
{
  "imageUrl": "https://example.com/images/abc123.png",
  "imageBase64": null,
  "model": "dall-e-3",
  "prompt": "A beautiful sunset over the ocean",
  "status": "SUCCESS"
}
```

Ollama returns `imageBase64` instead of `imageUrl` when using local models.

---

### Get Available Models

Get list of available image generation models.

```bash
curl -X GET "${BASE_URL}/api/images/models"
```

**Response Example**

```json
{
  "models": [
    {
      "id": "dall-e-3",
      "name": "DALL-E 3",
      "description": "Most capable model for generating images from text"
    },
    {
      "id": "dall-e-2",
      "name": "DALL-E 2",
      "description": "Faster and more affordable image generation"
    }
  ]
}
```

---

### Get Available Image Sizes

Get list of supported image dimensions.

```bash
curl -X GET "${BASE_URL}/api/images/sizes"
```

**Response Example**

```json
{
  "sizes": [
    { "width": 256, "height": 256 },
    { "width": 512, "height": 512 },
    { "width": 1024, "height": 1024 },
    { "width": 1792, "height": 1024 },
    { "width": 1024, "height": 1792 }
  ]
}
```

---

### Get Quality Options

Get available image quality options.

```bash
curl -X GET "${BASE_URL}/api/images/qualities"
```

**Response Example**

```json
{
  "qualities": [
    {
      "id": "standard",
      "name": "Standard",
      "description": "Standard quality, faster generation"
    },
    {
      "id": "hd",
      "name": "HD",
      "description": "Higher quality with more detail"
    }
  ]
}
```

---

## Audio/TTS API

### Text to Speech

Convert text to speech and receive audio file.

```bash
curl -X POST "${BASE_URL}/api/audio/speak" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Hello, welcome to our AI service!"
  }' \
  --output audio.mp3
```

**Request Body**


| Field   | Type   | Required | Description               |
| ------- | ------ | -------- | ------------------------- |
| `text`  | string | Yes      | Text to convert to speech |
| `model` | string | No       | TTS model to use          |
| `voice` | string | No       | Voice ID to use           |


**Response**

Binary audio file (audio/mpeg format, .mp3).

---

### Stream TTS (Real-time)

Stream text-to-speech in real-time.

```bash
curl -X GET "${BASE_URL}/api/audio/stream?text=Hello%20World"
```

**Query Parameters**


| Parameter | Type   | Required | Description     |
| --------- | ------ | -------- | --------------- |
| `text`    | string | Yes      | Text to convert |
| `model`   | string | No       | TTS model       |
| `voice`   | string | No       | Voice ID        |


**Response**

Streaming audio (audio/mpeg format).

---

### Get Available Voices

Get list of available TTS voices.

```bash
curl -X GET "${BASE_URL}/api/audio/voices"
```

**Response Example**

```json
{
  "voices": [
    {
      "id": "alloy",
      "name": "Alloy",
      "language": "en",
      "gender": "neutral"
    },
    {
      "id": "echo",
      "name": "Echo",
      "language": "en",
      "gender": "male"
    },
    {
      "id": "fable",
      "name": "Fable",
      "language": "en",
      "gender": "male"
    },
    {
      "id": "onyx",
      "name": "Onyx",
      "language": "en",
      "gender": "male"
    },
    {
      "id": "nova",
      "name": "Nova",
      "language": "en",
      "gender": "female"
    },
    {
      "id": "shimmer",
      "name": "Shimmer",
      "language": "en",
      "gender": "female"
    }
  ]
}
```

---

### Get Available TTS Models

Get list of available TTS models.

```bash
curl -X GET "${BASE_URL}/api/audio/models"
```

**Response Example**

```json
{
  "models": [
    {
      "id": "tts-1",
      "name": "TTS-1",
      "description": "Standard TTS model"
    },
    {
      "id": "tts-1-hd",
      "name": "TTS-1 HD",
      "description": "High definition TTS model"
    }
  ]
}
```

---

## Audio/ASR API

### WebSocket Streaming Transcription

Real-time speech-to-text using whisper.cpp via WebSocket.

**Endpoint:** `ws://localhost:9000/ws/audio/transcribe`

**Protocol:**

```json
// Client -> Server (audio chunk)
{"type": "audio", "data": "base64_wav_data"}

// Client -> Server (end stream)
{"type": "stop"}

// Server -> Client (partial result)
{"type": "partial", "text": "正在识别..."}

// Server -> Client (final result, sent before connection close)
{"type": "final", "text": "识别完成的文字"}

// Server -> Client (error)
{"type": "error", "text": "Transcription failed: ..."}
```

**Flow:**

1. Client sends one or more `audio` chunks and receives `partial` responses.
2. Client sends `stop` when finished recording.
3. Server sends `final` with the accumulated transcript, then closes the connection.

**Requirements:**

- whisper.cpp server running locally (OpenAI-compatible API on port 8178)
- Audio format: WAV, 16kHz, mono, base64-encoded

**Local setup:**

```bash
# Install whisper.cpp (macOS)
brew install whisper-cpp

# Download tiny.en model (~75MB, free)
curl -L -o ggml-tiny.en.bin \
  https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin

# Start whisper.cpp server (OpenAI-compatible endpoint)
whisper-server -m ggml-tiny.en.bin --host 127.0.0.1 --port 8178 \
  --request-path /v1/audio/transcriptions --inference-path ""
```

Configure the backend URL in `application.yml`:

```yaml
app:
  asr:
    whisper-cpp:
      base-url: http://localhost:8178
      model: whisper-base
```

---

## MCP Server API

### Health Check

Check MCP server health status.

```bash
curl -X GET "${BASE_URL}/api/mcp/health"
```

**Response Example**

```json
{
  "status": "UP",
  "server": "MCP Server",
  "version": "1.0.0",
  "protocol": "2024-11-05"
}
```

---

### Get MCP Server Info

Get detailed MCP server information.

```bash
curl -X GET "${BASE_URL}/api/mcp/info"
```

**Response Example**

```json
{
  "name": "Example MCP Server",
  "version": "1.0.0",
  "description": "A sample MCP server implementation",
  "capabilities": {
    "tools": true,
    "resources": true,
    "prompts": true
  },
  "availableTools": [
    {
      "name": "get_weather",
      "description": "Get weather information for a city",
      "inputSchema": {
        "type": "object",
        "properties": {
          "city": { "type": "string" }
        },
        "required": ["city"]
      }
    }
  ],
  "availableResources": [
    {
      "uri": "weather://current",
      "name": "Current Weather",
      "description": "Current weather conditions"
    }
  ],
  "availablePrompts": [
    {
      "name": "weather_query",
      "description": "Query weather for a location",
      "arguments": [
        { "name": "city", "required": true, "description": "City name" }
      ]
    }
  ]
}
```

---

## MCP Client API

### Get Client Status

Get MCP client connection status.

```bash
curl -X GET "${BASE_URL}/api/mcp/client/status"
```

**Response Example**

```json
{
  "status": "CONNECTED",
  "registeredTools": 5,
  "connectedServers": ["weather-server", "docs-server"]
}
```

---

### List Registered Tools

List all MCP tools registered with the client.

```bash
curl -X GET "${BASE_URL}/api/mcp/client/tools"
```

**Response Example**

```json
{
  "tools": [
    {
      "name": "get_weather",
      "server": "weather-server",
      "description": "Get weather for a city"
    },
    {
      "name": "search_documents",
      "server": "docs-server",
      "description": "Search knowledge base"
    }
  ]
}
```

---

### List Connected Servers

List all connected MCP servers.

```bash
curl -X GET "${BASE_URL}/api/mcp/client/servers"
```

**Response Example**

```json
{
  "servers": [
    {
      "name": "weather-server",
      "version": "1.0.0",
      "status": "CONNECTED"
    },
    {
      "name": "docs-server",
      "version": "1.0.0",
      "status": "CONNECTED"
    }
  ]
}
```

---

### Chat with MCP Tools

Chat with AI using available MCP tools.

```bash
curl -X POST "${BASE_URL}/api/mcp/client/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the weather in Beijing?",
    "docIds": []
  }'
```

**Request Body**


| Field      | Type   | Required | Description                  |
| ---------- | ------ | -------- | ---------------------------- |
| `question` | string | Yes      | Question text                |
| `docIds`   | array  | No       | Document IDs for RAG context |


**Response Example**

```json
{
  "answer": "The weather in Beijing today is sunny with a temperature of 25°C.",
  "toolCalls": ["get_weather"],
  "sessionId": "session-123"
}
```

---

## Chat Evaluation API

Evaluate AI chat response quality using LLM-as-a-Judge and Spring AI Evaluators.

### Evaluate Chat Response

```bash
curl -X POST "${BASE_URL}/api/eval/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "What is the capital of France?",
    "assistantResponse": "Paris is the capital of France.",
    "referenceDocuments": ["France is a country in Europe. Its capital is Paris."]
  }'
```

**Request Body**

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| `userMessage` | string | Yes | Original user question |
| `assistantResponse` | string | Yes | AI assistant reply to evaluate |
| `referenceDocuments` | array | No | RAG context chunks for relevancy and factuality checks |

**Behavior**

- Without `referenceDocuments`: evaluates coherence, relevancy, helpfulness, and safety. `factualityAvailable` is `false` and `factualityScore` is `null`.
- With `referenceDocuments`: also runs grounded fact-checking against the provided context. `factualityAvailable` is `true`.

**Response Example**

```json
{
  "coherenceScore": 0.9,
  "relevanceScore": 1.0,
  "helpfulnessScore": 0.88,
  "factualityScore": 1.0,
  "factualityAvailable": true,
  "overallScore": 0.94,
  "hasSafetyIssues": false,
  "safetyFlags": [],
  "suggestions": []
}
```

**Stop / Control Messages**

Not applicable — this is a synchronous REST endpoint (not WebSocket).

---

## Error Codes

### Error Response Format

All errors return the following standard format:

```json
{
  "error": "ERROR_CODE",
  "message": "Error message description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2026-06-19T03:30:00Z",
  "path": "/api/endpoint"
}
```

### Common Error Codes


| HTTP Status | Error Code           | Description                      |
| ----------- | -------------------- | -------------------------------- |
| `400`       | `VALIDATION_ERROR`   | Request validation failed        |
| `400`       | `BAD_REQUEST`        | Invalid request parameters       |
| `404`       | `SESSION_NOT_FOUND`  | Chat session does not exist      |
| `404`       | `DOCUMENT_NOT_FOUND` | Document does not exist          |
| `413`       | `FILE_TOO_LARGE`     | Uploaded file exceeds 50MB limit |
| `503`       | `AI_SERVICE_ERROR`   | AI service unavailable or error  |
| `500`       | `RAG_SERVICE_ERROR`  | RAG service error                |
| `500`       | `INTERNAL_ERROR`     | Internal server error            |


### Error Response Examples

**Validation Failed (400)**

```json
{
  "error": "VALIDATION_ERROR",
  "message": "message: Message content exceeds 10,000 characters",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-19T03:30:00Z"
}
```

**Session Not Found (404)**

```json
{
  "error": "SESSION_NOT_FOUND",
  "message": "Session not found: abc123",
  "errorCode": "SESSION_NOT_FOUND",
  "timestamp": "2026-06-19T03:30:00Z"
}
```

**AI Service Error (503)**

```json
{
  "error": "AI_SERVICE_ERROR",
  "message": "AI service error: Connection timeout",
  "errorCode": "AI_SERVICE_ERROR",
  "timestamp": "2026-06-19T03:30:00Z"
}
```

---

## Notes

- All timestamps use ISO 8601 format (UTC timezone)
- Streaming responses use Server-Sent Events (SSE) protocol
- Maximum upload file size: 50MB
- Maximum chat message length: 10,000 characters

