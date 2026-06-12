---
name: software-architecture
description: Software Architecture Design Methodology Guide. Covers Clean Architecture, DDD Bounded Contexts, Rich Domain Model Design, Hexagonal Architecture, Event-Driven Architecture, and Microservices Design Patterns.
---

# Software Architecture

## Architecture Design Principles

### SOLID Principles

| Principle                 | Description                                   | Violation Symptoms                                      |
| ------------------------- | --------------------------------------------- | ------------------------------------------------------- |
| **S**ingle Responsibility | A class should have only one reason to change | A class does too many things                            |
| **O**pen/Closed           | Open for extension, closed for modification   | Modifying existing code to add new features             |
| **L**iskov Substitution   | Subclasses can replace parent classes         | instanceof checks, type casting                         |
| **I**nterface Segregation | Small, focused interfaces                     | Fat interfaces, forced implementation of unused methods |
| **D**ependency Inversion  | Depend on abstractions, not concretions       | Direct dependency on concrete classes                   |

### Signs of Architecture Decay

- Circular dependencies: Module A → B → C → A
- Shotgun surgery: Changing one feature requires modifying multiple classes
- Feature envy: A class spends more time accessing other class's data than its own
- Duplicated code: Repeated code scattered across the codebase
- Premature abstraction: Violating YAGNI, adding unnecessary indirection

## Clean Architecture

### Layer Model

```
┌─────────────────────────────────────────────────────────┐
│                    Frameworks & Drivers                  │
│         Web frameworks, ORM, UI frameworks, DB, external services │
├─────────────────────────────────────────────────────────┤
│                   Interface Adapters                     │
│      Controllers, Gateways, Presenters, Mappers         │
├─────────────────────────────────────────────────────────┤
│                    Application Layer                     │
│            Use Cases, Application Services               │
│              Commands, Queries, Handlers                  │
├─────────────────────────────────────────────────────────┤
│                      Domain Layer                        │
│    Entities, Value Objects, Aggregates, Domain Events    │
│           Domain Services, Repository Interfaces          │
│                    (No external dependencies)           │
└─────────────────────────────────────────────────────────┘
         ↑ Dependencies only point inward, outer layers depend on inner layers, inner layers know nothing about outer layers
```

### Dependency Rules

1. **Domain Layer is the Core**: No dependencies on external frameworks, libraries, or infrastructure
2. **Dependency Direction**: Outer layers can depend on inner layers, inner layers must never know about outer layers
3. **Interface Definition Location**: The consumer defines the interface (producer implements it)
4. **Data Format**: Each layer uses its own data format, outer layer formats must not be passed directly

### Project Structure Example (Java)

```
src/main/java/com/ai/
├── domain/                    # Domain layer (core, no external dependencies)
│   ├── model/
│   │   ├── entity/           # Entities
│   │   │   └── Order.java
│   │   ├── vo/               # Value Objects
│   │   │   ├── Money.java
│   │   │   └── Email.java
│   │   ├── aggregate/        # Aggregates
│   │   │   └── OrderAggregate.java
│   │   ├── event/           # Domain Events
│   │   │   └── OrderPlacedEvent.java
│   │   └── service/         # Domain Services
│   │       └── PricingService.java
│   └── repository/          # Repository interfaces
│       └── OrderRepository.java
│
├── application/              # Application layer
│   ├── command/             # Command handling
│   │   └── placeorder/
│   │       ├── PlaceOrderCommand.java
│   │       └── PlaceOrderHandler.java
│   ├── query/               # Query handling
│   │   └── getorder/
│   │       ├── GetOrderQuery.java
│   │       └── GetOrderHandler.java
│   └── service/             # Application Services
│       └── OrderApplicationService.java
│
├── infrastructure/           # Infrastructure layer
│   ├── persistence/         # Persistence implementations
│   │   └── jpa/
│   │       ├── OrderRepositoryImpl.java
│   │       └── OrderJpaEntity.java
│   ├── messaging/           # Messaging implementations
│   │   └── OrderEventPublisher.java
│   └── external/           # External service adapters
│       └── PaymentGatewayAdapter.java
│
└── interface/               # Interface Adapters layer
    └── api/
        ├── controller/
        │   └── OrderController.java
        └── dto/
            ├── request/
            └── response/
```

## DDD Domain-Driven Design

### Strategic Design

#### Bounded Context

