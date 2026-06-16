# Test Coverage

Metrics that measure how much of the codebase is exercised by tests, with guidance on realistic targets per layer.

## When to Use

Use coverage metrics as a guide, not a goal. Measure coverage to identify untested code paths, not to prove quality. High coverage alone does not guarantee well-tested behavior — the intent and precision of assertions matter far more than the percentage of lines executed. Revisit coverage targets when adding new features or refactoring critical paths.

## Core Idea

### Coverage Goals

| Layer             | Coverage Type        | Target    |
| ----------------- | -------------------- | --------- |
| Domain Layer      | Line Coverage        | > 90%     |
| Application Layer | Path Coverage        | > 80%     |
| Infrastructure    | Integration Coverage | Key paths |
| Interface Layer   | Happy path           | 100%      |
| Interface Layer   | Error path           | > 70%     |

### Coverage Pitfalls

- **High coverage ≠ High quality**: False tests can pass coverage checks
- **Focus on test intent**: Do tests verify the correct behavior
- **Boundary conditions**: Coverage reports should focus on uncovered branches

## Common Pitfalls

### Coverage Theater

Teams sometimes write meaningless assertions purely to increase coverage numbers:

```java
// ❌ Coverage theater — executes line but doesn't verify behavior
@Test
void shouldCalculateOrder() {
    Order order = new Order(lines);
    order.totalAmount(); // Call without assertion — line is "covered"
    // No assertThat — the test passes but verifies nothing
}
```

- Line coverage counts the call, not the correctness
- A test without assertions is worse than no test at all

### Chasing a Number

```java
// ❌ Writing tests just to hit 80% coverage
@Test
void shouldHandleEdgeCaseX() { /* trivial */ }
@Test
void shouldHandleEdgeCaseY() { /* trivial */ }
@Test
void shouldHandleEdgeCaseZ() { /* trivial */ }
```

- Forcing a coverage percentage leads to low-value tests
- Focus coverage effort on domain logic and critical paths instead

### Ignoring Uncovered Branches

When coverage reports show uncovered branches, investigate them seriously:

```
Branch coverage report:
  Order.calculateShippingFee()
    Line 45: if (total >= 100) — TRUE  [covered]
    Line 45: if (total >= 100) — FALSE [NOT covered]
```

- An uncovered `false` branch often reveals a missing test case
- Uncovered exception paths are especially important to test

## Real Implementation Reference

- Coverage reports: `apps/server/build/reports/jacoco/test/html/index.html`
- CI coverage gates: `.github/workflows/ci.yml`

## Related References

- [Unit Testing](./unit-testing.md)
- [Integration Testing](./integration-testing.md)
- [Test Review Checklist](./test-review-checklist.md)
- [Testing Anti-Patterns](./testing-anti-patterns.md)
- [Software Testing](../SKILL.md)
