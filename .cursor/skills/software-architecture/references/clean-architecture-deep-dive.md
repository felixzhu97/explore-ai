# Clean Architecture

A layered architecture pattern that separates concerns and enforces dependency rules, keeping the domain core independent of frameworks and infrastructure.

## When to Use

Consult this file when setting up a new Java project structure, reviewing architecture compliance, designing layer boundaries, or refactoring towards a domain-centric architecture.

## Core Idea

Clean Architecture organizes code into concentric layers, each with specific responsibilities and dependency rules.

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

### Project Structure (Java)

```
src/main/java/com/ai/
├── domain/                    # Domain layer (core, no external dependencies)
│   ├── model/               # Entities, Value Objects, Aggregates
│   ├── event/              # Domain Events
│   ├── service/            # Domain Services
│   └── repository/        # Repository interfaces
├── application/            # Application layer
│   ├── command/           # Command handling (CQRS)
│   ├── query/             # Query handling
│   └── service/           # Application Services
├── infrastructure/         # Infrastructure layer
│   ├── persistence/      # JPA entities, Repository implementations
│   ├── messaging/        # Event publishers
│   └── external/         # External service adapters
└── interface/            # Interface Adapters layer
    └── api/              # Controllers, DTOs
```

## Dependency Rules Explained

### Rule 1: Domain is the Core

The Domain layer must have zero external dependencies. This means:

- No `import org.springframework.*`
- No `import jakarta.persistence.*`
- No `import javax.inject.*`
- No annotations like `@Entity`, `@Service`, `@Component`
- Pure Java (or the language's standard library only)

### Rule 2: Dependencies Point Inward Only

```
  Interface Adapters ──► Application ──► Domain ◄── Infrastructure
```

Outer layers (Infrastructure, Interface) can depend on inner layers (Application, Domain). Inner layers must never depend on outer layers.

### Rule 3: Consumer Defines Interfaces

The layer that **uses** an interface defines it. For example:

- Domain defines `OrderRepository` because Domain uses it
- Application defines `PaymentPort` because Application uses it
- Infrastructure implements these interfaces

### Rule 4: Each Layer Owns Its Data Format

Do not pass outer-layer DTOs (e.g., JPA entities, HTTP DTOs) into inner layers. Transform data at layer boundaries.

## Bad/Good Examples (Java)

```java
// ❌ BAD: Domain layer depends on Spring
package com.ai.domain.model;

import org.springframework.stereotype.Component; // FORBIDDEN!

@Component
public class Order { ... }

// ❌ BAD: Domain layer depends on JPA
package com.ai.domain.model;

import jakarta.persistence.*; // FORBIDDEN!

@Entity
@Table(name = "orders")
public class Order { ... }

// ❌ BAD: Repository implementation in Domain layer
package com.ai.domain.repository;

public interface OrderRepository { ... } // Interface here is OK
// But implementation must NOT be in domain/

// ❌ BAD: Controller passes DTO directly to Domain
public void create(@RequestBody CreateOrderDto dto) {
    // dto may contain Hibernate validation annotations
    orderService.create(dto); // dto from outer layer passed to inner layer
}
```

```java
// ✅ GOOD: Domain layer — pure Java, no framework dependencies
package com.ai.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot {
    private final OrderId id;
    private CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLine> lines;

    private Order(OrderId id, CustomerId customerId) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }

    public static Order create(CustomerId customerId) {
        return new Order(new OrderId(UUID.randomUUID()), customerId);
    }

    public void addLine(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Invalid quantity");
        lines.add(new OrderLine(product.getId(), product.getPrice(), quantity));
    }

    public void place() {
        if (status != OrderStatus.DRAFT) throw new OrderInvalidStateException();
        if (lines.isEmpty()) throw new OrderEmptyException();
        status = OrderStatus.PLACED;
        addDomainEvent(new OrderPlacedEvent(id, customerId, calculateTotal(), Instant.now()));
    }

    public Money calculateTotal() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    // Getters only — no public setters
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return calculateTotal(); }
}
```

```java
// ✅ GOOD: Application layer — defines repository interface it uses
package com.ai.application.usecase;

public class CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public CreateOrderUseCase(
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public OrderResult execute(CreateOrderCommand command) {
        Order order = Order.create(command.customerId());
        for (OrderItemCommand item : command.items()) {
            Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> ProductException.notFound(item.productId()));
            order.addLine(product, item.quantity());
        }
        orderRepository.save(order);
        return OrderResult.from(order);
    }
}
```

```java
// ✅ GOOD: Infrastructure layer — implements repository
package com.ai.infrastructure.persistence.repository;

@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository delegate;
    private final OrderEntityMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        return delegate.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public void save(Order order) {
        delegate.save(mapper.toEntity(order));
    }
}
```

## Common Pitfalls

- **Leaking infrastructure into domain**: Using Spring's `@Service` or JPA's `@Entity` in domain classes
- **Passing JPA entities into domain methods**: Transforming at the boundary is required
- **Repository implementations in the wrong layer**: Always implement Domain interfaces in Infrastructure
- **Application layer doing too much**: Keep use cases thin — orchestrate, don't implement business logic

## Architecture Validation Commands

```bash
# Check Domain layer has no framework dependencies
grep -r "import org.springframework" src/domain/ || echo "✅ Domain no Spring"
grep -r "import jakarta.persistence" src/domain/ || echo "✅ Domain no JPA"
grep -r "@Entity" src/domain/ || echo "✅ Domain no Entity annotations"

# Check dependency direction
grep -r "import.*application" src/domain/ || echo "✅ Domain does not depend on Application"
grep -r "import.*infrastructure" src/domain/ || echo "✅ Domain does not depend on Infrastructure"
grep -r "import.*interface" src/domain/ || echo "✅ Domain does not depend on Interface"
```

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/` — Domain layer with no external dependencies. `apps/server/src/main/java/com/ai/application/` — Use cases that orchestrate domain objects. `apps/server/src/main/java/com/ai/infrastructure/` — JPA implementations of domain repository interfaces.

## Related References

- [SOLID Principles](./solid-principles.md) — Underlying design principles
- [DDD Strategic Design](./ddd-strategic-design.md) — Bounded Contexts and Context Mapping
- [Entity Pattern](./entity-pattern.md) — Domain entities in Clean Architecture
- [Repository Pattern](./repository-pattern.md) — Repository interface/implementation separation
- [Hexagonal Architecture](./hexagonal-architecture.md) — Ports and Adapters, an alternative but complementary view
- [Software Architecture](../SKILL.md)
- [Software Development](../../software-development/SKILL.md)
