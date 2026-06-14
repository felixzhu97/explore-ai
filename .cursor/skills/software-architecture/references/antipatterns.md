# Anti-Patterns

Common architectural anti-patterns and their remedies, with code-level examples showing the problem and the fix.

## Code-Level Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Anemic Domain Model** | Entities only have getters/setters, no behavior | Business logic scattered in services, hard to test | Move business logic into domain objects |
| **God Class/Object** | Single class with too many responsibilities | Hard to understand, test, or reuse | Split into smaller, focused classes |
| **Shotgun Surgery** | One change requires modifying many classes | High coupling, fragile code | Use Extract Class, move behavior closer to data |
| **Feature Envy** | Class accesses other class's data more than its own | Tight coupling, poor cohesion | Move method to the class it envies |
| **Data Clump** | Same group of data fields appear together repeatedly | Duplication, inconsistency | Extract into a class/record |
| **Primitive Obsession** | Using primitives instead of domain types | Loss of type safety, validation scattered | Create value objects for domain concepts |
| **Long Method** | Methods that are too long | Hard to read, test, reuse | Extract smaller methods |
| **Switch Statements** | Repeated switch/case or if-else chains | Adding cases requires changing many places | Use polymorphism or strategy pattern |

### Common Examples

#### Anemic Domain Model

```java
// Bad: Anemic Domain Model — only getters/setters, all logic in service
public class AnemicOrder {
    private UUID id;
    private List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}

public class OrderService {
    public void placeOrder(AnemicOrder order) {
        if (order.getLines().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be placed");
        }
        order.setStatus(OrderStatus.PLACED);
        // Validation, domain events, persistence — all in service
    }
}
```

```java
// Good: Rich Domain Model — behavior encapsulated in the entity
public class Order extends AggregateRoot {
    private final OrderId id;
    private CustomerId customerId;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;

    private Order(CustomerId customerId) {
        this.id = OrderId.generate();
        this.customerId = customerId;
        this.status = OrderStatus.DRAFT;
    }

    public static Order create(CustomerId customerId) {
        return new Order(customerId);
    }

    public void place() {
        if (lines.isEmpty()) {
            throw new OrderEmptyException();
        }
        if (status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException("Only draft orders can be placed");
        }
        this.status = OrderStatus.PLACED;
        addDomainEvent(new OrderPlacedEvent(this.id, this.customerId, calculateTotal(), Instant.now()));
    }

    public Money calculateTotal() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }

    private void addDomainEvent(DomainEvent event) { /* ... */ }
}
```

#### God Class

```java
// Bad: God Class — handles everything: validation, persistence, email, logging, business logic
public class CustomerManager {
    private EntityManager em;
    private EmailService email;
    private AuditLogger logger;

    public void createCustomer(String name, String email, String phone,
                               String address, String city, String zip,
                               String billingInfo, String preferences) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name required");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("Invalid email");
        // ... 200 lines of validation, persistence, email, logging
    }

    public void updateCustomer(Long id, String name, String email, String phone,
                               String address, String city, String zip,
                               String billingInfo, String preferences) {
        // Duplicate validation and logic
    }

    public void deleteCustomer(Long id) {
        // Delete logic with email notification
    }
}
```

```java
// Good: Split into focused classes following Single Responsibility
public record CustomerName(String value) {
    public CustomerName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
    }
}

public record EmailAddress(String value) {
    public EmailAddress {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Valid email address is required");
        }
    }
}

public class Customer extends AggregateRoot {
    private final CustomerId id;
    private CustomerName name;
    private EmailAddress email;
    private PhoneNumber phone;
    private Address address;

    public void updateContactInfo(CustomerName name, EmailAddress email, PhoneNumber phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public void updateAddress(Address address) {
        this.address = address;
    }
}

public class CustomerRepository {
    public Optional<Customer> findById(CustomerId id) { /* ... */ }
    public void save(Customer customer) { /* ... */ }
    public void delete(CustomerId id) { /* ... */ }
}

public class CustomerApplicationService {
    private final CustomerRepository repository;
    private final DomainEventPublisher eventPublisher;

    public void createCustomer(CreateCustomerCommand command) {
        Customer customer = Customer.create(command.name(), command.email());
        repository.save(customer);
        eventPublisher.publish(customer.pullDomainEvents());
    }
}
```

