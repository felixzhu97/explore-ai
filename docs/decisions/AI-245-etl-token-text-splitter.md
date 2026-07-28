# AI-245 — ETL TokenTextSplitter alignment

Status: Draft / In Progress  
Type: Implementation

## Goal

Align document chunking/embedding with Spring AI ETL + same embedding model as VectorStore retrieval (AI-240).

## Scope

- TokenTextSplitter (or documented equivalent)
- Same embedding config for ingest and retrieve
- Migration note for existing READY docs

## Out of scope

- Rewriting VectorStore SPI itself (AI-253)

## References

- https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html
