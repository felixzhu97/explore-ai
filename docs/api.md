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
6. [Error Codes](#error-codes)

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

| Field | Type | Required | Description |
|------|------|----------|-------------|
| `message` | string | Yes | Chat message (max 10,000 characters) |
| `sessionId` | string | No | Session ID for multi-turn conversations |

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

| Field | Type | Required | Description |
|------|------|----------|-------------|
| `text` | string | Yes | Text to analyze |
| `language` | string | No | Analysis language (e.g., "en", "zh") |

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

| Value | Description |
|-------|-------------|
| `POSITIVE` | Positive sentiment |
| `NEUTRAL` | Neutral sentiment |
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

| Field | Type | Required | Description |
|------|------|----------|-------------|
| `title` | string | No | Session title (default: "New Session") |

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

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | string | Session ID |

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

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | string | Session ID |

**Response**

`204 No Content`

---

## RAG API

### Get Document List

Get all documents in the knowledge base.

```bash
curl -X GET "${BASE_URL}/api/rag/documents/"
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

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | file | Yes | File to upload (TXT or PDF) |
| `title` | string | No | Document title (default: filename) |

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
|-----------|------|-------------|
| `id` | UUID | Document ID |

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

| Field | Type | Required | Description |
|------|------|----------|-------------|
| `question` | string | Yes | Question text |
| `docIds` | array | No | Document IDs to search (empty = all) |
| `topK` | integer | No | Number of document chunks to retrieve (default: 5) |
| `temperature` | number | No | AI temperature parameter (default: 0.7) |
| `sessionId` | string | No | Session ID |

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

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | string | Yes | City name |

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

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | string | Yes | City name |
| `days` | integer | No | Number of forecast days |

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

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | Search keyword |
| `docIds` | string | No | Comma-separated document IDs to limit scope |

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

| Field | Type | Required | Description |
|------|------|----------|-------------|
| `question` | string | Yes | Question text |
| `docIds` | array | No | Document IDs for RAG retrieval |

**Response Example**

```json
{
  "answer": "The weather in Beijing today is sunny with a temperature of 25 degrees Celsius, perfect for outdoor activities.",
  "toolCalls": ["getWeather"]
}
```

---

### Tool Calling Chat (Streaming)

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

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| `400` | `VALIDATION_ERROR` | Request validation failed |
| `400` | `BAD_REQUEST` | Invalid request parameters |
| `404` | `SESSION_NOT_FOUND` | Chat session does not exist |
| `404` | `DOCUMENT_NOT_FOUND` | Document does not exist |
| `413` | `FILE_TOO_LARGE` | Uploaded file exceeds 50MB limit |
| `503` | `AI_SERVICE_ERROR` | AI service unavailable or error |
| `500` | `RAG_SERVICE_ERROR` | RAG service error |
| `500` | `INTERNAL_ERROR` | Internal server error |

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