#### Switch Statements

```java
// Bad: Repeated switch statements scattered across codebase
public class OrderDiscountCalculator {
    public Money calculateDiscount(Order order, CustomerType type, Season season) {
        switch (type) {
            case VIP -> {
                return order.getTotal().multiply(0.15);
            }
            case REGULAR -> {
                switch (season) {
                    case BLACK_FRIDAY -> { return order.getTotal().multiply(0.20); }
                    case HOLIDAY -> { return order.getTotal().multiply(0.10); }
                    default -> { return Money.ZERO; }
                }
            }
            case NEW -> {
                return order.getTotal().multiply(0.05);
            }
        }
    }
}
```

```java
// Good: Polymorphism — each strategy encapsulates its own discount logic
public sealed interface DiscountStrategy permits VipDiscount, RegularDiscount, NewCustomerDiscount {
    Money calculateDiscount(Order order);
}

public final class VipDiscount implements DiscountStrategy {
    private static final BigDecimal VIP_RATE = new BigDecimal("0.15");

    @Override
    public Money calculateDiscount(Order order) {
        return order.getTotal().multiply(VIP_RATE);
    }
}

public final class RegularDiscount implements DiscountStrategy {
    private final Season season;

    public RegularDiscount(Season season) { this.season = season; }

    @Override
    public Money calculateDiscount(Order order) {
        BigDecimal rate = switch (season) {
            case BLACK_FRIDAY -> new BigDecimal("0.20");
            case HOLIDAY -> new BigDecimal("0.10");
            default -> BigDecimal.ZERO;
        };
        return order.getTotal().multiply(rate);
    }
}

public final class NewCustomerDiscount implements DiscountStrategy {
    private static final BigDecimal NEW_CUSTOMER_RATE = new BigDecimal("0.05");

    @Override
    public Money calculateDiscount(Order order) {
        return order.getTotal().multiply(NEW_CUSTOMER_RATE);
    }
}

public class DiscountPolicy {
    private final Map<CustomerType, DiscountStrategy> strategies;

    public DiscountPolicy(Map<CustomerType, DiscountStrategy> strategies) {
        this.strategies = strategies;
    }

    public Money apply(Order order, CustomerType type) {
        return strategies.get(type).calculateDiscount(order);
    }
}
```

---

## Architecture-Level Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Circular Dependency** | A → B → C → A | Changes cascade unpredictably, hard to test | Introduce abstractions, extract common interface |
| **Premature Abstraction** | Unnecessary interfaces, indirection | Over-engineering, harder to understand | Follow YAGNI, add abstraction when needed |
| **Golden Hammer** | Applying one solution to all problems | Wrong tool for the job | Understand trade-offs of different approaches |
| **Architecture Astronaut** | Overly complex architecture for simple needs | Excessive complexity, slow development | Start simple, evolve as needed |
| **Lava Flow** | Old, unmaintainable code still in production | Risk when changing, hard to remove | Incrementally refactor, document unknowns |
| **Big Ball of Mud** | No clear architecture, everything mixed | Chaos, high maintenance cost | Define bounded contexts, isolate changes |

### Common Examples

#### Circular Dependency

```java
// Bad: Circular dependency between domain classes
public class Order {
    private Customer customer;

    public void setCustomer(Customer customer) { this.customer = customer; }
    public Customer getCustomer() { return customer; }
}

public class Customer {
    private List<Order> orders = new ArrayList<>();

    public void addOrder(Order order) { orders.add(order); }
    public List<Order> getOrders() { return orders; }
}
```

```java
// Good: Use ID references across aggregate boundaries, not direct object references
public class Order extends AggregateRoot {
    private final CustomerId customerId;  // Reference by ID, not object

    public CustomerId getCustomerId() { return customerId; }
}

public class Customer extends AggregateRoot {
    private final List<OrderId> orderIds = new ArrayList<>();  // References by ID

    public void addOrderReference(OrderId orderId) { orderIds.add(orderId); }
    public List<OrderId> getOrderIds() { return List.copyOf(orderIds); }
}
```

