# Clean Code: Tests

Tests are specifications for behavior. Like any specification, they must be precise, complete, and maintainable. Well-written tests serve as executable documentation that verifies the system behaves as intended and protects against regressions as the codebase evolves.

## Why It Matters

Tests are not an afterthought; they are the primary mechanism for verifying that software works as designed. Without tests, there is no reliable way to verify that changes are correct, no safety net for refactoring, and no living documentation of system behavior. Tests that are hard to write often reveal design problems; tests that are hard to read often reveal unclear requirements.

The quality of tests is more important than the quantity. A few well-designed, targeted tests that cover critical behavior are more valuable than hundreds of tests that assert trivial properties or duplicate the implementation.

## Smell Catalog

### FIRST Principles (Fast, Independent, Repeatable, Self-Validating, Timely)

Tests should be Fast (run in milliseconds), Independent (no shared state between tests), Repeatable (same result every time), Self-validating (automatically detect pass or fail), and Timely (written before or with the production code).

#### Bad (Java)

```java
@Test
void testOrderProcessing() {
    // Slow: hits real database
    DatabaseHelper.reset();
    DatabaseHelper.insertCustomer(customer);
    DatabaseHelper.insertOrder(order);

    // Slow: hits real email service
    orderService.processOrder(orderId);

    // Slow: checks actual email inbox
    List<String> emails = emailService.getSentEmails();
    assertTrue(emails.contains("Order confirmed"));
}
```

External dependencies make the test slow and non-repeatable.

#### Good (Java)

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EmailService emailService;

    @Test
    void shouldSendConfirmationEmailWhenOrderIsPlaced() {
        // Fast: mocked dependencies, no I/O
        Order order = createTestOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.processOrder(order.getId());

        // Fast: verify mock interaction
        verify(emailService).sendConfirmation(order.getCustomerEmail());
    }
}
```

Mocks eliminate I/O. The test runs in milliseconds and is repeatable.

#### Bad (TypeScript)

```typescript
describe('OrderService', () => {
  it('processes order', async () => {
    // Slow: real database
    await db.reset();
    await db.insertCustomer(customer);
    await db.insertOrder(order);

    // Slow: real HTTP call
    await orderService.process(orderId);

    // Slow: external service call
    const response = await fetch('https://email.test/inbox');
    const emails = await response.json();
    expect(emails).toContainEqual(expect.objectContaining({ to: customer.email }));
  });
});
```

Real infrastructure makes the test slow, flaky, and environment-dependent.

#### Good (TypeScript)

```typescript
describe('OrderService', () => {
  const orderRepository = createMock<OrderRepository>();
  const emailService = createMock<EmailService>();

  const orderService = new OrderService(orderRepository, emailService);

  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('should send confirmation email when order is placed', async () => {
    const order = createTestOrder();
    orderRepository.findById.mockResolvedValue(order);

    await orderService.process(order.id);

    expect(emailService.sendConfirmation).toHaveBeenCalledWith(order.customerEmail);
  });
});
```

Vitest mocks are fast and isolated. No real infrastructure involved.

### Single Assert Per Concept

Each test should verify one concept or one behavior. When a test fails, it should be immediately clear what failed. Multiple assertions are fine if they verify a single concept; multiple concepts require multiple tests.

#### Bad (Java)

```java
@Test
void testOrder() {
    Order order = new Order();
    order.addLine(product, 2);
    order.addLine(product2, 1);

    // Verifying multiple concepts in one test
    assertEquals(2, order.getLines().size());  // Concept 1: line count
    assertEquals(new Money("300.00"), order.getTotal());  // Concept 2: total calculation
    assertEquals(OrderStatus.DRAFT, order.getStatus());  // Concept 3: initial status
    assertNotNull(order.getId());  // Concept 4: ID generation
}
```

When this test fails, the reason is unclear.

#### Good (Java)

```java
@Test
void shouldHaveCorrectLineCountWhenLinesAdded() {
    Order order = new Order();
    order.addLine(product, 2);
    order.addLine(product2, 1);
    assertEquals(2, order.getLines().size());
}

@Test
void shouldCalculateTotalAsSumOfLineSubtotals() {
    Order order = new Order();
    order.addLine(productA, 2);  // 100 * 2 = 200
    order.addLine(productB, 1);  // 50 * 1 = 50
    assertEquals(new Money("250.00"), order.calculateTotal());
}

@Test
void shouldBeInDraftStatusWhenCreated() {
    Order order = new Order();
    assertEquals(OrderStatus.DRAFT, order.getStatus());
}
```

Each test has one clear reason to fail. The test name describes what is being verified.

#### Bad (TypeScript)

```typescript
it('order behavior', () => {
  const order = createOrder();
  expect(order.lines.length).toBe(2);
  expect(order.total.value).toBe(300);
  expect(order.status).toBe('draft');
  expect(order.id).toBeDefined();
});
```

Multiple concepts bundled together.

#### Good (TypeScript)

```typescript
it('should contain added line items', () => {
  const order = createOrder();
  expect(order.lines).toHaveLength(2);
});

