---
name: software-development
description: Clean Code and Clean Architecture Development Practice Guide. Covers naming conventions, functions, comments, error handling, formatting, testing, and layered architecture with Java/TypeScript examples.
version: "1.0"
lastUpdated: "2026-06-20"
---

# Software Development

## When to Use

Consult this skill during day-to-day coding, code review, or refactoring sessions. It provides actionable guidance on writing readable, maintainable code and structuring projects using Clean Architecture principles. Use it when making design decisions, mentoring others, or evaluating code quality.

## Quick Reference

### Clean Code

| Topic | Description |
|-------|-------------|
| [Naming](./references/clean-code-naming.md) | Naming conventions for variables, functions, classes, and modules |
| [Functions](./references/clean-code-functions.md) | Writing small, focused, single-purpose functions |
| [Comments](./references/clean-code-comments.md) | When to comment and when to refactor instead |
| [Error Handling](./references/clean-code-error-handling.md) | Defensive programming and exception strategies |
| [Formatting & Structure](./references/clean-code-formatting-structure.md) | Code layout, vertical/horizontal organization |
| [Tests as Design Feedback](./references/clean-code-tests.md) | Writing tests that drive good design |
| [Code Quality](./references/code-quality.md) | Standards, metrics, tooling, and watchlist for code quality |

### Clean Architecture (Detailed Examples)

| Topic | Description |
|-------|-------------|
| [Domain Layer](./references/clean-architecture-domain-layer.md) | Entities, value objects, aggregates, domain events |
| [Application Layer](./references/clean-architecture-application-layer.md) | Use cases, command/query handlers, DTOs |
| [Infrastructure Layer](./references/clean-architecture-infrastructure-layer.md) | Persistence, external services, adapters |
| [Interface Adapters Layer](./references/clean-architecture-interface-layer.md) | Controllers, presenters, mappers |
| [End-to-End Walkthrough](./references/clean-architecture-end-to-end.md) | Full example from request to persistence |

## How to Use This Skill

1. Identify the topic relevant to your current task from the Quick Reference above.
2. Read the corresponding reference file for patterns, examples, and anti-patterns.
3. For Clean Architecture topics, the reference file may point to deeper examples in subsequent files — drill in as needed.
4. Examples throughout use Java and TypeScript with Bad/Good contrast. Real implementations can be found in the `apps/server/` Java module.

## Companion Skills

- [Software Architecture](../software-architecture/SKILL.md) — Strategic design: bounded contexts, microservices, C4 model, Architecture Decision Records
- [Software Testing](../software-testing/SKILL.md) — Test pyramid, mutation testing, contract testing
- [Spring AI](../spring-ai/SKILL.md) — Spring AI implementation standards with Clean Architecture integration
