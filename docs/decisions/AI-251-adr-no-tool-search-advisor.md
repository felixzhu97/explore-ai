# ADR: Defer ToolSearchAdvisor (AI-251)

Status: Accepted (Won't / Deferred this phase)  
Date: 2026-07-28

## Decision

Do **not** enable ToolSearchAdvisor in production until tool catalog size justifies it.

## Why

ToolSearch helps large tool catalogs (token cost). Current tool count is below the ~15–20 threshold.

## Revisit when

Registered production tools exceed ~15–20 **or** prompt token share from tool schemas becomes a measured cost issue.

## Boundary

- AI-244 may converge `AnswerAfterToolsAdvisor` to official ToolCalling **without** ToolSearch
- This ADR forbids ToolSearch only

## References

- https://docs.spring.io/spring-ai-agent-utils/reference/toolsearch.html