it('should calculate total as sum of line subtotals', () => {
  const order = createOrder();
  expect(order.total).toEqual({ value: 300, currency: 'USD' });
});

it('should have draft status on creation', () => {
  const order = createOrder();
  expect(order.status).toBe('draft');
});
```

Each test verifies a single concept. Test names are self-documenting.

### AAA Pattern (Arrange/Act/Assert)

Organize tests into three distinct phases: Arrange (set up test data and dependencies), Act (execute the behavior under test), Assert (verify the outcome). This structure makes tests scannable and separates concerns.

#### Bad (Java)

```java
@Test
void testPlaceOrder() {
    Order order = new Order(UUID.randomUUID());
    Product product = new Product("Laptop", new Money("1000.00"));
    order.addLine(product, 1);
    orderRepository.save(order);
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    orderService.placeOrder(order.getId());
    Order placed = orderRepository.findById(order.getId()).orElseThrow();
    assertEquals(OrderStatus.PLACED, placed.getStatus());
    verify(emailService).sendConfirmation(any());
}
```

The test mixes setup, action, and verification. The reader must parse the entire test to understand what is being verified.

#### Good (Java)

```java
@Test
void shouldPlaceOrderAndSendConfirmation() {
    // Arrange
    Order order = createOrderWithLines();
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    // Act
    orderService.placeOrder(order.getId());

    // Assert
    Order placed = orderRepository.findById(order.getId()).orElseThrow();
    assertThat(placed.getStatus()).isEqualTo(OrderStatus.PLACED);
    verify(emailService).sendConfirmation(order.getCustomerEmail());
}
```

Each phase is clearly separated. The test reads like a specification.

#### Bad (TypeScript)

```typescript
it('places order', async () => {
  const customer = createCustomer();
  const order = createOrder(customer);
  orderRepository.save.mockResolvedValue(order);
  await orderService.placeOrder(order.id);
  const placed = await orderRepository.findById.mock.results[0].value;
  expect(placed?.status).toBe('placed');
});
```

Setup, action, and assertion are interleaved.

#### Good (TypeScript)

```typescript
it('should place order and send confirmation email', async () => {
  // Arrange
  const order = createOrder();
  orderRepository.findById.mockResolvedValue(order);

  // Act
  await orderService.placeOrder(order.id);

  // Assert
  expect(orderRepository.save).toHaveBeenCalled();
  expect(emailService.sendConfirmation).toHaveBeenCalledWith(order.customerEmail);
});
```

The AAA structure makes the test immediately comprehensible.

### Test Naming Convention (should_X_when_Y)

Test names should describe the expected behavior in terms of the domain, not the implementation. Use the format `should_X_when_Y` or `given_A_and_B_when_C_then_D`. The name should be readable as a sentence that describes the specification.

#### Bad (Java)

```java
@Test
void testPlace() { }

@Test
void testCalc() { }

@Test
void testNull() { }
```

Uninterpretable names.

#### Good (Java)

```java
@Test
void shouldPlaceOrderWhenInDraftStatus() { }

@Test
void shouldThrowExceptionWhenOrderIsAlreadyPlaced() { }

@Test
void shouldApplyTenPercentDiscountForVipCustomersWhenTotalExceeds100Dollars() { }
```

Test names read like specifications. The behavior under test is clear without reading the code.

#### Bad (TypeScript)

```typescript
it('test1', () => { });
it('calc', () => { });
it('null case', () => { });
```

Unhelpful names.

#### Good (TypeScript)

```typescript
it('should place order when status is draft', async () => { });
it('should throw OrderAlreadyPlacedError when status is placed', async () => { });
it('should apply ten percent discount for VIP customers when total exceeds $100', async () => { });
```

Test names are self-documenting specifications.

### Testability-Driven Design

Code that is hard to test often has design problems: tight coupling, hidden dependencies, static coupling, or global state. Testable code is modular, depends on abstractions rather than concretions, and avoids hidden dependencies.

#### Bad (Java)

```java
public class OrderService {
    public void placeOrder(UUID orderId) {
        // Hard-coded dependency: cannot mock
        EmailService service = new GmailEmailService();
        Order order = orderRepository.findById(orderId);
        order.place();
        orderRepository.save(order);
        service.sendConfirmation(order.getCustomerEmail());
    }
}
```

`new` instantiates a concrete dependency. There is no way to substitute a test double.

#### Good (Java)

```java
public class OrderService {
    private final OrderRepository orderRepository;
    private final EmailService emailService;  // Injected abstraction

