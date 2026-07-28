# ADR: Opt-in Agent Skills runtime (AI-250)

Status: Accepted (Implemented)  
Date: 2026-07-28

## Decision

Add an **opt-in** Agent Skills loader for the Agent module. Controlled skill id lists load `SKILL.md` metadata from classpath resources and inject prompt guidance plus the Spring AI `SkillsTool` callback into worker invocations. Default is **off**; invalid or unlisted skills fail soft with logs only.

## Why

ExploreAI Agent already has `AgentPromptCatalog` and per-worker tools. Agent Skills add a controlled way to ship reusable instruction packs without changing default boot behavior or binding Cursor dev-tooling paths into production.

## Configuration

```yaml
app:
  agent:
    skills:
      enabled: ${AGENT_SKILLS_ENABLED:false}
      ids: [] # e.g. [brief-style]
      resource-location: classpath:agent/skills/
```

When `enabled=false` (default), no skills are loaded and worker prompts are unchanged.

## Consequences

- `AgentSkillLoader` parses and filters skills by configured ids; malformed files are skipped
- `AgentSkillsRuntime` augments worker system prompts and registers `SkillsTool` when active
- `spring-ai-agent-utils` is on the classpath; runtime wiring stays Agent-module scoped
- Sample skill: `classpath:agent/skills/brief-style/SKILL.md`

## References

- https://spring-ai-community.github.io/spring-ai-agent-utils/latest-snapshot/tools/SkillsTool/
- https://docs.spring.io/spring-ai/reference/