A Bounded Context is an explicit boundary around a semantic boundary, each context has its own:

- **Ubiquitous Language**: Terms and meanings shared by the team
- **Domain Model**: Concepts that belong exclusively to this context
- **Boundary**: Clear definition of what's inside and what's outside

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Order Context  │    │  Inventory Context │    │  Payment Context │
│                  │    │                  │    │                  │
│  - Order        │◄──►│  - Inventory    │◄──►│  - Payment      │
│  - OrderItem    │    │  - Stock        │    │  - Transaction  │
│  - Pricing      │    │  - Warehouse    │    │  - Gateway     │
│                  │    │                  │    │                  │
│  Team: Order    │    │  Team: Warehouse │    │  Team: Payment  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### Core Domain / Supporting Domain / Generic Domain

| Type                  | Description                                           | Investment                            |
| --------------------- | ----------------------------------------------------- | ------------------------------------- |
| **Core Domain**       | Core competency, unique value proposition             | Maximum investment, carefully crafted |
| **Supporting Domain** | Supports the core domain, requires custom development | Moderate investment                   |
| **Generic Domain**    | Generic solutions, can be purchased                   | Minimal investment                    |

#### Context Mapping

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Customer   │      │    Order    │      │   Fulfill   │
│   Context   │      │   Context   │      │   Context   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │ conformist          │ upstream/downstream│ conformist
       └────────────────────┴────────────────────┘
```

- **Shared Kernel**: Subset of shared domain model
- **Customer/Supplier**: Upstream/downstream relationship
- **Conformist**: Downstream completely follows upstream model
- **Anticorruption Layer**: Translation layer isolating different models

### Tactical Design

#### Entity

Objects with unique identity whose lifecycle can continue.

```java
// Rich Domain Model: Behavior inside the entity
public class Order extends AggregateRoot {
    private OrderId id;           // Unique identifier
    private CustomerId customerId;
    private List<OrderLine> lines;
    private OrderStatus status;
    private Money totalAmount;

    // Factory method to create order
    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        Order order = new Order();
        order.id = OrderId.generate();
        order.customerId = customerId;
        order.lines = new ArrayList<>(lines);
        order.status = OrderStatus.DRAFT;
        order.totalAmount = calculateTotal(lines);
        return order;
    }

    // Business behavior: Place order
    public void place() {
        if (status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException("Only draft order can be placed");
        }
        if (lines.isEmpty()) {
            throw new OrderEmptyException("Order must have at least one line");
        }
        status = OrderStatus.PLACED;
        // Publish domain event
        addDomainEvent(new OrderPlacedEvent(this));
    }

    // Business behavior: Cancel order
    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new OrderCannotBeCancelledException();
        }
        status = OrderStatus.CANCELLED;
        addDomainEvent(new OrderCancelledEvent(this, reason));
    }

    // Protected constructor (enforce factory method usage)
    protected Order() {}
}
```

#### Value Object

No unique identity, immutable, equality based on attribute values.

```java
// Immutable value object
public record Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        this.amount = amount.stripTrailingZeros();
        this.currency = Objects.requireNonNull(currency, "Currency is required");
    }

    // Value object operations return new instances
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    // equals/hashCode based on value
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    // No setters, all fields final
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }
}
```

#### Aggregate

Consistency boundary, accessed externally through the root entity.

```java
// Aggregate root: Order is the access point for OrderLine
public class Order extends AggregateRoot {
    private OrderId id;
    private List<OrderLine> lines;  // Internally managed, not directly exposed

    // External access only through aggregate root
    public void addLine(Product product, int quantity) {
        // Invariant rules validated inside aggregate root
        validateLine(product, quantity);

        OrderLine line = new OrderLine(product.getId(), product.getPrice(), quantity);
        lines.add(line);
        recalculateTotal();
    }

    // No direct access to internal lines
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    // Modifications only through aggregate root
    public void removeLine(OrderLineId lineId) {
        lines.removeIf(line -> line.getId().equals(lineId));
        recalculateTotal();
    }
}

// Incorrect: Exposing internal implementation
// public class BadOrder {
//     public List<OrderLine> lines;  // Directly exposed, can be modified externally
// }
```

#### Repository

Collection abstraction for aggregates, CRUD operations only on aggregate roots.

```java
// Domain layer: Define repository interface (no implementation dependencies)
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Optional<Order> findByIdWithLines(OrderId id);  // Aggregate loading strategy
    Page<Order> findByCustomer(CustomerId customerId, Pageable pageable);
    void save(Order order);
    void delete(Order order);
}

