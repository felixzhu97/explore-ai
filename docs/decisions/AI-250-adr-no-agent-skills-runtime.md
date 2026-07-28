# ADR: Defer Agent Skills runtime (AI-250)

Status: Accepted (Won't / Deferred this phase)  
Date: 2026-07-28

## Decision

Do **not** wire Agent Skills / Cursor skill directories / `spring-ai-agent-utils` Skills runtime into production in this phase.

## Why

API surface is still evolving; ExploreAI Agent already has `AgentPromptCatalog` + tools. Premature binding risks churn without user value.

## Revisit when

1. Official Skills API marked stable for Spring AI 2.x production use
2. Clear Agent-module ROI (measurable latency/quality or ops benefit)

## Consequences

- Observe upstream only; no production skill-file loader
- Experimental branches allowed if isolated from default boot

## References

- https://docs.spring.io/spring-ai-agent-utils/reference/
- https://docs.spring.io/spring-ai/reference/