    public OrderService(OrderRepository orderRepository, EmailService emailService) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.emailService = Objects.requireNonNull(emailService);
    }

    public void placeOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId);
        order.place();
        orderRepository.save(order);
        emailService.sendConfirmation(order.getCustomerEmail());
    }
}
```

Dependencies are injected. Test doubles can replace real implementations.

#### Bad (TypeScript)

```typescript
class OrderService {
  async placeOrder(orderId: string): Promise<void> {
    // Hard-coded dependency: cannot mock
    const db = new PrismaClient();
    const emailService = new SendGridEmailService();
    const order = await db.order.findUnique({ where: { id: orderId } });
    order.status = 'placed';
    await db.order.update({ where: { id: orderId }, data: order });
    await emailService.sendConfirmation(order.customerEmail);
  }
}
```

Concrete dependencies are instantiated directly. No way to substitute test doubles.

#### Good (TypeScript)

```typescript
class OrderService {
  constructor(
    private readonly db: Database,
    private readonly emailService: EmailService
  ) {}

  async placeOrder(orderId: string): Promise<void> {
    const order = await this.db.order.findUnique({ where: { id: orderId } });
    order.status = 'placed';
    await this.db.order.update({ where: { id: orderId }, data: order });
    await this.emailService.sendConfirmation(order.customerEmail);
  }
}
```

Dependencies are injected. The class can be tested with mock implementations.

### Avoid Testing Private Methods

Private methods are implementation details. Testing them directly couples tests to implementation, preventing refactoring. If a private method is complex enough to warrant testing in isolation, it likely should be a separate class with its own tests.

#### Bad (Java)

```java
public class Order {
    private BigDecimal calculateTotal() {
        // Complex calculation logic
        return lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Using reflection to test private method
    @Test
    void testCalculateTotal() throws Exception {
        Method method = Order.class.getDeclaredMethod("calculateTotal");
        method.setAccessible(true);
        BigDecimal result = (BigDecimal) method.invoke(order);
        assertEquals(new BigDecimal("150.00"), result);
    }
}
```

Testing private methods via reflection is fragile and indicates a design problem.

#### Good (Java)

```java
public class Order {
    public BigDecimal calculateTotal() {
        return lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

class OrderTotalCalculationTest {
    @Test
    void shouldCalculateTotalAsSumOfLineSubtotals() {
        Order order = new Order();
        order.addLine(productA, 2);  // 100 * 2 = 200
        order.addLine(productB, 1);  // 50 * 1 = 50
        assertEquals(new BigDecimal("250.00"), order.calculateTotal());
    }
}
```

`calculateTotal()` is public because it is part of the domain contract. Tests exercise it through the public API.

#### Bad (TypeScript)

```typescript
class OrderProcessor {
  private validateOrder(order: Order): boolean {
    return order.lines.length > 0 && order.customerId !== undefined;
  }

  async process(orderId: string): Promise<void> {
    const order = await this.db.findOrder(orderId);
    if (!this.validateOrder(order)) {
      throw new ValidationError('Invalid order');
    }
    // ...
  }
}

// Testing private method via casting
it('validates order', () => {
  const processor = new OrderProcessor(db, email) as any;
  expect(processor.validateOrder(order)).toBe(true);
});
```

Testing private methods exposes implementation details.

#### Good (TypeScript)

```typescript
class OrderProcessor {
  async process(orderId: string): Promise<void> {
    const order = await this.db.findOrder(orderId);
    this.ensureValidOrder(order);
    // ...
  }

  private ensureValidOrder(order: Order): void {
    if (order.lines.length === 0) {
      throw new EmptyOrderError();
    }
    if (!order.customerId) {
      throw new MissingCustomerError();
    }
  }
}

it('should throw EmptyOrderError when order has no lines', async () => {
  const order = createOrder({ lines: [] });
  await expect(processor.process(order.id)).rejects.toThrow(EmptyOrderError);
});
```

The test exercises the public behavior. Private methods are covered indirectly.

### One Concept Per Test

Each test should verify one behavior or one aspect of behavior. When tests are narrow, failures are diagnostic. When tests are broad, failures are confusing and test isolation is lost.

#### Bad (Java)

```java
@Test
void testOrderWorkflow() {
    // Sets up complex state
    Order order = createOrderWithCustomerAndItems();
    // Verifies multiple things
    assertEquals(2, order.getLines().size());
    assertEquals(new Money("500.00"), order.getTotal());
    order.place();
    assertEquals(OrderStatus.PLACED, order.getStatus());
    order.cancel();
    assertEquals(OrderStatus.CANCELLED, order.getStatus());
    assertTrue(order.getCancellationReason().isPresent());
}
```

This test verifies an entire workflow. A failure in any step obscures which step failed.

#### Good (Java)

```java
@Test
void shouldContainAddedLineItems() { /* one assertion */ }

@Test
void shouldCalculateTotalAsSumOfLineSubtotals() { /* one assertion */ }

@Test
void shouldTransitionToPlacedStatusWhenPlaced() { /* one assertion */ }

@Test
void shouldTransitionToCancelledStatusWhenCancelled() { /* one assertion */ }

@Test
void shouldRecordCancellationReasonWhenCancelled() { /* one assertion */ }
```

Each test verifies one concept. Failures are immediately diagnostic.

#### Bad (TypeScript)

```typescript
it('order workflow', async () => {
  const order = createOrderWithItems();
  expect(order.lines).toHaveLength(2);
  expect(order.total).toBe(500);
  await processor.placeOrder(order.id);
  const placed = await processor.getOrder(order.id);
  expect(placed.status).toBe('placed');
  await processor.cancelOrder(order.id);
  const cancelled = await processor.getOrder(order.id);
  expect(cancelled.status).toBe('cancelled');
  expect(cancelled.cancellationReason).toBeDefined();
});
```

Multiple behaviors bundled into one test.

#### Good (TypeScript)

```typescript
it('should contain added line items', () => { /* one assertion */ });
it('should calculate total as sum of line subtotals', () => { /* one assertion */ });
it('should transition to placed status when placeOrder is called', async () => { /* one assertion */ });
it('should transition to cancelled status when cancelOrder is called', async () => { /* one assertion */ });
it('should record cancellation reason when cancelled', async () => { /* one assertion */ });
```

Each test has one clear purpose. Failures are pinpointed.

### Fakes vs Mocks

Use fakes for simplified implementations of dependencies that are expensive or complex to set up in tests (e.g., in-memory databases). Use mocks to verify interactions with dependencies (e.g., that a method was called with specific arguments). Do not use mocks when a fake or a simple stub would suffice.

#### Bad (Java)

```java
@Test
void shouldSaveOrderToRepository() {
    // Overusing mocks for simple behavior
    OrderRepository mockRepo = mock(OrderRepository.class);
    when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OrderService service = new OrderService(mockRepo, emailService);
    service.placeOrder(order);

    verify(mockRepo).save(any());  // Verifying save happened
}
```

A fake would be simpler and more readable. The mock adds no value.

#### Good (Java)

```java
class InMemoryOrderRepository implements OrderRepository {
    private final Map<UUID, Order> store = new HashMap<>();

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Order order) {
        store.put(order.getId(), order);
    }
}

@Test
void shouldSaveOrderToRepository() {
    InMemoryOrderRepository repo = new InMemoryOrderRepository();
    OrderService service = new OrderService(repo, emailService);

    service.placeOrder(order);

    assertTrue(repo.findById(order.getId()).isPresent());
}
```

The fake is simple, deterministic, and reveals no implementation details.

#### Bad (TypeScript)

```typescript
it('should save order to repository', async () => {
  const mockRepo = createMock<OrderRepository>();
  mockRepo.save.mockResolvedValue(order);

  const service = new OrderService(mockRepo, emailService);
  await service.placeOrder(order.id);

  expect(mockRepo.save).toHaveBeenCalled();
});
```

Mock for behavior that a fake could provide.

#### Good (TypeScript)

```typescript
class InMemoryOrderRepository implements OrderRepository {
  private readonly orders = new Map<string, Order>();

