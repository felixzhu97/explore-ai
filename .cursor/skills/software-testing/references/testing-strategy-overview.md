# Testing Strategy Overview

A layered testing strategy that balances speed, reliability, and confidence through a well-structured testing pyramid.

## When to Use

Use this testing strategy as the foundation for any project's test suite. Apply it when designing a new test suite, auditing an existing one, or deciding where to invest testing effort. It guides how many tests to write at each layer and what kinds of guarantees each layer provides.

## Core Idea

### Testing Pyramid

```
                    ▲
                   /E\
                  /2E \      E2E Tests (Few, Slow, Expensive)
                 /-----\        - Complete cross-system flow
                / Inte \       - Critical user paths
               /gration\         - Single source of trust
              /---------\
             /  Unit   \       Integration Tests
            /  Tests   \         - Component collaboration
           /------------\         - Database, message queues
          /              \       - Inter-service interaction
         ▼◄───────────────▼◄
              Trust Level
         ──────────────────────
         ▲                      ▲
        High                   Low
```

### Testing Layer Principles

| Layer             | Percentage | Speed   | Scope                   | Trust Level                |
| ----------------- | ---------- | ------- | ----------------------- | -------------------------- |
| Unit Tests        | 70%        | < 1ms   | Single class/method     | High                       |
| Integration Tests | 20%        | < 100ms | Component collaboration | Medium                     |
| E2E Tests         | 10%        | < 10s   | Complete system         | Low (high false positives) |

## Bad/Good Examples

### Bad: Skewed Pyramid

```
         ▲
        /E\        50 E2E tests
       /---\       (Slow, flaky, expensive to maintain)
      /     \
     ▼───────▼      Unit tests: 20
```

- Too many E2E tests lead to slow CI pipelines and fragile builds
- High maintenance cost, low confidence per test

### Good: Well-Balanced Pyramid

```
         ▲
        /E\        5 E2E tests (critical user journeys only)
       /---\       (Full system smoke tests)
      /Integ\
     /ration\     20 Integration tests (API + DB)
    /--------\
   /  Unit   \   100 Unit tests (domain logic, edge cases)
  /  Tests   \
 /------------\
```

- Unit tests provide fast, reliable feedback
- Integration tests verify component collaboration
- E2E tests cover only the most critical paths

## Real Implementation Reference

- Unit tests: `apps/server/src/test/java/com/ai/`
- Integration tests: `apps/server/src/test/java/com/ai/integration/`
- E2E tests: `apps/admin-ui/e2e/` or `apps/web/e2e/`

## Related References

- [TDD Test-Driven Development](./tdd-test-driven-development.md)
- [BDD Behavior-Driven Development](./bdd-behavior-driven-development.md)
- [Unit Testing](./unit-testing.md)
- [Integration Testing](./integration-testing.md)
- [E2E Testing](./e2e-testing.md)
- [Software Testing](../SKILL.md)
