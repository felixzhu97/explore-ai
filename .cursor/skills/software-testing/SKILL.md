---
name: software-testing
description: Software Testing Methodology Guide. Covers TDD Test-Driven Development, BDD Behavior-Driven Development, Testing Pyramid, Unit Testing, Integration Testing, E2E Testing Strategies and Practices.
---

# Software Testing

## Testing Strategy Overview

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

## TDD Test-Driven Development

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

## BDD Behavior-Driven Development

### Gherkin Syntax

```gherkin
Feature: Order Free Shipping Calculation
  As a customer
  I want free shipping on orders over 100 yuan
  So that I can reduce shopping costs

  Scenario: Order amount exceeds 100 yuan, enjoy free shipping
    Given my shopping cart has items
      | Item Name | Unit Price | Quantity |
      | Laptop    | 5000       | 1        |
      | Mouse     | 200        | 2        |
    When I submit the order
    Then shipping fee should be 0 yuan
    And I should see message "Order amount exceeds 100 yuan, free shipping"

  Scenario: Order amount below 100 yuan, shipping fee charged
    Given my shopping cart has items
      | Item Name | Unit Price | Quantity |
      | Pencil    | 5          | 3        |
    When I submit the order
    Then shipping fee should be 10 yuan
    And I should see message "Order amount below 100 yuan, shipping fee charged"

  Scenario: Order exactly 100 yuan, enjoy free shipping
    Given my shopping cart has items
      | Item Name | Unit Price | Quantity |
      | Book      | 100        | 1        |
    When I submit the order
    Then shipping fee should be 0 yuan

  Scenario Outline: Shipping fee calculation for different amounts
    Given my shopping cart total is <total>
    When I submit the order
    Then shipping fee should be <shipping_fee>

    Examples:
      | total | shipping_fee |
      | 50    | 10          |
      | 100   | 0           |
      | 150   | 0           |
```

### BDD Implementation Mapping

```java
// Step Definitions
public class OrderShippingSteps {

    private Order order;
    private Money shippingFee;
    private List<OrderLine> cartItems;

    @Given("my shopping cart has items")
    public void given_my_cart_has_items(DataTable dataTable) {
        cartItems = dataTable.asList(OrderLine.class);
        order = new Order(cartItems);
    }

    @When("I submit the order")
    public void when_i_submit_order() {
        shippingFee = order.calculateShippingFee();
    }

    @Then("shipping fee should be {int} yuan")
    public void then_shipping_fee_should_be(int expected) {
        assertThat(shippingFee).isEqualTo(Money.of(expected));
    }

    @And("I should see message {string}")
    public void and_i_should_see_message(String message) {
        assertThat(order.getLastMessage()).isEqualTo(message);
    }
}
```

### Relationship Between BDD and TDD

```
BDD (Acceptance Tests)
    │
    │ Guides
    ▼
TDD (Unit Tests)
    │
    │ Implements
    ▼
Code
```

- **BDD** defines system behavior from an external perspective (What)
- **TDD** implements functionality from an internal perspective (How)
- BDD scenarios are the source of requirements for TDD tests

## Unit Testing

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

## Integration Testing

### Database Integration Testing

```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback(false)  // Set to false when viewing test data
    void shouldPersistAndRetrieveOrder() {
        // Given
        Order order = OrderTestBuilder.builder()
            .withLine(Money.of(100), 2)
            .build();
        order.place();

        // When
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> found = orderRepository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().totalAmount()).isEqualTo(Money.of(200));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void shouldFindOrdersByCustomer() {
        // Given
        Customer customer = createCustomer("test@example.com");
        Order order1 = createOrder(customer);
        Order order2 = createOrder(customer);
        orderRepository.saveAll(List.of(order1, order2));

        // When
        Page<Order> orders = orderRepository.findByCustomer(customer.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(2);
    }
}
```

### API Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderAndReturn201() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(new OrderLineRequest("prod-1", 2))
        );

        // When/Then
        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.totalAmount").value(200.00));
    }

    @Test
    void shouldReturn400WhenOrderIsEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("empty")));
    }
}
```

### Message Queue Integration Testing

```java
@Testcontainers
class OrderEventIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaMessageListenerContainer<String, OrderPlacedEvent> listener;

    @Test
    void shouldReceiveOrderPlacedEvent() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        OrderPlacedEvent receivedEvent = null;

        listener.setupMessageListener(event -> {
            receivedEvent = event;
            latch.countDown();
        });

        // When
        Order order = createPlacedOrder();
        kafkaTemplate.send("order-events", order.getId().toString(),
            new OrderPlacedEvent(order.getId(), order.getTotalAmount()));

        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedEvent.orderId()).isEqualTo(order.getId());
    }
}
```

## E2E Testing

### Playwright Example

```typescript
// e2e/orders.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Order Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('login-btn').click();
    await page.getByLabel('Email').fill('customer@test.com');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
  });

  test('should complete order with free shipping', async ({ page }) => {
    // Given
    await page.getByTestId('product-1').click();
    await page.getByTestId('add-to-cart').click();
    await page.getByTestId('cart-total').waitFor();

    // Verify cart shows correct total
    await expect(page.getByTestId('cart-total')).toHaveText('$5,200');

    // When
    await page.getByTestId('checkout-btn').click();
    await page.getByTestId('shipping-select').selectOption('standard');
    await page.getByTestId('place-order-btn').click();

    // Then
    await expect(page.getByTestId('order-success')).toBeVisible();
    await expect(page.getByTestId('order-total')).toHaveText('$5,200'); // Free shipping
    await expect(page.getByTestId('shipping-fee')).toHaveText('$0.00');
  });

  test('should show shipping fee for orders under $100', async ({ page }) => {
    // Given - Add cheap item
    await page.getByTestId('product-pencil').click();
    await page.getByTestId('add-to-cart').click();

    // When
    await page.getByTestId('checkout-btn').click();

    // Then
    await expect(page.getByTestId('shipping-fee')).toHaveText('$10.00');
    await expect(page.getByTestId('shipping-message')).toContainText('Order amount below 100 yuan');
  });

  test('should handle payment failure gracefully', async ({ page }) => {
    // Given
    await addExpensiveItemToCart(page);
    await page.getByTestId('checkout-btn').click();

    // Enter invalid card
    await page.getByTestId('card-number').fill('4000000000000002'); // Stripe test failure card
    await page.getByTestId('expiry').fill('12/30');
    await page.getByTestId('cvc').fill('123');

    // When
    await page.getByTestId('place-order-btn').click();

    // Then
    await expect(page.getByTestId('payment-error')).toBeVisible();
    await expect(page.getByTestId('payment-error')).toContainText('Card declined');
  });
});
```

## Test Coverage

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

## Test Data Management

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

## Test Review Checklist

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

## Testing Anti-Patterns

| Anti-Pattern                            | Symptoms                              | Correct Approach          |
| --------------------------------------- | ------------------------------------- | ------------------------- |
| **Testing implementation not behavior** | `assertEquals(1, service.getCount())` | Test business value       |
| **Weak assertions**                     | `assertTrue(result)`                  | Precise assertions        |
| **Over-mocking**                        | Mock everything                       | Use Fakes or real objects |
| **Testing private methods**             | `testPrivateMethod()`                 | Test public behavior      |
| **Slow tests**                          | Unit tests access DB                  | Mock dependencies         |
| **Brittle tests**                       | Tests break on implementation changes | Test intent               |
| **Commented out tests**                 | Skipped tests                         | Delete or fix             |