#### Premature Abstraction

```java
// Bad: Interface for every class before any variation exists
public interface IOrderRepository { void save(Order o); }
public interface IOrderService { Money calculate(Order o); }
public interface IOrderValidator { boolean validate(Order o); }

public class OrderRepository implements IOrderRepository { /* ... */ }
public class OrderService implements IOrderService { /* ... */ }
public class OrderValidator implements IOrderValidator { /* ... */ }
```

```java
// Good: Start with concrete classes, introduce interfaces when variation is needed
public class OrderRepository {
    public void save(Order order) { /* single implementation */ }
}

// Introduce abstraction only when a second implementation emerges
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}

public class JpaOrderRepository implements OrderRepository { /* ... */ }
public class InMemoryOrderRepository implements OrderRepository { /* used only in tests */ }
```

#### Big Ball of Mud

```java
// Bad: Everything in one package with no layering
// com.example.app/
//   OrderService.java
//   CustomerService.java
//   Validator.java
//   Repository.java
//   Entity.java
//   DTO.java
//   Controller.java
//   Helper.java  ← nobody knows what this does

public class EverythingService {
    public void processOrder(String data) {
        // Validation here
        // Business logic here
        // Database access here
        // Email sending here
        // Logging here
        // If something fails, retry logic here
    }
}
```

```java
// Good: Clear Clean Architecture layers
// com.ai.domain.model.Order.java
// com.ai.domain.repository.OrderRepository.java
// com.ai.application.usecase.PlaceOrderUseCase.java
// com.ai.infrastructure.persistence.JpaOrderRepository.java
// com.ai.interface.controller.OrderController.java

public class PlaceOrderUseCase {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public OrderResult execute(PlaceOrderCommand command) {
        Order order = Order.create(command.customerId());
        command.items().forEach(order::addLine);
        order.place();
        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
        return OrderResult.from(order);
    }
}
```

---

## Layering Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Anemic Domain Model** | Domain layer has no logic, all in services | Violates Clean Architecture | Move behavior into domain entities |
| **Smart UI** | Business logic in controllers/presenters | Hard to test, tied to UI framework | Move logic to domain layer |
| **Gateway Agglomeration** | One gateway wrapping multiple services | Tight coupling, single point of change | Split into focused adapters |
| **DTO Overdose** | Excessive DTOs, no domain objects | All logic in transformation | Use domain objects internally |
| **Leaking Abstractions** | Domain depends on infrastructure | Violates dependency rules | Define interfaces in domain |

### Common Examples

#### Smart UI — Business Logic in Controller

```java
// Bad: Controller contains business logic
@RestController
public class OrderController {
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // Business logic in controller
        if (request.items().isEmpty()) {
            throw new BadRequestException("Order must have items");
        }
        Money total = Money.ZERO;
        for (OrderItemRequest item : request.items()) {
            Product product = productRepository.findById(item.productId());
            if (product == null) throw new NotFoundException("Product not found");
            Money lineTotal = product.getPrice().multiply(item.quantity());
            total = total.add(lineTotal);
        }
        if (total.isGreaterThan(Money.of(10000)) && !customer.isVip()) {
            throw new BadRequestException("Order exceeds limit for non-VIP");
        }
        Order order = new Order(request.customerId(), request.items());
        orderRepository.save(order);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

```java
// Good: Controller delegates to use case, domain handles logic
@RestController
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = OrderMapper.toCommand(request);
        OrderResult result = createOrderUseCase.execute(command);
        return ResponseEntity.ok(OrderResponse.from(result));
    }
}

