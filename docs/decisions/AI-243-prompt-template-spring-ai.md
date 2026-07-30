# AI-243 — Unify PromptTemplate

Status: Implemented  
Type: Implementation

## Goal

Migrate Chat / RAG / Agent prompts onto Spring AI `PromptTemplate` (thin wrapper allowed).

## Scope

- Replace ad-hoc `ClasspathPromptLoader.fill` with `ClasspathPromptTemplate` + Spring AI `PromptTemplate`
- Keep Eval path consistent via the same wrapper
- Behavior regression tests (`PromptTemplatesTest`, `LocalizedRagPromptBuilderTest`, `ClasspathPromptTemplateTest`)

## Out of scope

- Prompt content redesign / product copy rewrite

## Implementation

- `ClasspathPromptTemplate`: loads classpath `.st` resources and renders placeholders via Spring AI `PromptTemplate`
- Static fragments with JSON braces (A2UI examples) stay load/join-only — not passed through `render()` without variables
- `PromptTemplates` holds `PromptTemplate` instances for task templates; RAG user prompts render through the wrapper

## References

- https://docs.spring.io/spring-ai/reference/api/prompt.html