// Infrastructure layer: Implement repository
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository delegate;

    @Override
    public Optional<Order> findByIdWithLines(OrderId id) {
        return delegate.findWithLinesById(id.value())
            .map(jpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Order order) {
        JpaOrder entity = jpaMapper.toEntity(order);
        delegate.save(entity);
    }
}
```

#### Domain Service

Business logic that cannot be attributed to a single entity.

```java
// Cross-entity business rules
public class PricingService {

    // Calculate order total, considering discount rules
    public Money calculateOrderPrice(List<OrderLine> lines, Customer customer, Promotion promotion) {
        Money subtotal = lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(new Money(BigDecimal.ZERO, Currency.USD), Money::add);

        Money discount = calculateDiscount(subtotal, customer, promotion);
        return subtotal.add(discount.negate());
    }

    private Money calculateDiscount(Money subtotal, Customer customer, Promotion promotion) {
        Money discount = Money.ZERO;

        // VIP customer discount
        if (customer.isVip()) {
            discount = discount.add(subtotal.multiply(0.1));
        }

        // Promotion discount
        if (promotion != null && promotion.isActive()) {
            discount = discount.add(promotion.applyTo(subtotal));
        }

        // Cannot exceed order amount
        return discount.isGreaterThan(subtotal) ? subtotal : discount;
    }
}
```

#### Domain Event

Important events that occurred within the domain, used for decoupling.

```java
// Domain event definition
public record OrderPlacedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    Instant occurredAt
) {}

// Aggregate root publishes events
public class Order extends AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public void place() {
        // ... placement logic
        addDomainEvent(new OrderPlacedEvent(
            this.id,
            this.customerId,
            this.totalAmount,
            Instant.now()
        ));
    }

    @Override
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}

// Event handler
@EventHandler
public class OrderEventHandler {
    public void handle(OrderPlacedEvent event) {
        // Send emails, notify inventory, update reports, etc.
    }
}
```

### Anemic Domain Model vs Rich Domain Model

| Characteristic          | Anemic Domain Model (Anti-Pattern) | Rich Domain Model (Recommended)           |
| ----------------------- | ---------------------------------- | ----------------------------------------- |
| Entity content          | Only fields + getter/setter        | Fields + business behavior                |
| Business logic location | Service layer                      | Inside domain objects                     |
| State changes           | Service directly modifies fields   | Domain object methods encapsulate changes |
| Validation logic        | Service or utility classes         | Domain object self-validation             |
| Testability             | Test Service                       | Test domain objects                       |

```java
// Anemic Domain Model (Incorrect)
public class AnemicOrder {
    private UUID id;
    private List<OrderLine> lines;
    private OrderStatus status;

    // Only getter/setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ...
}

// Service takes all business logic (violates Single Responsibility)
public class AnemicOrderService {
    public void placeOrder(Order order) {
        if (order.getLines().isEmpty()) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.PLACED);
        repository.save(order);
        // Send emails, deduct inventory...
    }
}

// Rich Domain Model (Correct)
public class Order {
    public void place() {
        if (this.lines.isEmpty()) {
            throw new OrderEmptyException();
        }
        this.status = OrderStatus.PLACED;
    }
}
```

## Hexagonal Architecture (Ports and Adapters)

```
                    ┌─────────────────────┐
                    │    Primary Adapters  │
                    │  (Driving Actors)     │
                    │                       │
                    │  ┌─────────────────┐  │
                    │  │   Controllers   │  │
                    │  │   REST, GraphQL │  │
                    │  │   CLI, Events   │  │
                    │  └────────┬────────┘  │
                    └───────────┼───────────┘
                                │
                                ▼
                    ┌─────────────────────┐
                    │                     │
                    │      PORTS           │
                    │  (Inbound Interfaces)│
                    │                     │
                    │  ┌─────────────────┐  │
                    │  │  Use Cases /    │  │
                    │  │  Commands       │  │
                    │  │                 │  │
                    │  └────────┬────────┘  │
                    │           │            │
                    │           ▼            │
                    │  ┌─────────────────┐  │
                    │  │     DOMAIN       │  │
                    │  │   (Core Logic)   │  │
                    │  │                  │  │
                    │  │  Entities       │  │
                    │  │  Value Objects  │  │
                    │  │  Domain Services │  │
                    │  └────────┬────────┘  │
                    │           │            │
                    │      PORTS           │
                    │  (Outbound Interfaces)│
                    │           │            │
                    │  ┌────────┴────────┐  │
                    │  │    Secondary    │  │
                    │  │    Adapters     │  │
                    │  │                 │  │
                    │  │  Repositories   │  │
                    │  │  External APIs  │  │
                    │  │  Message Queues │  │
                    └──┴─────────────────┴──┘
