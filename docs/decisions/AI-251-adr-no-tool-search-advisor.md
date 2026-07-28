# AI-251 — ToolSearchToolCallingAdvisor

Status: Implemented  
Date: 2026-07-28

## Goal

Wire Spring AI `ToolSearchToolCallingAdvisor` behind a config switch so large tool catalogs only expose search-matched tools to the model.

## Behavior

- Default: `app.ai.tool-search.enabled=false` — keep existing `AnswerAfterToolsAdvisor` tool loop
- Enabled: use `ToolSearchToolCallingAdvisor` + `RegexToolIndex` instead of the full-schema tool advisor
- Env: `AI_TOOL_SEARCH_ENABLED=true`

## Out of scope

- Vector / Lucene indexes (regex is enough for current catalog size)
- Changing AI-244 loop-guard convergence on other branches (merge may rebase this switch onto `ToolCallLoopGuardAdvisor`)

## References

- https://docs.spring.io/spring-ai/reference/guides/dynamic-tool-search.html
- https://docs.spring.io/spring-ai/reference/api/tools.html
