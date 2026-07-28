# AI-242 — RAG vision true streaming

Status: Draft / In Progress  
Type: Implementation

## Goal

Align vision RAG with text path `ChatClient.stream()` true SSE (AI-240 already covers text).

## Scope

- Vision branch uses `chatStream` where feasible
- Preserve text RAG + sources behavior
- Explicit error end signal on stream failure

## Out of scope

- Rewriting AI-239/240 text RAG main path (see AI-253)

## References

- https://docs.spring.io/spring-ai/reference/api/chatclient.html
