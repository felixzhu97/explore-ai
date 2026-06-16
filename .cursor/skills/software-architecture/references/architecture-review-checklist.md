# Architecture Review Checklist

A systematic checklist for reviewing software architecture at code-level, design-level, and change-impact level — ensuring the system remains maintainable as it evolves.

## When to Use

Consult this file when conducting a code review focused on architecture quality, reviewing pull requests for design compliance, performing an architecture audit, or evaluating an impact assessment before a significant change.

## Core Idea

Architecture review has three dimensions: **Code-Level** (what the code looks like), **Design-Level** (how the system is organized), and **Change Impact** (where a change needs to go).

### The Three Dimensions

```
┌─────────────────────────────────────────────────────────┐
│              Architecture Review                         │
├─────────────────┬─────────────────┬────────────────────┤
│  Code-Level     │  Design-Level   │  Change Impact     │
│  Review         │  Review         │  Analysis          │
├─────────────────┼─────────────────┼────────────────────┤
│ Is the code     │ Are bounded     │ What modules       │
│ clean per       │ contexts well   │ need modification  │
│ SOLID/CA rules? │ defined?        │ for this change?   │
└─────────────────┴─────────────────┴────────────────────┘
```

## Code-Level Review

Use this checklist when reviewing individual files, classes, or modules.

### Dependency Rules

- [ ] **No circular dependencies** at module/package level
- [ ] **Domain layer has no infrastructure dependencies** — no Spring, JPA, Hibernate imports
- [ ] **Domain layer has no entity/service annotations** — no `@Entity`, `@Service`, `@Component`
- [ ] **Interfaces defined by consumers, not producers** — repository interfaces in Domain, not Infrastructure

### Domain Model Quality

- [ ] **Entities contain business behavior** (not just fields and getters/setters)
- [ ] **Value objects are immutable** — all fields final, no setters
- [ ] **Aggregate boundaries are clear** — access only through aggregate root
- [ ] **Repositories only operate on aggregate roots** — no `save(OrderLine line)`
- [ ] **Domain events published for important state changes**

### Data Flow

- [ ] **Outer layer DTOs are not passed into Domain** — transformation happens at boundary
- [ ] **Application layer contains orchestration, not business logic**
- [ ] **Controllers are thin** — no business logic, only request routing

### Error Handling

- [ ] **Domain exceptions are specific** — not generic `RuntimeException`
- [ ] **Validation happens in domain objects** — not just at the controller
- [ ] **No checked exceptions leaking across layers** (unless intentional)

## Design-Level Review

Use this checklist when reviewing system-level organization, bounded contexts, or service boundaries.

### Bounded Context Division

- [ ] **Bounded context division is reasonable** — each context is cohesive and independently understandable
- [ ] **Context mapping relationships are clear** — shared kernel, customer/supplier, anticorruption layer
- [ ] **Core domain receives sufficient investment** — highest quality code for most valuable parts
- [ ] **Generic domains are identified** — don't over-engineer commodity capabilities

### Architecture Layers

- [ ] **Architecture layers follow dependency rules** — dependencies only point inward
- [ ] **Layer responsibilities are clear** — Domain has no external deps, Application orchestrates, Infrastructure implements
- [ ] **Ports and adapters separation is maintained** — domain doesn't depend on how it's accessed

### Strategic Patterns

- [ ] **Anticorruption layers exist where needed** — contexts that depend on external/legacy systems
- [ ] **Published languages or open host services used for cross-context integration**
- [ ] **Domain events used for cross-context communication** (if applicable)

## Change Impact Analysis

Use this checklist when planning a feature, refactoring, or bug fix.

### Impact Scope

- [ ] **Identified what modules/layers need modification** for the given change
- [ ] **Identified what bounded contexts are affected**
- [ ] **Identified what services need deployment changes** (for microservices)

### Placement Decision

- [ ] **New functionality placed in the correct bounded context**
- [ ] **New entity/value object belongs to correct aggregate**
- [ ] **Business logic in domain, not in application service (unless truly orchestration)**
- [ ] **Repository methods added only for aggregate roots**

### Dependency Considerations

- [ ] **Change does not introduce circular dependencies**
- [ ] **Change does not violate layer boundaries**
- [ ] **Change does not create cross-context direct references**

### Testing Strategy

- [ ] **Domain logic has unit tests** (no framework dependencies)
- [ ] **Application layer has unit/integration tests** (with mocked ports)
- [ ] **Integration tests cover adapter implementations**

## Review Process

### During Code Review

1. Reviewer examines the code structure before reading business logic
2. Check layer responsibilities and dependency directions first
3. Then review business logic within the correct layers
4. Verify aggregate boundaries are respected

### Architecture Review Meeting

1. Present the change and its scope
2. Walk through the design-level checklist together
3. Identify any concerns or trade-offs
4. Document any follow-up ADRs needed
5. Approve or request changes

## Bad/Good Examples

```java
// ❌ BAD: Code-level review failure — Anemic domain model
// Domain entity has no behavior — just fields and getters/setters
public class AnemicOrder {
    private UUID id;
    private List<OrderLine> lines;
    private OrderStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}

// ✅ GOOD: Code-level review pass — Rich domain model
public class Order extends AggregateRoot {
    private final OrderId id;
    private List<OrderLine> lines;
    private OrderStatus status;

    public void place() {
        if (status != OrderStatus.DRAFT) throw new OrderInvalidStateException();
        if (lines.isEmpty()) throw new OrderEmptyException();
        status = OrderStatus.PLACED;
    }
}
```

```java
// ❌ BAD: Design-level review failure — Domain depends on Infrastructure
// Violates dependency rules
package com.ai.domain.model;
import com.ai.infrastructure.persistence.jpa.OrderEntity; // FORBIDDEN!
public class Order { ... }
```

## Common Pitfalls

- **Reviewing business logic before structure**: Always check architecture first — a well-structured but wrong algorithm is easier to fix than a poorly-structured correct one
- **Missing the big picture**: Individual files may look fine, but the interaction between modules may reveal circular dependencies or inappropriate coupling
- **Not updating the checklist**: As the system evolves, add new checklist items for recurring issues
- **Checklist as bureaucracy**: The checklist is a guide, not a mandatory gate. Use judgment — some violations are justified

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/` — Review domain layer for dependency rule compliance. Check that entities follow the rich domain model pattern.

## Related References

- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Dependency rules to verify
- [Entity Pattern](./entity-pattern.md) — Code-level entity review
- [Aggregate Pattern](./aggregate-pattern.md) — Aggregate boundary checks
- [Repository Pattern](./repository-pattern.md) — Repository responsibility checks
- [DDD Strategic Design](./ddd-strategic-design.md) — Bounded context design review
- [Architecture Decision Records](./architecture-decision-records.md) — Document decisions that need review
- [SOLID Principles](./solid-principles.md) — Code-level design principles
- [Software Architecture](../SKILL.md)
