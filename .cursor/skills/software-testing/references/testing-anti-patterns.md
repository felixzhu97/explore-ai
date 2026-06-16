# Testing Anti-Patterns

Common mistakes that reduce test value, increase maintenance burden, or create false confidence.

## When to Use

Use this reference when diagnosing flaky, brittle, or low-value tests. When a test suite is hard to maintain, slow, or provides little confidence, cross-check the symptoms against the anti-patterns below. Apply the corrective approaches during refactoring sessions or when reviewing test code.

## Core Idea

### Anti-Patterns and Correct Approaches

| Anti-Pattern                            | Symptoms                              | Correct Approach          |
| --------------------------------------- | ------------------------------------- | ------------------------- |
| **Testing implementation not behavior** | `assertEquals(1, service.getCount())` | Test business value       |
| **Weak assertions**                     | `assertTrue(result)`                  | Precise assertions        |
| **Over-mocking**                        | Mock everything                       | Use Fakes or real objects |
| **Testing private methods**             | `testPrivateMethod()`                 | Test public behavior      |
| **Slow tests**                          | Unit tests access DB                  | Mock dependencies         |
| **Brittle tests**                       | Tests break on implementation changes | Test intent               |
| **Commented out tests**                 | Skipped tests                         | Delete or fix             |

## Common Pitfalls

### Testing Implementation Details

```java
// ❌ Fragile — breaks when internal structure changes
@Test
void shouldHaveThreeLines() {
    assertEquals(3, order.getLines().size());
}

// ✅ Stable — verifies business contract
@Test
void shouldCalculateTotalAsSumOfAllLineSubtotals() {
    Money total = order.totalAmount();
    Money expected = order.getLines().stream()
        .map(OrderLine::subtotal)
        .reduce(Money.ZERO, Money::add);
    assertThat(total).isEqualTo(expected);
}
```

### Weak Assertions

```java
// ❌ Too vague — passes even when behavior is wrong
@Test
void shouldProcessOrder() {
    order.process();
    assertNotNull(order.getResult()); // What exactly should result be?
}

// ✅ Precise — clearly defines expected outcome
@Test
void shouldSetOrderStatusToPlacedAndPersistToRepository() {
    order.process();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    verify(orderRepository).save(order);
}
```

### Over-Mocking

```java
// ❌ Mocking everything — test loses connection to reality
@Test
void shouldSendEmail() {
    when(emailService.send(any())).thenReturn(true);
    when(clock.now()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
    when(userRepository.findById(any())).thenReturn(user);

    service.notifyUser(userId);

    verify(emailService).send(any());
}
```

```java
// ✅ Use real objects for simple, fast dependencies
@Test
void shouldSendEmail() {
    EmailService emailService = new RealEmailService(); // Uses test SMTP
    UserRepository userRepo = new InMemoryUserRepository(); // Fake

    service = new NotificationService(emailService, userRepo, clock);

    service.notifyUser(userId);

    // Verify real behavior
    assertThat(testSmtpServer.receivedMessages()).hasSize(1);
}
```

### Slow Unit Tests

```java
// ❌ Database access in unit test — makes suite slow
@Test
void shouldFindOrder() {
    OrderRepository repo = new JdbcOrderRepository(dataSource);
    Optional<Order> found = repo.findById(orderId);
    assertThat(found).isPresent();
}
```

```java
// ✅ Use in-memory fake for unit tests
@Test
void shouldFindOrder() {
    OrderRepository repo = new InMemoryOrderRepository();
    repo.save(testOrder);

    Optional<Order> found = repo.findById(testOrder.getId());
    assertThat(found).isPresent();
}
```

## Real Implementation Reference

- Use as diagnostic reference when refactoring tests in: `apps/server/src/test/java/com/ai/`

## Related References

- [Unit Testing](./unit-testing.md)
- [Test Review Checklist](./test-review-checklist.md)
- [Test Coverage](./test-coverage.md)
- [Test Data Management](./test-data-management.md)
- [Software Testing](../SKILL.md)
