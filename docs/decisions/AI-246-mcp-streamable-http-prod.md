# AI-246 — Production MCP Streamable HTTP

Status: Draft / In Progress  
Type: Implementation

## Goal

Non-local MCP via Streamable HTTP with timeout/isolation; keep local default off (AI-240).

## Scope

- HTTP MCP config for cloud/prod
- Failure isolation for Chat/RAG core
- Docs: STDIO local-optional only

## References

- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
- https://blog.modelcontextprotocol.io/posts/2026-07-28-mcp-is-evolving/