public class CreateOrderUseCase {
    public OrderResult execute(CreateOrderCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> CustomerException.notFound(command.customerId()));
        Order order = Order.create(customer.getId());
        for (OrderItemCommand item : command.items()) {
            Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> ProductException.notFound(item.productId()));
            order.addLine(product, item.quantity());
        }
        order.place();
        orderRepository.save(order);
        return OrderResult.from(order);
    }
}
```

#### Leaking Abstractions — Domain Depends on Infrastructure

```java
// Bad: Domain entity depends on JPA annotations
package com.ai.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLine> lines;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}
```

```java
// Good: Domain entity is pure Java, JPA entity is in infrastructure layer
// Domain layer — no framework dependencies
public class Order extends AggregateRoot {
    private final OrderId id;
    private CustomerId customerId;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;

    public void place() {
        if (lines.isEmpty()) throw new OrderEmptyException();
        if (status != OrderStatus.DRAFT) throw new OrderInvalidStateException();
        this.status = OrderStatus.PLACED;
    }
}

// Infrastructure layer — JPA entity
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    private String status;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLineEntity> lines;

    public Order toDomain() { /* mapping logic */ }
    public static OrderEntity fromDomain(Order order) { /* mapping logic */ }
}
```

---

## Database Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Single Table Inheritance** | One table for entire class hierarchy | Null columns, complex queries | Separate tables per class |
| **Surrounding the Transaction** | Business logic tied to transaction scope | Hard to test, inflexible | Separate transaction boundaries |
| **Query in Loop** | N+1 queries | Performance degradation | Use JOIN or batch queries |
| **Database as Queue** | Using tables for message queuing | Lost updates, race conditions | Use proper message broker |

### Common Examples

#### Query in Loop (N+1 Problem)

```java
// Bad: N+1 queries — one query per order line to fetch product details
public List<OrderLineDTO> getOrderLines(UUID orderId) {
    Order order = orderRepository.findById(orderId);
    List<OrderLineDTO> result = new ArrayList<>();
    for (OrderLine line : order.getLines()) {
        // Each call triggers a database query!
        Product product = productRepository.findById(line.getProductId());
        result.add(new OrderLineDTO(product.getName(), line.getQuantity(), product.getPrice()));
    }
    return result;
}
```

```java
// Good: Batch query — fetch all products in one query
public List<OrderLineDTO> getOrderLines(UUID orderId) {
    Order order = orderRepository.findById(orderId);
    List<ProductId> productIds = order.getLines().stream()
        .map(OrderLine::getProductId)
        .toList();
    Map<ProductId, Product> productMap = productRepository.findByIds(productIds)
        .stream()
        .collect(Collectors.toMap(Product::getId, p -> p));
    return order.getLines().stream()
        .map(line -> {
            Product product = productMap.get(line.getProductId());
            return new OrderLineDTO(product.getName(), line.getQuantity(), product.getPrice());
        })
        .toList();
}
```

#### Single Table Inheritance

```java
// Bad: One table with nullable columns for all Payment types
// Table: payments
// Columns: id, type, credit_card_number, bank_account_id,
//          crypto_wallet_address, refund_reason, refund_amount, ...
@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id private UUID id;
    private String type;  // "CREDIT_CARD", "BANK_TRANSFER", "CRYPTO", "REFUND"
    // Credit card fields
    private String cardNumber;
    private String expiryDate;
    // Bank transfer fields
    private String bankAccountId;
    // Crypto fields
    private String walletAddress;
    // Refund fields
    private String refundReason;
    private BigDecimal refundAmount;
    // 50+ more nullable columns for different payment types...
}
```

```java
// Good: Separate tables per payment type
@Entity
@Table(name = "credit_card_payments")
public class CreditCardPaymentEntity {
    @Id private UUID id;
    private String cardNumber;
    private String expiryDate;
    private Money amount;
    private Instant authorizedAt;
}

@Entity
@Table(name = "bank_transfer_payments")
public class BankTransferPaymentEntity {
    @Id private UUID id;
    private String bankAccountId;
    private String routingNumber;
    private Money amount;
    private Instant transferAt;
}

