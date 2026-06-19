# AI Service API Documentation

## Base URL

```
@baseUrl = http://localhost:9000
@contentType = application/json
```

---

## Table of Contents

1. [Health Check](#health-check)
2. [Chat APIs](#chat-apis)
3. [Session APIs](#session-apis)
4. [RAG APIs](#rag-apis)
5. [Tool Calling APIs](#tool-calling-apis)
6. [Error Codes](#error-codes)

---

## Health Check

### Health Check

Check if the service is running.

```http
GET {{baseUrl}}/api/health
```

**Response**

```json
{
  "status": "UP"
}
```

---

## Chat APIs

### Chat with AI

Send a chat message and receive an AI response. Supports session context for multi-turn conversations.

```http
POST {{baseUrl}}/api/chat
Content-Type: {{contentType}}

{
  "message": "Hello, how are you?"
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `message` | string | Yes | The chat message (max 10000 characters) |
| `sessionId` | string | No | Session ID for multi-turn conversations |

**Response**

```json
{
  "response": "Hello! I'm doing great, thank you for asking. How can I help you today?",
  "sessionId": "abc123",
  "messageId": "msg-uuid",
  "timestamp": "2026-06-19T03:30:00Z"
}
```

---

### Chat with AI (with Session)

Continue a conversation using a session ID.

```http
POST {{baseUrl}}/api/chat
Content-Type: {{contentType}}

{
  "message": "Continue our conversation about AI",
  "sessionId": "your-session-id"
}
```

---

### Simple Chat (Legacy)

Simple chat without session support (legacy API).

```http
POST {{baseUrl}}/api/chat/simple
Content-Type: {{contentType}}

{
  "message": "Hello!"
}
```

**Response**

```json
{
  "response": "Hello! How can I assist you?"
}
```

---

### Text Analysis

Analyze text and return structured results including summary, sentiment, key points, and entities.

```http
POST {{baseUrl}}/api/chat/analyze
Content-Type: {{contentType}}

{
  "text": "This is a sample text that needs analysis.",
  "language": "en"
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `text` | string | Yes | The text to analyze |
| `language` | string | No | Target language for analysis (e.g., "en", "zh") |

**Response**

```json
{
  "summary": "A brief summary of the text...",
  "sentiment": "NEUTRAL",
  "keyPoints": [
    "First key point",
    "Second key point"
  ],
  "entities": [
    "Entity 1",
    "Entity 2"
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

## Session APIs

### Create Session

Create a new chat session.

```http
POST {{baseUrl}}/api/sessions
Content-Type: {{contentType}}

{
  "title": "My Chat Session"
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | No | Session title (defaults to "New Chat") |

**Response**

```json
{
  "sessionId": "abc123",
  "title": "My Chat Session",
  "messageCount": 0,
  "createdAt": "2026-06-19T03:30:00Z",
  "lastActivityAt": "2026-06-19T03:30:00Z"
}
```

---

### List All Sessions

Retrieve all chat sessions.

```http
GET {{baseUrl}}/api/sessions
```

**Response**

```json
[
  {
    "sessionId": "abc123",
    "title": "My Chat Session",
    "messageCount": 5,
    "createdAt": "2026-06-19T03:30:00Z",
    "lastActivityAt": "2026-06-19T04:00:00Z"
  }
]
```

---

### Get Session Messages

Retrieve message history for a specific session.

```http
GET {{baseUrl}}/api/sessions/{sessionId}/messages
```

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | string | The session ID |

**Response**

```json
{
  "sessionId": "abc123",
  "messages": [
    {
      "id": "msg-uuid-1",
      "text": "Hello!",
      "role": "user",
      "timestamp": "2026-06-19T03:30:00Z"
    },
    {
      "id": "msg-uuid-2",
      "text": "Hi there! How can I help?",
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

```http
DELETE {{baseUrl}}/api/sessions/{sessionId}
```

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | string | The session ID |

**Response**

`204 No Content`

---

## RAG APIs

### List Documents

List all documents in the knowledge base.

```http
GET {{baseUrl}}/api/rag/documents/
```

**Response**

```json
{
  "documents": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Sample Document",
      "status": "PROCESSED",
      "createdAt": "2026-06-19T03:30:00Z",
      "chunkCount": 5
    }
  ]
}
```

---

### Upload Document

Upload a document to the knowledge base. Supports TXT and PDF files.

```http
POST {{baseUrl}}/api/rag/documents/upload
Content-Type: multipart/form-data

file: sample.txt
title: Sample Document
```

**Form Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | file | Yes | The file to upload (TXT or PDF) |
| `title` | string | No | Document title (defaults to filename) |

**Response**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Sample Document",
  "status": "PROCESSED",
  "chunkCount": 5,
  "createdAt": "2026-06-19T03:30:00Z"
}
```

---

### Upload PDF Document

```http
POST {{baseUrl}}/api/rag/documents/upload
Content-Type: multipart/form-data

file: document.pdf
title: PDF Document
```

---

### Delete Document

Delete a document from the knowledge base.

```http
DELETE {{baseUrl}}/api/rag/documents/{id}
```

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | The document ID |

**Response**

`204 No Content`

---

### RAG Chat (Streaming)

Chat with AI using document retrieval (streaming response).

```http
POST {{baseUrl}}/api/rag/chat/stream
Content-Type: {{contentType}}
Accept: text/event-stream

{
  "question": "What is the content about?",
  "docIds": [],
  "topK": 5
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `question` | string | Yes | The question to ask |
| `docIds` | array | No | Specific document IDs to search (empty = all) |
| `topK` | integer | No | Number of chunks to retrieve (default: 5) |
| `temperature` | number | No | AI temperature (default: 0.7) |
| `sessionId` | string | No | Session ID for context |

**Response (SSE)**

```
event: message
data: The 
data: content 
data: is 
data: about 

event: sources
data: [{"text":"...","score":0.95,"metadata":{"source":"doc.pdf"}}]
```

---

### RAG Chat with Specific Documents

Ask a question about specific documents only.

```http
POST {{baseUrl}}/api/rag/chat/stream
Content-Type: {{contentType}}
Accept: text/event-stream

{
  "question": "Summarize the key points",
  "docIds": ["550e8400-e29b-41d4-a716-446655440000", "660e8400-e29b-41d4-a716-446655440001"],
  "topK": 3
}
```

---

## Tool Calling APIs

### Get Weather

Get current weather for a city.

```http
GET {{baseUrl}}/api/tools/weather?city=Beijing
```

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | string | Yes | City name |

**Response**

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

Get weather forecast for a city.

```http
GET {{baseUrl}}/api/tools/weather/forecast?city=Shanghai&days=7
```

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | string | Yes | City name |
| `days` | integer | No | Number of days (default: varies) |

**Response**

```json
{
  "city": "Shanghai",
  "forecast": [
    {"day": 1, "condition": "Sunny", "high": "28°C", "low": "22°C"},
    {"day": 2, "condition": "Cloudy", "high": "26°C", "low": "20°C"}
  ]
}
```

---

### Search Documents

Search for documents in the knowledge base.

```http
GET {{baseUrl}}/api/tools/documents/search?query=artificial%20intelligence
```

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | Search query |
| `docIds` | string | No | Comma-separated document IDs to filter |

**Response**

```json
{
  "results": [
    {
      "text": "Document content snippet...",
      "score": 0.95,
      "source": "doc-title"
    }
  ]
}
```

---

### Search in Specific Documents

Search within specific documents only.

```http
GET {{baseUrl}}/api/tools/documents/search?query=AI&docIds=uuid1,uuid2
```

---

### List Documents in Knowledge Base

List all documents available for search.

```http
GET {{baseUrl}}/api/tools/documents/list
```

**Response**

```json
{
  "documents": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Document Title",
      "status": "AVAILABLE"
    }
  ]
}
```

---

### Chat with Function Calling

Chat with AI using function calling (non-streaming).

```http
POST {{baseUrl}}/api/tools/chat
Content-Type: {{contentType}}

{
  "question": "What's the weather in Beijing?"
}
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `question` | string | Yes | The question to ask |
| `docIds` | array | No | Specific document IDs for RAG |

**Response**

```json
{
  "answer": "The weather in Beijing is currently sunny with a temperature of 25°C.",
  "toolCalls": ["getWeather"]
}
```

---

### Chat with Function Calling (Streaming)

Chat with AI using function calling (streaming response).

```http
POST {{baseUrl}}/api/tools/chat/stream
Content-Type: {{contentType}}
Accept: text/event-stream

{
  "question": "What's the weather in Beijing?",
  "docIds": []
}
```

**Response (SSE)**

```
data: The 
data: weather 
data: in 
data: Beijing 
data: is 
data: 25°C 
data: and 
data: sunny.
```

---

### Chat with Document Search

Ask a question that triggers document search.

```http
POST {{baseUrl}}/api/tools/chat
Content-Type: {{contentType}}

{
  "question": "Search for information about machine learning"
}
```

---

## Error Codes

### Error Response Format

All errors return a standard response format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
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
| `404` | `SESSION_NOT_FOUND` | Chat session not found |
| `404` | `DOCUMENT_NOT_FOUND` | Document not found |
| `413` | `FILE_TOO_LARGE` | Uploaded file exceeds 50MB limit |
| `503` | `AI_SERVICE_ERROR` | AI service unavailable or error |
| `500` | `RAG_SERVICE_ERROR` | RAG service error |
| `500` | `INTERNAL_ERROR` | Unexpected server error |

### Example Error Responses

**Validation Error (400)**

```json
{
  "error": "VALIDATION_ERROR",
  "message": "message: Message cannot exceed 10000 characters",
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

- All timestamps are in ISO 8601 format (UTC)
- Streaming responses use Server-Sent Events (SSE)
- Maximum upload file size: 50MB
- Chat message max length: 10,000 characters
