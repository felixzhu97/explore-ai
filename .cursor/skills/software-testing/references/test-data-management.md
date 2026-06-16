# Test Data Management

Strategies for constructing, isolating, and maintaining test data across the test suite.

## When to Use

Use test data management strategies from the start of a project and refine them as the codebase grows. Apply builders and fixtures from the first unit test to avoid duplicating construction logic. Use test database isolation techniques whenever tests run against a shared database. Manage test data whenever tests become fragile due to shared state, order dependencies, or data conflicts.

## Core Idea

### Test Data Construction Strategies

```java
// Strategy 1: Builder Pattern
Order order = OrderBuilder.builder()
    .withCustomer(vipCustomer)
    .withLine(Money.of(100), 2)
    .withStatus(OrderStatus.PLACED)
    .build();

// Strategy 2: Fixture Factory
class TestFixtures {
    public static Customer vipCustomer() { ... }
    public static Customer regularCustomer() { ... }
    public static Order placedOrder(Customer customer) { ... }
}

// Strategy 3: Test Data Generator
class OrderGenerator {
    public static Order randomOrder() {
        return OrderBuilder.builder()
            .withLine(randomPrice(), randomQuantity(1, 10))
            .build();
    }
}
```

### Test Database Isolation

```java
// Each test method uses independent transaction
@SpringBootTest
@Transactional
class RepositoryTest {
    @Test
    void test1() {
        repository.save(entity);  // Only visible in test1
    }

    @Test
    void test2() {
        // Cannot see test1's data
        assertThat(repository.findAll()).isEmpty();
    }
}

// Or use test containers
@Testcontainers
class ContainerizedTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

## Bad/Good Examples

### Good: Centralized Fixtures

```java
public class TestFixtures {
    public static Customer vipCustomer() {
        return CustomerBuilder.builder()
            .withVipStatus(true)
            .withName("VIP Test")
            .build();
    }

    public static Order placedOrder(Customer customer) {
        Order order = OrderBuilder.builder()
            .withCustomer(customer)
            .withLine(Money.of(100), 1)
            .build();
        order.place();
        return order;
    }
}

// Tests are clean and consistent
@Test
void shouldApplyDiscountToVipOrders() {
    Customer vip = TestFixtures.vipCustomer();
    Order order = TestFixtures.placedOrder(vip);
    // ...
}
```

### Bad: Inline Data Construction

```java
// ❌ Duplicated setup everywhere
@Test
void test1() {
    Customer c = new Customer();
    c.setId(UUID.randomUUID());
    c.setName("Test");
    c.setVip(true);
    Order o = new Order(c);
    // ...
}

@Test
void test2() {
    // Same setup repeated...
    Customer c = new Customer();
    c.setId(UUID.randomUUID());
    c.setName("Test");
    c.setVip(true);
    // ...
}
```

- Setup code is repeated across many tests
- Changing the test data structure requires updating dozens of places

## Real Implementation Reference

- Builders: `apps/server/src/test/java/com/ai/test/builder/`
- Fixtures: `apps/server/src/test/java/com/ai/test/fixture/`
- Containerized tests: `apps/server/src/test/java/com/ai/integration/`

## Related References

- [Unit Testing](./unit-testing.md)
- [Integration Testing](./integration-testing.md)
- [Test Review Checklist](./test-review-checklist.md)
- [Testing Anti-Patterns](./testing-anti-patterns.md)
- [Software Testing](../SKILL.md)
