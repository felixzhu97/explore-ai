# Unit Testing

Fast, focused tests that verify individual units of code in isolation from their dependencies.

## When to Use

Use unit tests for all business logic, domain models, value objects, and application services. Unit tests are the primary feedback mechanism during development — they run in milliseconds and provide precise guidance on whether a unit behaves correctly. Write them before or alongside implementation (TDD) and maintain them as living documentation of intended behavior.

## Core Idea

### AAA Pattern

```java
class OrderPricingTest {

    @Test
    @DisplayName("Order total should be sum of all line items")
    void shouldCalculateTotalAsSumOfLines() {
        // Arrange: Prepare test data
        List<OrderLine> lines = List.of(
            createLine(Money.of(100), 2),  // 200
            createLine(Money.of(50), 3)    // 150
        );
        Order order = new Order(lines);

        // Act: Execute the operation under test
        Money total = order.totalAmount();

        // Assert: Verify the result
        assertThat(total).isEqualTo(Money.of(350));
    }
}
```

### Test Doubles

| Type      | Purpose                                                | Example                                   |
| --------- | ------------------------------------------------------ | ----------------------------------------- |
| **Dummy** | Fill parameters, not used                              | `new Order(null, null, dummy)`            |
| **Fake**  | Simplified implementation, not suitable for production | `InMemoryUserRepository`                  |
| **Stub**  | Pre-configured responses                               | `when(repo.findById(1)).thenReturn(user)` |
| **Spy**   | Record calls, retain real behavior                     | `Mockito.spy(list).add(1)`                |
| **Mock**  | Verify interactions, verify behavior                   | `verify(repo).save(user)`                 |

```java
// Stub: Pre-configured return values
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void shouldCalculateOrderWithStubbedProducts() {
        // Given: Stub product not found
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        // When
        Order order = orderService.createOrder(List.of(new OrderLineId("p1", 1)));

        // Then
        assertThat(order).isNull();
    }
}

// Mock: Verify interactions
@Test
void shouldSaveOrderWhenPlaced() {
    // Given
    Order order = createValidOrder();
    when(orderRepository.save(any())).thenReturn(order);

    // When
    orderService.placeOrder(order);

    // Then: Verify save was called
    verify(orderRepository).save(order);
    verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
}

// Spy: Record calls but retain real behavior
@Test
void shouldLogWhenOrderPlaced() {
    // Given
    Order order = createValidOrder();
    Logger logger = spy(Logger.class);
    order.setLogger(logger);

    // When
    order.place();

    // Then
    verify(logger).info(contains("Order placed"));
}
```

### Boundary Condition Testing

```java
class OrderEdgeCaseTest {

    @Test
    void shouldThrowWhenOrderHasNoLines() {
        Order order = new Order(List.of());
        assertThrows(OrderEmptyException.class, order::place);
    }

    @Test
    void shouldHandleLargeQuantity() {
        OrderLine line = new OrderLine(product(), 999_999_999);
        Order order = new Order(List.of(line));

        assertThatCode(order::place).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleZeroPriceProduct() {
        OrderLine line = new OrderLine(Product.free(), 1);
        Order order = new Order(List.of(line));

        assertThat(order.totalAmount()).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldHandleNegativeQuantity() {
        assertThrows(IllegalQuantityException.class,
            () -> new OrderLine(product(), -1));
    }
}
```

### Test Data Builders

```java
// Builder pattern to simplify test data construction
class OrderTestBuilder {
    private List<OrderLine> lines = new ArrayList<>();
    private Customer customer = CustomerBuilder.builder().build();
    private OrderStatus status = OrderStatus.DRAFT;

    public static OrderTestBuilder builder() {
        return new OrderTestBuilder();
    }

    public OrderTestBuilder withLine(Money price, int qty) {
        lines.add(new OrderLine(productWithPrice(price), qty));
        return this;
    }

    public OrderTestBuilder withCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public OrderTestBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public Order build() {
        Order order = new Order(customer, lines);
        if (status != OrderStatus.DRAFT) {
            order.setStatus(status);
        }
        return order;
    }

    private Product productWithPrice(Money price) {
        return new Product(ProductId.generate(), "Test", price);
    }
}

// Usage
@Test
void shouldCalculateDiscountForVipCustomers() {
    Customer vipCustomer = CustomerBuilder.builder()
        .withVipStatus(true)
        .build();

    Order order = OrderTestBuilder.builder()
        .withCustomer(vipCustomer)
        .withLine(Money.of(1000), 1)
        .build();

    Money discount = discountService.calculate(order);
    assertThat(discount).isEqualTo(Money.of(100)); // 10% VIP discount
}
```

## Bad/Good Examples

### Good: Focused Unit Test

```java
@Test
void shouldApplyTenPercentDiscountForVipCustomers() {
    // Given
    Customer vipCustomer = CustomerBuilder.builder().withVipStatus(true).build();
    Order order = OrderTestBuilder.builder()
        .withCustomer(vipCustomer)
        .withLine(Money.of(1000), 1)
        .build();

    // When
    Money discount = discountService.calculate(order);

    // Then
    assertThat(discount).isEqualTo(Money.of(100));
}
```

### Bad: Testing Private Methods

```java
// ❌ Testing implementation detail
@Test
void testPrivateHelper() throws Exception {
    Method method = MyService.class.getDeclaredMethod("helper", String.class);
    method.setAccessible(true);
    Object result = method.invoke(service, "input");
    assertEquals("expected", result);
}
```

- Private methods can change freely without breaking tests
- Tests should verify public behavior, not implementation

## Real Implementation Reference

- `apps/server/src/test/java/com/ai/domain/model/`
- `apps/server/src/test/java/com/ai/application/usecase/`
- `apps/server/src/test/java/com/ai/domain/service/`

## Related References

- [TDD Test-Driven Development](./tdd-test-driven-development.md)
- [BDD Behavior-Driven Development](./bdd-behavior-driven-development.md)
- [Integration Testing](./integration-testing.md)
- [Test Data Management](./test-data-management.md)
- [Testing Anti-Patterns](./testing-anti-patterns.md)
- [Software Testing](../SKILL.md)
