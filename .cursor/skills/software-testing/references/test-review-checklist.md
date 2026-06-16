# Test Review Checklist

A structured checklist for reviewing test quality across readability, reliability, maintainability, and coverage.

## When to Use

Use this checklist when conducting code reviews on test code, performing test suite audits, or self-reviewing your own tests before committing. Run through the relevant sections depending on the type of test being reviewed — unit tests, integration tests, or E2E tests.

## Core Idea

### Test Readability

- [ ] Test name clearly expresses intent
- [ ] Given/When/Then structure is clear
- [ ] No magic numbers (use constants or variables)
- [ ] Assertion messages are helpful

### Test Reliability

- [ ] Tests are deterministic (no random failures)
- [ ] No dependencies between tests
- [ ] Tests behave consistently in local and CI environments
- [ ] Tests clean up their own state

### Test Maintainability

- [ ] Use Test Builder to simplify data construction
- [ ] Test data centrally managed (Fixtures)
- [ ] Avoid duplicated test setup code
- [ ] Test code given same importance as production code

### Test Coverage

- [ ] Core business logic 100% covered
- [ ] Boundary conditions tested
- [ ] Exception paths tested
- [ ] Both happy path and sad path covered

## Common Pitfalls

### Cryptic Test Names

```java
// ❌ Name doesn't reveal intent
@Test
void test1() { ... }

// ✅ Name describes expected behavior
@Test
void shouldApplyTenPercentDiscountForVipCustomers() { ... }
```

### Unclear Arrange Section

```java
// ❌ Magic numbers without explanation
@Test
void shouldCalculateTotal() {
    Order order = new Order(List.of(
        new OrderLine(p, 100),      // What does 100 mean?
        new OrderLine(p, 50)        // And 50?
    ));
    assertThat(order.totalAmount()).isEqualTo(350);
}

// ✅ Named variables explain the intent
@Test
void shouldCalculateTotalAsSumOfLines() {
    int unitPrice = 100;
    int quantity = 2;
    Order order = new Order(List.of(new OrderLine(product, unitPrice, quantity)));
    assertThat(order.totalAmount()).isEqualTo(Money.of(200));
}
```

### Missing Error Path Tests

Reviewers should flag tests that only cover happy paths:

```java
// ❌ Only happy path
@Test
void shouldPlaceOrder() {
    Order order = OrderTestBuilder.builder().withLine(Money.of(100), 1).build();
    order.place();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
}

// ✅ Also covers error paths
@Test
void shouldRejectEmptyOrder() {
    Order order = new Order(List.of());
    assertThrows(OrderEmptyException.class, order::place);
}
```

## Real Implementation Reference

- Apply during code review of: `apps/server/src/test/java/com/ai/`
- E2E review: `apps/admin-ui/e2e/`

## Related References

- [Unit Testing](./unit-testing.md)
- [Integration Testing](./integration-testing.md)
- [Test Data Management](./test-data-management.md)
- [Testing Anti-Patterns](./testing-anti-patterns.md)
- [Test Coverage](./test-coverage.md)
- [Software Testing](../SKILL.md)
