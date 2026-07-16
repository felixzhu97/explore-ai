# AI-Explore Project

> ⚠️ 本文件由 `.claude/generate-rules.sh` 自动生成
> 修改规范请编辑 `.cursor/rules/*.mdc`，然后运行此脚本重新生成


<!-- source: .cursor/rules/architecture.mdc -->

# Architecture & Java Core

## Dependency Rule

```
web → application → domain ← infrastructure
```

**Domain has NO dependencies on other layers.**

## Terminology Index (Hexagonal ↔ This Project)

| Hexagonal concept | Hexagonal layout | This project layer | This project package |
|-------------------|------------------|--------------------|----------------------|
| Driving adapter | `adapter/in` | Web + Application | `{module}/web/`, `{module}/application/` |
| Driven adapter | `adapter/out` | Infrastructure | `{module}/infrastructure/` |
| Outbound port | `domain/port` | Repository interface | `{module}/domain/repository/` |
| Domain core | `domain` | Domain | `{module}/domain/model`, `domain/vo`, `domain/service` |
| Composition / wiring | `config` | Spring configuration | `{module}/infrastructure/config/`, `common/config/` |

## Forbidden in New Code

- `domain/port/` — use `domain/repository/` (or `domain/service/`)
- `*Port` interface suffix — prefer `*Repository`, `*Gateway`, or a domain-specific name
- `adapter/in` / `adapter/out` packages — use `web/`, `application/`, `infrastructure/`

## Layers

| Layer | Contains |
|-------|----------|
| `domain/` | Entities, Value Objects, Repository interfaces |
| `application/` | Use Cases, Facades |
| `infrastructure/` | Repository implementations, External adapters |
| `web/` | Controllers, DTOs |

## Project Structure

```
src/main/java/com/ai/
├── {module}/
│   ├── domain/
│   │   ├── model/
│   │   ├── vo/
│   │   └── repository/
│   ├── application/
│   │   └── usecase/
│   ├── infrastructure/
│   │   └── persistence/
│   └── web/
│       ├── controller/
│       └── dto/
└── common/
    └── exception/
```

## Domain Rules

- No framework annotations in domain
- Entities: private constructor + factory method
- Value Objects: immutable `record`
- Repository: interface in domain, implementation in infrastructure

## Java Naming

| Type | Rule | Example |
|------|------|---------|
| Domain Model | PascalCase | `ChatSession` |
| Value Objects | PascalCase | `SessionId` |
| Repository | PascalCase + Repository | `ChatRepository` |
| Use Case | PascalCase + UseCase | `ChatUseCase` |
| Controller | PascalCase + Controller | `ChatController` |
| DTO | PascalCase + Request/Response | `ChatRequest` |
| Methods | camelCase | `findById` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY` |
| Package | lowercase | `com.ai.domain.model` |

## DI / REST / Validation

```java
@Service
@RequiredArgsConstructor
class ChatUseCaseImpl implements ChatUseCase {
    private final ChatRepository repository;
}
```

| Operation | Status |
|-----------|--------|
| Create | 201 |
| Success | 200 |
| No Content | 204 |
| Error | 4xx/5xx |

```java
public record ChatRequest(@NotBlank String message, String sessionId) {}
```

## Test Naming

```
should_expectedResult_when_condition
```

## Checklist

- [ ] Domain has no outward dependencies
- [ ] No circular dependencies
- [ ] Entities encapsulate behavior

## Hard constraints (delivery)

When **creating a Jira ticket**, **branching**, **committing**, or **opening a PR**: always follow [developer](.cursor/skills/developer/SKILL.md) §5 (`<type>/AI-<key>`, Chain PRs, commit/PR templates) and [Product Owner](.cursor/skills/product-owner/SKILL.md). **References** must prefer official documentation and research links ([dependency-docs](.cursor/skills/developer/references/dependency-docs.md), [sources](.cursor/skills/business-tech-analysis/references/sources.md), arXiv).

## Skills (on demand)

| Task | Skill |
|------|-------|
| Feature / tests / commit / Apple UX | [developer](.cursor/skills/developer/SKILL.md) |
| Business + tech strategy | [business-tech-analysis](.cursor/skills/business-tech-analysis/SKILL.md) |
| Spring AI / RAG / tools | [spring-ai](.cursor/skills/spring-ai/SKILL.md) |
| Angular depth | [angular-developer](.cursor/skills/angular-developer/SKILL.md) |
| Product Owner / Jira | [Product Owner](.cursor/skills/product-owner/SKILL.md) |

<!-- Generated at Thu Jul 16 11:49:05 CST 2026 -->
