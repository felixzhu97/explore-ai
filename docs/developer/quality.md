# Quality

Snapshot of how well each area is protected. Update the row when a PR
changes tests, rules or docs for that area.

Grades: **A** tests + rules + docs current · **B** one gap · **C** two or more gaps.

| Area | Unit tests | Architecture rules | Docs | Grade |
|------|-----------|--------------------|------|-------|
| Backend layering | — | ArchUnit naming, entity mapping, time, controller contract, dependency direction (70 frozen violations) | `AGENTS.md`, C4 | B |
| Chat / sessions | JUnit + Vitest | covered by layer rules | Glossary, C4 | A |
| RAG | JUnit, contract regression | covered by layer rules | Guideline, golden-eval | A |
| MCP / tools / agents | JUnit | covered by layer rules | Guideline | B |
| Frontend | Vitest, Playwright smoke | — | — | B |
| iOS app | XCTest | — | README | C |
