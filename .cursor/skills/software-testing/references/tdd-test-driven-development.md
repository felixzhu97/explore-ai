# TDD Test-Driven Development

A development discipline where tests are written before implementation, driving design from the outside in.

## When to Use

Use TDD when implementing business logic, designing domain models, or adding new features to an existing codebase. TDD is especially valuable when the requirements are clear but the implementation approach is not yet determined. It is less suited for exploratory work, prototyping, or when the problem domain is still poorly understood.

## Core Idea

### Red-Green-Refactor Cycle

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│    ┌─────────┐      ┌─────────┐      ┌─────────┐               │
│    │   RED   │ ───► │  GREEN  │ ───► │ REFACTOR │               │
│    │         │      │         │      │         │               │
│    │ Write   │      │ Minimal │      │ Clean   │               │
│    │ failing │      │ implement│      │ code    │               │
│    │ test    │      │ pass    │      │ keep test│               │
│    └─────────┘      └─────────┘      └─────────┘               │
│         ▲                                      │               │
│         └──────────────────────────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### TDD Steps in Detail

#### Step 1: RED - Write Failing Test

```java
// Business requirement: Free shipping for orders over 100 yuan
class OrderShippingTest {

    @Test
    void shouldBeEligibleForFreeShippingWhenOrderExceeds100Dollars() {
        // Given: An order with amount of 120 yuan
        Order order = new Order(List.of(new OrderLine(Product.of("Item", Money.of(120)), 1)));

        // When: Calculate shipping fee
        Money shippingFee = order.calculateShippingFee();

        // Then: Should be eligible for free shipping
        assertThat(shippingFee).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldChargeShippingFeeWhenOrderBelow100Dollars() {
        // Given
        Order order = new Order(List.of(new OrderLine(Product.of("Item", Money.of(50)), 1)));

        // When
        Money shippingFee = order.calculateShippingFee();

        // Then
        assertThat(shippingFee).isEqualTo(Money.of(10));
    }
}
```

#### Step 2: GREEN - Minimal Implementation

```java
// Minimal implementation to make the test pass
public class Order {
    public Money calculateShippingFee() {
        if (totalAmount().isGreaterThanOrEqual(Money.of(100))) {
            return Money.ZERO;
        }
        return Money.of(10);
    }
}
```

#### Step 3: REFACTOR - Clean Code

```java
// After refactoring
public class Order {
    private static final Money FREE_SHIPPING_THRESHOLD = Money.of(100);
    private static final Money STANDARD_SHIPPING_FEE = Money.of(10);

    public Money calculateShippingFee() {
        return totalAmount().isGreaterThanOrEqual(FREE_SHIPPING_THRESHOLD)
            ? Money.ZERO
            : STANDARD_SHIPPING_FEE;
    }
}
```

### TDD Test Naming Convention

```
should_expected_behavior_when_trigger_condition

Java:     shouldBeEligibleForFreeShippingWhenOrderExceeds100Dollars
Python:   should_be_eligible_for_free_shipping_when_order_exceeds_100_dollars
TypeScript: should_be_eligible_for_free_shipping_when_order_exceeds_100_dollars
```

### TDD Best Practices

- **Test Order**: Order by business value, test critical paths first
- **Minimal Implementation**: Write only the code needed to pass tests, no more
- **Fast Feedback**: Test execution time < 1 second
- **Independent Tests**: No dependencies between tests, can run in parallel
- **Single Assertion Principle**: Each test verifies one behavior (optional)

## Bad/Good Examples

### Good: TDD Cycle Followed

```java
// RED: Write failing test first
@Test
void shouldReturnFullDiscountWhenOrderExceeds1000Yuan() {
    Order order = new Order(List.of(new OrderLine(Product.of("Laptop", Money.of(1500)), 1)));
    Money discount = discountService.calculate(order);
    assertThat(discount).isEqualTo(Money.of(150)); // 10%
}

// GREEN: Minimal pass
public Money calculate(Order order) {
    return Money.of(150); // Hardcoded to pass the test
}

// REFACTOR: Clean implementation
public Money calculate(Order order) {
    if (order.totalAmount().isGreaterThanOrEqual(Money.of(1000))) {
        return order.totalAmount().multiply(0.1);
    }
    return Money.ZERO;
}
```

### Bad: Test After Implementation

```java
// Implementation written first, test is just verification
class OrderTest {
    @Test
    void testCalculateShipping() {
        Order order = new Order();
        // ...
    }
}
```

- No design guidance from tests
- Tests may only cover happy paths
- Refactoring becomes risky without regression safety

## Real Implementation Reference

- `apps/server/src/test/java/com/ai/domain/model/`
- `apps/server/src/test/java/com/ai/application/usecase/`

## Related References

- [Testing Strategy Overview](./testing-strategy-overview.md)
- [BDD Behavior-Driven Development](./bdd-behavior-driven-development.md)
- [Unit Testing](./unit-testing.md)
- [Testing Anti-Patterns](./testing-anti-patterns.md)
- [Software Testing](../SKILL.md)