  async findById(id: string): Promise<Order | undefined> {
    return this.orders.get(id);
  }

  async save(order: Order): Promise<void> {
    this.orders.set(order.id, order);
  }
}

it('should persist order', async () => {
  const repo = new InMemoryOrderRepository();
  const service = new OrderService(repo, emailService);

  await service.placeOrder(order.id);

  const persisted = await repo.findById(order.id);
  expect(persisted).toBeDefined();
  expect(persisted?.status).toBe('placed');
});
```

The fake is simple and tests the actual persistence behavior.

## Checklist

- [ ] Tests run in milliseconds (no real I/O, mocked dependencies).
- [ ] Tests are independent (no shared state between tests).
- [ ] Tests are repeatable (same result every run).
- [ ] Tests are self-validating (assertions fail clearly on incorrect behavior).
- [ ] Tests follow the AAA pattern: Arrange, Act, Assert.
- [ ] Test names follow `should_X_when_Y` convention.
- [ ] Each test verifies one concept or one behavior.
- [ ] Private methods are not tested directly.
- [ ] Dependencies are injected for testability.
- [ ] Mocks verify interactions; fakes replace complex dependencies.
- [ ] Tests are written before or with production code.

## Cross-References

- [`../code-quality.md`](./code-quality.md) - General code quality principles
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) - Domain layer testability
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) - Application layer testing
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) - Infrastructure layer testing
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) - Interface layer testing
- [`../clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) - End-to-end testing
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) - Strategic design decisions
