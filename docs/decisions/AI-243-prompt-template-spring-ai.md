# AI-243 — Unify PromptTemplate

Status: Draft / In Progress  
Type: Implementation

## Goal

Migrate Chat / RAG / Agent prompts onto Spring AI `PromptTemplate` (thin wrapper allowed).

## Scope

- Replace ad-hoc `PromptTemplates` usage where duplicated
- Keep Eval path consistent
- Behavior regression tests

## Out of scope

- Prompt content redesign / product copy rewrite

## References

- https://docs.spring.io/spring-ai/reference/api/prompt.html
