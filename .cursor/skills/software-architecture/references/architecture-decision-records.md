# Architecture Decision Records

A lightweight practice for documenting significant architectural decisions — capturing the context, decision, and consequences so future maintainers understand the "why".

## When to Use

Consult this file when making a significant architectural choice (new pattern, framework, data model, protocol), when onboarding team members who need to understand past decisions, or when reviewing whether a decision is still valid.

## Core Idea

An Architecture Decision Record (ADR) is a short document that captures an important architectural decision: what was decided, why it was decided, and what consequences followed. ADRs are stored in the repository alongside the code so they stay in sync with the system.

### ADR Lifecycle

1. **Proposed**: Someone identifies a decision to make. A draft ADR is created.
2. **Accepted**: The team discusses and approves the ADR.
3. **Deprecated**: A newer ADR supersedes it.
4. **Superseded**: Replaced by another ADR (links to the replacement).

## Standard ADR Template

```markdown
# ADR-001: Designing Order Aggregate Using Rich Domain Model

## Status
Accepted

## Context
Order business logic is scattered across OrderService and various places.

## Decision
Adopt Rich Domain Model, encapsulate order state changes and business rules inside the Order entity.

## Consequences
- Order state machine fully encapsulated
- Business rules cohesive within domain objects
- Easy to unit test

## Drawbacks
- Team needs to learn DDD Rich Domain Model
- Aggregate design requires careful review
```

### Section Definitions

| Section | Purpose |
|---------|---------|
| **Status** | Current state: Proposed, Accepted, Deprecated, Superseded |
| **Context** | What forced this decision? Constraints, problems, options considered |
| **Decision** | What was decided? The choice made |
| **Consequences** | Positive outcomes after implementing the decision |
| **Drawbacks** | Negative outcomes, trade-offs, risks introduced |
| **Alternatives Considered** | (Optional) Other options that were rejected and why |

## When to Write an ADR

Write an ADR when the decision:

- Affects multiple teams or services
- Is hard to reverse (e.g., database schema, language choice)
- Has significant consequences (performance, security, maintainability)
- Was non-obvious (a senior developer might question it without context)
- Required significant research or trade-off analysis

Do NOT write an ADR for:
- Routine coding conventions
- Minor refactorings with obvious outcomes
- Decisions that can be easily reversed

## ADR Numbering

Use sequential numbers: `ADR-001`, `ADR-002`, etc. Never reuse numbers. When an ADR is superseded, keep it with a link to the replacement.

## ADR Storage

Store ADRs in the repository:

```
docs/
└── adr/
    ├── ADR-001-rich-domain-model.md
    ├── ADR-002-cqrs-for-reports.md
    └── ADR-003-event-sourcing-for-audit.md
```

Or in the project root:

```
adr/
├── 001-rich-domain-model.md
├── 002-cqrs-for-reports.md
└── 003-event-sourcing-for-audit.md
```

## Bad/Good Examples

```markdown
<!-- ❌ BAD: Vague ADR with no context -->
# ADR-042: Use PostgreSQL

## Decision
We will use PostgreSQL.

## Consequences
Better data integrity.
```

```markdown
<!-- ✅ GOOD: Clear context, decision, and trade-offs -->
# ADR-042: Choose PostgreSQL over MongoDB for Order Storage

## Status
Accepted

## Context
We need to store order data. Two options were evaluated:
- PostgreSQL: ACID compliance, relational schema, rich indexing
- MongoDB: Flexible schema, horizontal scaling, JSON-native

Our team has strong PostgreSQL expertise. Current data model is relational.

## Decision
Use PostgreSQL with the following schema:
- orders table (id, customer_id, status, created_at)
- order_lines table (id, order_id, product_id, quantity, price)

## Consequences
- ACID transactions ensure order consistency
- Complex queries (joins, aggregations) are straightforward
- Team can leverage existing PostgreSQL knowledge

## Drawbacks
- Horizontal scaling requires sharding (not needed now, but complicates future scaling)
- Schema migrations require careful planning
- JSONB used for extensible attributes, but MongoDB would handle this more naturally

## Alternatives Considered
- MongoDB: Rejected due to team expertise gap and relational nature of data model
- DynamoDB: Rejected due to vendor lock-in and complex pricing model
```

## Common Pitfalls

- **Writing ADRs for everything**: Only document significant decisions. Routine choices don't need ADRs.
- **Writing ADRs after the fact**: Create ADRs during the decision-making process, not after. The act of writing clarifies thinking.
- **No context**: Future readers won't understand the trade-offs without knowing what options were considered.
- **Stale ADRs**: Mark ADRs as deprecated when decisions change. Don't silently update old ADRs.
- **ADRs without teeth**: If an ADR is ignored in practice, it provides false confidence.

## ADR vs. Other Documentation

| Document Type | Purpose | Granularity | When |
|---------------|--------|-------------|------|
| ADR | Capture why a decision was made | One decision | During or after decision |
| Architecture Decision Log | Running list of decisions | One decision per entry | During development |
| Design Document | Deep dive into a system/component | Multiple decisions | Before implementation |
| RFC | Proposal for change, open for discussion | Feature/system | Before decision |
| Post-mortem | Retrospective on an incident/decision outcome | Decision + outcome | After implementation |

## Tools

- **adr-tools**: CLI for managing ADRs (`adr new`, `adr list`, `adr link`)
- **MADR**: Markdown Any Decision Records — standardized ADR template
- **ADR-tools**: Git-based ADR management

## Real Implementation Reference

`docs/adr/` or `.github/adr/` in the project root.

## Related References

- [Architecture Review Checklist](./architecture-review-checklist.md) — Where ADRs fit into architecture review
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Architecture decisions that warrant ADRs
- [DDD Strategic Design](./ddd-strategic-design.md) — Strategic decisions about bounded contexts
- [Software Architecture](../SKILL.md)