@Entity
@Table(name = "refunds")
public class RefundEntity {
    @Id private UUID id;
    private UUID originalPaymentId;
    private Money refundAmount;
    private String reason;
    private Instant refundedAt;
}
```

---

## Microservices Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Microservice Premium** | Breaking monolith into too many services | Complexity explosion | Start with bounded contexts |
| **Shared Database** | Multiple services sharing same DB | Tight coupling, deployment issues | Extract to separate databases |
| **Chatty Services** | Excessive service-to-service calls | Latency, cascading failures | Batch operations, use events |
| **Nanoservices** | Over-granular service decomposition | Too many moving parts | Consolidate related functionality |
| **Ignore Failure** | No circuit breaker, retry logic | Cascading failures | Implement resilience patterns |

### Common Examples

#### Chatty Services

```java
// Bad: Synchronous call chain — each service calls the next
public class OrderService {
    public void placeOrder(Order order) {
        // 10 sequential calls across 10 services
        Customer customer = customerClient.getCustomer(order.getCustomerId());  // 1
        InventoryServiceClient inventory = inventoryClient.check(order.getItems());  // 2
        PricingServiceClient pricing = pricingClient.calculate(order.getItems());  // 3
        FraudDetectionClient fraudCheck = fraudClient.check(order, customer);  // 4
        PaymentServiceClient payment = paymentClient.charge(order.getTotal());  // 5
        InventoryClient reserve = inventoryClient.reserve(order.getItems());  // 6
        WarehouseClient assign = warehouseClient.assign(order.getId());  // 7
        ShippingServiceClient shipping = shippingClient.schedule(order.getId());  // 8
        NotificationClient email = notificationClient.sendConfirmation(order);  // 9
        AuditClient log = auditClient.record(order);  // 10
    }
}
```

```java
// Good: Event-driven choreography — services react to events
public class OrderService {
    public void placeOrder(Order order) {
        order.place();
        orderRepository.save(order);
        eventPublisher.publish(new OrderPlacedEvent(order.getId(), order.getCustomerId(), order.getTotal()));
    }
}

@EventListener
public class OnOrderPlacedEvent {
    public void handle(OrderPlacedEvent event) {
        // This service only cares about its own responsibility
        inventoryService.reserve(event.orderId(), event.items());
    }
}

@EventListener
public class OnInventoryReservedEvent {
    public void handle(InventoryReservedEvent event) {
        paymentService.charge(event.orderId(), event.totalAmount());
    }
}

@EventListener
public class OnPaymentSucceededEvent {
    public void handle(PaymentSucceededEvent event) {
        shippingService.schedule(event.orderId());
        notificationService.sendConfirmation(event.orderId());
    }
}
```

#### Nanoservices

```java
// Bad: One microservice per field — extreme decomposition
// OrderService → OrderIdService, OrderLineService, OrderTotalService,
//               OrderStatusService, OrderAddressService, OrderCustomerService...
public class OrderIdService {
    public UUID generateOrderId() { return UUID.randomUUID(); }
}

public class OrderTotalService {
    public Money calculateTotal(List<OrderLine> lines) {
        return lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(Money.ZERO, Money::add);
    }
}

public class OrderCustomerService {
    public Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId);
    }
}
```

```java
// Good: Service aligned with bounded context
public class OrderService {  // One service owns the entire Order bounded context
    public Order createOrder(CreateOrderCommand command) {
        Customer customer = customerRepository.findById(command.customerId());
        Order order = Order.create(customer.getId());
        for (OrderItemCommand item : command.items()) {
            Product product = productRepository.findById(item.productId());
            order.addLine(product, item.quantity());
        }
        order.place();
        orderRepository.save(order);
        return order;
    }

    public Money calculateTotal(Order order) {
        return order.calculateTotal();  // Domain method
    }