```

## Event-Driven Architecture

### Event Sourcing

```java
// Event store instead of state store
public class BankAccount {
    private AccountId id;
    private List<DomainEvent> events = new ArrayList();

    // Reconstruct state from event replay
    public void replay(Iterable<DomainEvent> events) {
        events.forEach(this::mutate);
    }

    private void mutate(DomainEvent event) {
        switch (event) {
            case DepositedEvent e -> apply(e);
            case WithdrawnEvent e -> apply(e);
        }
    }

    private void apply(DepositedEvent e) {
        this.balance = this.balance.add(e.amount());
    }

    // Commands produce events
    public void deposit(Money amount) {
        if (amount.isNegative()) throw new InvalidAmountException();
        events.add(new DepositedEvent(id, amount, Instant.now()));
        mutate(events.get(events.size() - 1));
    }
}
```

### CQRS (Command Query Responsibility Segregation)

```
┌─────────────────┐         ┌─────────────────┐
│   Commands      │         │     Queries     │
│  (Write Model)  │         │  (Read Model)   │
│                 │         │                 │
│  CreateOrder    │────────►│  OrderSummary   │
│  UpdateOrder    │  sync    │  OrderDetails   │
│  CancelOrder    │  async   │  OrderHistory   │
│                 │         │                 │
└────────┬────────┘         └────────▲────────┘
         │                            │
         │        ┌──────────────────┘
         │        │
         ▼        ▼
    ┌─────────────────────────────────┐
    │       Event Store / Bus         │
    │   (Kafka, EventStore, etc.)     │
    └─────────────────────────────────┘
```

## Microservices Design Patterns

### Aggregates vs Event-Driven

| Pattern          | Characteristics                                      | Applicable Scenarios                                   |
| ---------------- | ---------------------------------------------------- | ------------------------------------------------------ |
| **Aggregates**   | Monolithic architecture, divided by bounded contexts | Small teams, moderate business complexity              |
| **Event-Driven** | Async collaboration via events                       | Independent deployment, high concurrency, cross-system |
| **Saga**         | Distributed transaction management                   | Requires cross-service consistency                     |
| **CQRS**         | Read/write separation                                | Large read/write ratio differences                     |

### Inter-Service Communication

```
Sync communication:                        Async communication:
┌─────┐    REST/gRPC    ┌─────┐   ┌─────┐    Event    ┌─────┐
│ A   │ ───────────────► │ B   │   │ A   │ ─────────► │ B   │
└─────┘                  └─────┘   └─────┘            └─────┘
      Response                        Publish/Subscribe     Consume/Process
```

## Architecture Decision Records (ADR)

Each significant architecture decision should be documented:

```markdown
# ADR-001: Designing Order Aggregate Using Rich Domain Model

## Status

Accepted

## Context

Order business logic is scattered across OrderService and various places, lacking unified encapsulation.

## Decision

Adopt Rich Domain Model, encapsulate order state changes and business rules inside the Order entity.

## Consequences

- Order state machine fully encapsulated
- Business rules cohesive within domain objects
- Easy to unit test

## Drawbacks

- Team needs to learn DDD Rich Domain Model
- Aggregate design requires careful review
```

## Architecture Review Checklist

### Code-Level Review

- [ ] No circular dependencies (module/package level)
- [ ] Domain layer has no infrastructure dependencies
- [ ] Entities contain business behavior (not just fields)
- [ ] Value objects are immutable
- [ ] Aggregate boundaries are clear
- [ ] Repositories only operate on aggregate roots

### Design-Level Review

- [ ] Bounded Context division is reasonable
- [ ] Context mapping relationships are clear
- [ ] Core Domain receives sufficient investment
- [ ] Architecture layers follow dependency rules

### Change Impact Analysis

- [ ] What modules/layers need modification for a given change
- [ ] Where should new functionality be placed in bounded contexts
- [ ] Need to create new aggregates or services
