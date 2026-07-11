# Domain Terminology (Frontend ↔ Backend)

> Canonical business terms: see [Domain Glossary](Domain-Glossary.md). This document maps **API JSON fields** between frontend and backend.

Canonical **JSON API** convention: **camelCase**. Legacy snake_case is accepted via `@JsonAlias` on backend DTOs during migration.

## Chat & Sessions

| Concept | Domain (Java) | API JSON | Frontend (TS) |
|---------|---------------|----------|-----------------|
| Session ID | `ChatSessionId` | `sessionId` | `sessionId` |
| Session title | — | `title` | `title` |
| Message count | — | `messageCount` | — |
| Last activity | `ChatSession.lastActivityAt` | `lastActivityAt` | — |
| Message body | `ChatMessage.text` | `content` | `content` |
| Message role | `user` / `assistant` / `system` | `role` | `role` |
| Stream tools flag | `TextChatOptions.toolsEnabled` | `toolsEnabled` | `toolsEnabled` |
| Provider label | `displayName` | `displayName` | `displayName` |
| Provider status | — | `available` / `unavailable` | same |
| Tool call status | — | — | `pending` / `running` / `success` / `error` |

Endpoints: `POST /api/text/chat/stream`, `GET/POST/DELETE /api/sessions`, `GET /api/text/providers`, `GET /api/text/models`

## RAG

| Concept | Domain (Java) | API JSON | Frontend (TS) |
|---------|---------------|----------|-----------------|
| User question | `RagChatRequest.question` | `query` | `query` |
| Session ID | `sessionId` | `sessionId` | `sessionId` |
| Retrieval limit | `topK` | `topK` | `topK` |
| Document filter | `docIds` | `docIds` | `docIds` |
| Citation text | `SourceDocument.text` | `text` | `text` |
| Similarity score | `score` | `score` | `score` |
| Document summary | `DocumentSummaryDto` | `id`, `title`, `status`, `createdAt`, `chunkCount` | `DocumentListItem` |

Endpoints: `GET/POST/DELETE /api/rag/documents`, `POST /api/rag/chat/stream`

## Vision

| Concept | API JSON | Frontend (TS) |
|---------|----------|-----------------|
| OCR text | `fullText` | `fullText` |
| Detection label | `className` | `className` |
| Latency | `processingTimeMs` | `processingTimeMs` |

Endpoints: `POST /api/vision/{caption,detect,ocr}`

## Image Generation

| Concept | API JSON | Frontend (TS) |
|---------|----------|-----------------|
| Result URL | `imageUrl` | `imageUrl` |
| Result base64 | `imageBase64` | `imageBase64` |
| Revised prompt | `revisedPrompt` | `revisedPrompt` |

Endpoint: `POST /api/images/generate`

## TTS

| Concept | API JSON | Frontend (TS) |
|---------|----------|-----------------|
| Speech text | `text` | `text` |
| Voice ID | `voice` | `voice` |
| Speed | `speed` | `speed` |
| Output format | `outputFormat` | `outputFormat` |

Endpoints: `GET /api/audio/voices`, `POST /api/audio/speak`

## UI labels (i18n)

| Glossary Term | i18n Key | EN Label |
|---------------|----------|----------|
| Document QA | `nav.documentQA`, `ragChat.title` | Document QA |
| Chat Session | `sidebar.newChat`, `sidebar.recents` | New Chat Session / Recent Sessions |
| Source Document | `ragChat.sources`, `ragChat.basedOn` | Source Documents |
| Similarity Score | `ragChat.similarity` | Similarity Score |
| Tools Enabled | `chat.toolsEnabled` | Tools Enabled |
| Image Generation | `generate.tabs.image` | Image Generation |
| Image Analysis | `nav.imageAnalysis` | Image Analysis |

## Error codes (REST)

| Code | HTTP | Meaning |
|------|------|---------|
| `SESSION_NOT_FOUND` | 404 | Chat session not found |
| `DOCUMENT_NOT_FOUND` | 404 | RAG document not found |
| `AI_SERVICE_ERROR` | 503 | LLM / AI provider failure |
| `RAG_SERVICE_ERROR` | 500 | RAG pipeline failure |
| `IMAGE_PROVIDER_NOT_CONFIGURED` | 503 | Image generation unavailable |
| `TTS_PROVIDER_NOT_CONFIGURED` | 503 | Speech synthesis unavailable |
| `VALIDATION_ERROR` | 400 | Invalid request body |
| `CHAT_MEMORY_ERROR` | 500 | Chat memory sync failure |
| `FILE_TOO_LARGE` | 413 | Upload exceeds 50MB |
| `INTERNAL_ERROR` | 500 | Unhandled server error |
