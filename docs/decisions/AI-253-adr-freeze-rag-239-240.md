# ADR: Freeze AI-239/240 RAG main path (AI-253)

Status: Accepted (Won't rewrite)  
Date: 2026-07-28

## Decision

Do **not** rewrite the merged AI-239/AI-240 RAG main path (single-pass retrieval, Observation defaults, H2 VectorStore SPI, text `ChatClient.stream()`, local MCP fail-soft).

## Allowed incremental stories

- AI-242: vision true stream
- AI-245: ETL TokenTextSplitter / embedding alignment
- Other hotspot items that do not replace the above core

## Why

Those paths are delivered and tested; hotspot work should be incremental, not a second rewrite.

## References

- https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- https://felixzhu.atlassian.net/browse/AI-239
- https://felixzhu.atlassian.net/browse/AI-240
