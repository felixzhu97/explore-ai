# AI-246 — Production MCP Streamable HTTP

Status: Implemented  
Type: Implementation

## Goal

Non-local MCP via Streamable HTTP with timeout/isolation; keep local default off (AI-240).

## Scope

- HTTP MCP config for cloud/prod
- Failure isolation for Chat/RAG core
- Docs: STDIO local-optional only

## Decision

- **Prod transport**: `spring.ai.mcp.client.streamable-http.connections.remote` driven by `MCP_STREAMABLE_HTTP_URL` / `MCP_STREAMABLE_HTTP_ENDPOINT`; opt-in via `MCP_CLIENT_ENABLED=true`.
- **Timeouts**: `spring.ai.mcp.client.request-timeout` (default 15s prod, 20s base) applied through `McpClientResilienceConfig` (`requestTimeout` + `initializationTimeout`).
- **Boot safety**: `spring.ai.mcp.client.initialized=false` defers client init; `McpClientToolsConfig` catches registration failures and returns empty callbacks so Chat/RAG keep local tools.
- **Local**: `application-local.yml` keeps `spring.ai.mcp.client.enabled=false`; STDIO Fetch documented only in `application-local.yml.example`.

## Env (prod)

| Variable | Default | Purpose |
|----------|---------|---------|
| `MCP_CLIENT_ENABLED` | `false` | Enable remote MCP client |
| `MCP_STREAMABLE_HTTP_URL` | — | Streamable HTTP base URL |
| `MCP_STREAMABLE_HTTP_ENDPOINT` | `/mcp` | MCP endpoint path |
| `MCP_REQUEST_TIMEOUT` | `15s` | Request + initialization timeout |

## References

- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
- https://blog.modelcontextprotocol.io/posts/2026-07-28-mcp-is-evolving/
- https://modelcontextprotocol.io/specification/2025-11-25/basic/transports
