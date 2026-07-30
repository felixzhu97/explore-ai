# AI-242 — RAG vision true streaming

Status: Implemented  
Type: Implementation

## Goal

Align vision RAG with the text path `ChatClient.stream()` true SSE (AI-240 covers text-only RAG).

## Decision

- `POST /api/rag/chat/stream` with `images` delegates to `VisionChatUseCase.chatStreamWithImages`, which calls `ChatClient.stream().content()` on the Ollama vision model after manual vector retrieval (same pre-stream steps as the previous blocking path).
- Removed `StreamingService` fake word-chunk streaming from the vision branch in `RagController`.
- When Ollama vision is unavailable (`VisionChatUseCase` bean absent), the controller falls back to text `RagChatUseCase.chatStream` (ignores images).
- Stream failures emit an SSE `error` event with a message payload, then complete.

## Limitations

- Retrieval still runs synchronously before tokens stream (images do not participate in vector search).
- True token streaming depends on the configured Ollama vision model supporting the streaming chat API via Spring AI `OllamaChatModel`.
- `chatWithImages()` (blocking) remains for direct programmatic use but is no longer used by the HTTP stream endpoint.

## References

- [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
