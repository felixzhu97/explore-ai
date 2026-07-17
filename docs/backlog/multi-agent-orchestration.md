# Multi-Agent Orchestration Backlog

## Goal

Deliver Supervisor + specialized workers using Spring AI [Orchestrator-Workers](https://docs.spring.io/spring-ai/reference/api/effective-agents.html), with SSE streaming and a minimal Angular `/agents` UI.

## Architecture

```text
UI /agents
  → AgentController (/api/agents)
    → AgentFacade / OrchestratorWorkersUseCase
      → SupervisorRouter (structured routing)
      → WorkerAgentInvoker (per-agent system prompt + optional tools)
    → SSE: agent_handoff | message | error | done
```

## API (this PR)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/agents/list` | List registered agents |
| GET | `/api/agents/{type}/health` | Agent health |
| GET | `/api/agents/{type}` | Agent details |
| POST | `/api/agents/supervisor/invoke/sse` | Supervisor orchestration stream |
| POST | `/api/agents/pipeline/invoke/sse` | Run user-authored agent pipeline graph |

Request body: `{ "message": "...", "sessionId?: "...", "agentType?: "..." }`

## Registered agents

| Type | Role |
|------|------|
| `supervisor` | Orchestrator (routing only) |
| `k8s` | Kubernetes advisor |
| `monitoring` | Metrics / alerts advisor |
| `aiops` | Incident / anomaly advisor |
| `vectordb` | RAG / vector advisor (+ document search tools) |

## Story map

| Story | SP | This PR |
|-------|----|---------|
| A List agents + health | 3 | Yes |
| B Supervisor route + SSE | 5 | Yes |
| C Direct agent invoke + SSE | 5 | Yes |
| D Frontend agent picker / stream / quick prompts | 5 | Yes |
| E Session save & export | 3 | No |
| F Pluggable real ops tools | 8 | No |
| G Drag-drop agent pipeline canvas (CDK + Foblex) | 8 | Yes ([AI-148](https://felixzhu.atlassian.net/browse/AI-148)) |

## Out of scope (this PR)

- Real kubectl / Prometheus / MLflow integrations
- Python LangGraph migration
- Custom agent authoring UI
- Conversation export

## Pipeline canvas (AI-148)

- CDK palette drag + Foblex node/edge canvas on `/agents` Pipeline mode
- `POST /api/agents/pipeline/invoke/sse` with `{ message, nodes, edges }`

## Copy-paste user stories

See the chat reply / PR description for full Epic + Stories A–F Markdown (Background / User Story / GIVEN-WHEN-THEN / Notes / SP).

## References

- [Building Effective Agents - Spring AI](https://docs.spring.io/spring-ai/reference/api/effective-agents.html)
- [Orchestrator-Workers example](https://github.com/spring-projects/spring-ai-examples/tree/main/agentic-patterns/orchestrator-workers)
- [User Story Mapping - Jeff Patton](https://www.jpattonassociates.com/user-story-mapping/)
