# C4 Documentation Best Practices

Guidelines for keeping C4 diagrams accurate, useful, and maintained alongside the codebase.

## When to Use

Apply these practices whenever C4 diagrams are created or updated. Good documentation practices prevent diagrams from becoming stale and losing trust with stakeholders.

## Keep Diagrams Simple

C4 diagrams are communication tools. Overly detailed diagrams defeat the purpose.

### Good Practices

- **Use consistent notation** across all diagrams — a legend on every page
- **Show only what matters** for the target audience — don't show everything
- **Limit the scope** — a diagram showing 3 systems is clearer than one showing 20
- **Use titles** on every diagram so the context is clear without reading surrounding text

### Common Mistakes

| Mistake | Why It Fails |
|---------|-------------|
| Showing all components at Level 2 | Context diagram becomes unreadable |
| Mixing abstraction levels | Reader cannot orient themselves |
| No legend | Notation becomes ambiguous across teams |
| Decorative boxes | Adds noise without information |

## Match Audience to Level

Different audiences need different levels of detail.

| Audience | C4 Level | Content Focus |
|----------|----------|---------------|
| Business stakeholders | Context | Business capabilities, external integrations |
| Product managers | Context + Container | System boundaries, key services |
| Developers, architects | Container + Component | Technology choices, component relationships |
| Team implementing | Component + Code | Class structure, interfaces, dependencies |

**Rule of thumb:** Start at the highest level needed. Add detail only when the audience requires it.

## Update Diagrams with Code

Diagrams become harmful when they diverge from reality. Integrate maintenance into the development workflow.

### CI/CD Integration

```yaml
# .github/workflows/architecture-check.yml
- name: Validate PlantUML diagrams
  run: |
    for diagram in docs/*.puml; do
      plantuml -checkonly "$diagram" || exit 1
    done
```

### Code Annotations

Use code annotations to maintain accuracy:

```java
/**
 * AccountController — see C4 Component diagram: order-component.puml
 * This component handles account lifecycle operations.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    // ...
}
```

### Code Review Checklist

When reviewing architecture changes, ask:
- [ ] Does this change require updating any C4 diagrams?
- [ ] Are the new components correctly placed at the right level?
- [ ] Do the relationships match the actual dependencies?
- [ ] Is the change reflected in the ADR (Architecture Decision Record)?

## Folder Structure for C4 Documentation

Organize C4 diagrams alongside the code they describe:

```
docs/
├── c4/
│   ├── context/
│   │   └── system-context.puml
│   ├── container/
│   │   ├── web-app-container.puml
│   │   └── api-app-container.puml
│   └── component/
│       ├── order-component.puml
│       └── payment-component.puml
└── adrs/
    ├── 001-use-postgresql.md
    └── 002-adopt-cqrs.md
```

## Diagram Review Process

1. **Author creates or updates diagram** alongside the code change
2. **PR reviewer validates** diagram matches implementation
3. **Diagram is auto-generated** from PlantUML templates in CI
4. **ADR created** if the change represents a significant architectural decision

## Related References

- [C4 Four Levels](./c4-four-levels.md) — The four levels these best practices apply to
- [C4 PlantUML Templates](./c4-plantuml-templates.md) — Templates to keep accurate
- [Architecture Decision Records](./architecture-decision-records.md) — How to document architectural choices
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — How C4 layers map to Clean Architecture
- [Architecture Review Checklist](./architecture-review-checklist.md) — Review framework including documentation quality