    public void cancelOrder(OrderId orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel(reason);
        orderRepository.save(order);
    }
}
```

---

## Testing Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Test Envy** | Tests have more code than production | Tests become burden | Keep tests focused, use test utilities |
| **Mock Overload** | Excessive mocking of collaborators | Tests don't reflect reality | Use real objects when possible |
| **Assertion Roulette** | Multiple unrelated assertions | Hard to diagnose failures | One assertion per test |
| **Gentleman Test** | Tests not run or commented out | Degraded confidence | Delete or fix tests |
| **Secret Catcher** | Tests that don't verify anything | False confidence | Always assert expected behavior |

### Common Examples

#### Mock Overload

```java
// Bad: Mocking everything — test has no real behavior
@ExtendWith(MockitoExtension.class)
class BadOrderServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmailService emailService;
    @Mock private AuditLogger auditLogger;
    @Mock private Clock clock;

    @Test
    void shouldPlaceOrder() {
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clock.instant()).thenReturn(Instant.now());
        when(eventPublisher.publish(any())).thenReturn(List.of());
        when(emailService.send(any())).thenReturn(true);
        when(auditLogger.log(any())).thenReturn(null);

        OrderService service = new OrderService(
            orderRepository, customerRepository, productRepository,
            eventPublisher, emailService, auditLogger, clock
        );

        service.placeOrder(createTestOrder());

        verify(orderRepository).save(any());
        verify(eventPublisher).publish(any());
        verify(emailService).send(any());
        verify(auditLogger).log(any());
    }
}
```

```java
// Good: Use real domain objects, minimal mocking
@ExtendWith(MockitoExtension.class)
class GoodOrderServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private CreateOrderUseCase useCase;

    @Test
    void shouldCreateOrderWithItems() {
        // Given
        CustomerId customerId = CustomerId.generate();
        Product product = Product.create("Laptop", Money.of(1000));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(
            Customer.create(customerId, "John Doe")
        ));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        CreateOrderCommand command = new CreateOrderCommand(
            customerId,
            List.of(new OrderItemCommand(product.getId(), 2))
        );
        OrderResult result = useCase.execute(command);

        // Then
        assertThat(result.totalAmount()).isEqualTo(Money.of(2000));
        assertThat(result.status()).isEqualTo(Order.OrderStatus.PLACED);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldRejectEmptyOrder() {
        // Given
        CustomerId customerId = CustomerId.generate();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(
            Customer.create(customerId, "John Doe")
        ));

        // When/Then
        CreateOrderCommand command = new CreateOrderCommand(customerId, List.of());
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(OrderEmptyException.class);
    }
}
```

---

## Refactoring Anti-Patterns

### Summary Table

| Anti-Pattern | Symptoms | Consequences | Remedy |
|--------------|----------|--------------|--------|
| **Parallel Replacement** | Keeping old and new code during refactor | Confusion, technical debt | Delete old code after validation |
| **Waterbed Effect** | Fixing one problem creates another | Complexity shifts | Use test coverage, review impact |
| **Cutting the Gordian Knot** | Big bang refactor instead of incremental | High risk, hard to rollback | Incremental changes with small PRs |

### Common Examples

#### Parallel Replacement

```java
// Bad: Keeping both old and new code
public class OrderService {
    // Old implementation — still here "just in case"
    public Money calculateTotalOld(List<OrderLine> lines) {
        Money total = Money.ZERO;
        for (OrderLine line : lines) {
            total = total.add(line.getPrice().multiply(line.getQuantity()));
        }
        return total;
    }

    // New implementation — supposed to replace the old one
    public Money calculateTotal(List<OrderLine> lines) {
        return lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(Money.ZERO, Money::add);
    }
}
```

```java
// Good: Incremental refactor with small PRs and deletion of old code
// PR 1: Add the new method
public class Order {
    public Money calculateTotal() {
        return lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(Money.ZERO, Money::add);
    }
}

// PR 2: Migrate callers, delete old code
// OrderService now calls order.calculateTotal() directly
// No parallel implementation kept
```

---

## Related References

- [SOLID Principles](./solid-principles.md) — The foundational principles that prevent many anti-patterns
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — The layer definitions that prevent layering anti-patterns
- [Rich vs Anemic Model](./rich-vs-anemic-model.md) — Detailed comparison of domain model styles
- [Repository Pattern](./repository-pattern.md) — How to properly abstract data access
- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Alternative to traditional state storage
- [Microservices Patterns](./microservices-patterns.md) — Patterns and anti-patterns for distributed systems
