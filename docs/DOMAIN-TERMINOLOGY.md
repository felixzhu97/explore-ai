# Domain Terminology (Frontend ↔ Backend)

Canonical **JSON API** convention: **camelCase**. Legacy snake_case is accepted via `@JsonAlias` on backend DTOs during migration.

## Chat & Sessions

| Concept | Domain (Java) | API JSON | Frontend (TS) |
|---------|---------------|----------|-----------------|
| Session ID | `ChatSessionId` | `sessionId` | `sessionId` |
| Message body | `ChatMessage.text` | `content` | `content` |
| Message role | `user` / `assistant` | `role` | `role` |
| Stream tools flag | `TextChatOptions.toolsEnabled` | `toolsEnabled` | `toolsEnabled` |
| Provider label | `displayName` | `displayName` | `displayName` |
| Provider status | — | `available` / `unavailable` | same |

Endpoints: `POST /api/text/chat/stream`, `GET/POST/DELETE /api/sessions`

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

## Error codes (REST)

`SESSION_NOT_FOUND`, `AI_SERVICE_ERROR`, `DOCUMENT_NOT_FOUND`, `RAG_SERVICE_ERROR`, `IMAGE_PROVIDER_NOT_CONFIGURED`, `TTS_PROVIDER_NOT_CONFIGURED`, `VALIDATION_ERROR`, `CHAT_MEMORY_ERROR`, `INTERNAL_ERROR`
