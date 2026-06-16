# Architecture Patterns

Detailed code examples for key architectural patterns are maintained in dedicated files. Each file covers one pattern with Bad/Good code examples, real implementation references, and related links.

## Patterns

Patterns are detailed in dedicated files:

- [Entity Pattern](./entity-pattern.md) — Objects with unique identity and lifecycle
- [Value Object Pattern](./value-object-pattern.md) — Immutable objects whose equality is based on attribute values
- [Aggregate Pattern](./aggregate-pattern.md) — Consistency boundary grouping related entities and value objects
- [Repository Pattern](./repository-pattern.md) — Collection abstraction for aggregates
- [Domain Service Pattern](./domain-service-pattern.md) — Business logic that spans multiple entities
- [Domain Event Pattern](./domain-event-pattern.md) — Important domain occurrences for decoupling
- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Storing events instead of state
- [CQRS Pattern](./cqrs-pattern.md) — Separate Command (write) and Query (read) models
- [Saga Pattern](./saga-pattern.md) — Distributed transaction management via compensating transactions
- [Rich vs Anemic Model](./rich-vs-anemic-model.md) — Comparison of domain model styles
- [Hexagonal Architecture](./hexagonal-architecture.md) — Ports and Adapters architecture style

## See Also

- [Architecture Overview](../SKILL.md) — Top-level index with principles, patterns, C4, and anti-patterns
- [Clean Architecture Deep Dive](../references/clean-architecture-deep-dive.md) — How patterns fit into Clean Architecture layers
- [DDD Strategic Design](../references/ddd-strategic-design.md) — Bounded Contexts and Context Mapping that define aggregate boundaries
