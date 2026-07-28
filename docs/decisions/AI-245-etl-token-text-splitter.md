# AI-245 — ETL TokenTextSplitter alignment

Status: Implemented  
Type: Implementation

## Goal

Align document chunking/embedding with Spring AI ETL + same embedding model as VectorStore retrieval (AI-240).

## Outcome

- **`RagEtlConfig`** — wires Spring AI `TokenTextSplitter` from `spring.ai.rag.chunk.size` (tokens).
- **`ChunkingDocumentTransformer`** — uses `TokenTextSplitter` instead of custom character-based `ChunkingService` (removed).
- **Embedding** — ingest (`EmbeddingDocumentWriter` → `OllamaEmbeddingAdapter`) and retrieval (`H2SpringAiVectorStore` → `TextEmbeddingRepository`) share the same `EmbeddingModel` bean from `OllamaConfig` (`spring.ai.ollama.embedding.model`, default `mxbai-embed-large`).

## Migration — existing READY documents

Documents ingested **before** this change used character-based splitting (size/overlap in characters). After deploy:

1. Chunk boundaries and counts will differ for **new uploads**.
2. Existing READY documents keep old chunks/embeddings until re-processed.
3. **Recommended:** delete and re-upload READY documents that must match new retrieval quality, or add a future re-ingest job (out of scope here).

`spring.ai.rag.chunk.overlap` remains in config for MCP compatibility but is **not applied** by Spring AI 2.0 `TokenTextSplitter`.

## Out of scope

- Rewriting VectorStore SPI itself (AI-253)
- Automated bulk re-ingest of existing READY docs

## References

- https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html
- https://docs.spring.io/spring-ai/reference/api/embeddings.html
