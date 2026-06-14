# Hexagonal Architecture

An architecture pattern that organizes code around business logic (the "hexagon") with clear separation between inbound/outbound ports and their adapters.

## When to Use

Consult this file when designing a system that needs to be framework-agnostic, when multiple clients (REST, GraphQL, CLI, events) should drive the same core logic, or when you want to make the core business logic independently testable without any infrastructure.

## Core Idea

Hexagonal Architecture (also called Ports and Adapters) places the domain at the center. Everything outside the domain — frameworks, databases, UI, external services — are adapters that plug into ports.

### The Hexagon Diagram

```
                    ┌─────────────────────┐
                    │    Primary Adapters  │
                    │  (Driving Actors)     │
                    │  ┌─────────────────┐  │
                    │  │   Controllers   │  │
                    │  │   REST, GraphQL │  │
                    │  │   CLI, Events   │  │
                    │  └────────┬────────┘  │
                    └───────────┼───────────┘
                                │
                                ▼
                    ┌─────────────────────┐
                    │      PORTS           │
                    │  (Inbound Interfaces)│
                    │  ┌─────────────────┐  │
                    │  │  Use Cases /    │  │
                    │  │  Commands       │  │
                    │  └────────┬────────┘  │
                    │           │            │
                    │           ▼            │
                    │  ┌─────────────────┐  │
                    │  │     DOMAIN       │  │
                    │  │   (Core Logic)   │  │
                    │  │  Entities       │  │
                    │  │  Value Objects  │  │
                    │  │  Domain Services │  │
                    │  └────────┬────────┘  │
                    │           │            │
                    │      PORTS           │
                    │  (Outbound Interfaces)│
                    │  ┌────────┴────────┐  │
                    │  │    Secondary    │  │
                    │  │    Adapters     │  │
                    │  │  Repositories   │  │
                    │  │  External APIs  │  │
                    │  │  Message Queues │  │
                    └──┴─────────────────┴──┘
```

### Ports

A **port** is an interface defined by the core (domain/application) that specifies an interaction contract.

- **Inbound Ports** (or Driving Ports): Defined by the application/use case layer. Define what operations the outside world can perform on the system. Example: `OrderService`, `CreateOrderUseCase`
- **Outbound Ports** (or Driven Ports): Defined by the application layer. Define what the core needs from the outside world. Example: `OrderRepository`, `PaymentGateway`, `EmailSender`

### Adapters

An **adapter** is an implementation that satisfies a port.

- **Primary (Driving) Adapters**: Act on the system (REST controllers, GraphQL resolvers, CLI handlers, event listeners)
- **Secondary (Driven) Adapters**: The system acts through them (JPA repositories, HTTP clients, message publishers)

### Relationship to Clean Architecture

Hexagonal and Clean Architecture express the same ideas with different metaphors:

| Hexagonal | Clean Architecture | Responsibility |
|-----------|-------------------|----------------|
| Primary Adapters | Interface Adapters | Controllers, REST, GraphQL |
| Inbound Ports | Application Layer | Use Cases, Commands |
| Domain | Domain Layer | Entities, Value Objects |
| Outbound Ports | Application Ports | Repository interfaces |
| Secondary Adapters | Infrastructure | Database, external services |

Both enforce the same dependency rule: **outer layers depend inward, inner layers know nothing about outer layers**.

## Code Example

### Domain (Center of the Hexagon)

```java
// Outbound Port: defined by Application layer, implemented by Infrastructure
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
}

// Outbound Port: defined by Application layer
public interface PaymentGateway {
    PaymentResult charge(Money amount, PaymentMethod method);
}

// Inbound Port: use case interface
public interface CreateOrderUseCase {
    OrderResult execute(CreateOrderCommand command);
}

// Domain Entity
public class Order extends AggregateRoot {
    private final OrderId id;
    private OrderStatus status;

    public void place(Money totalAmount, PaymentGateway paymentGateway) {
        if (this.status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException();
        }
        PaymentResult result = paymentGateway.charge(totalAmount, PaymentMethod.CARD);
        if (result.isSuccess()) {
            this.status = OrderStatus.PAID;
            addDomainEvent(new OrderPlacedEvent(this.id));
        }
    }
}
```

### Infrastructure (Secondary Adapters)

```java
// JPA Adapter implements OrderRepository (outbound port)
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository delegate;

    @Override
    public Optional<Order> findById(OrderId id) {
        return delegate.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public void save(Order order) {
        delegate.save(mapper.toEntity(order));
    }
}

// External Payment Gateway Adapter
public class StripePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult charge(Money amount, PaymentMethod method) {
        // Stripe API call
    }
}
```

### Application (Inbound Port Implementation)

```java
// Application layer implements the inbound port
@Service
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentGateway paymentGateway;

    @Override
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

### Interface (Primary Adapters)

```java
// REST Controller — primary (driving) adapter
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = mapper.toCommand(request);
        OrderResult result = createOrderUseCase.execute(command);
        return ResponseEntity.created(URI.create("/api/orders/" + result.orderId()))
            .body(OrderResponse.from(result));
    }
}
```

## Bad/Good Examples

```java
// ❌ BAD: Business logic in the adapter (primary driving actor)
// The controller is doing too much — this is Smart UI anti-pattern
@RestController
public class BadOrderController {
    @PostMapping("/orders")
    public Order placeOrder(@RequestBody OrderRequest req) {
        // Business logic in the controller
        if (req.items().isEmpty()) throw new BadRequestException();
        Order order = new Order(req.customerId());
        for (Item i : req.items()) {
            Product p = productRepo.findById(i.productId());
            order.addLine(p, i.quantity());
        }
        // Direct field manipulation — no encapsulation
        order.status = "PLACED";
        repo.save(order);
        emailService.send(order);
        return order;
    }
}

// ✅ GOOD: Thin controller delegates to use case
@RestController
public class GoodOrderController {
    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        CreateOrderCommand command = mapper.toCommand(req);
        OrderResult result = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(result));
    }
}
```

## Common Pitfalls

- **Ports explosion**: Creating too many small ports. Group related operations into cohesive use cases.
- **Domain depending on port interfaces**: Ports belong in Application layer, not Domain. Domain defines only domain-level interfaces (e.g., `OrderRepository` for persistence).
- **Adapters knowing too much**: Adapters should be thin wrappers around external systems. If your adapter has business logic, it's in the wrong place.
- **Skipping the hexagon metaphor**: Don't draw the hexagon diagram and then ignore it. The separation is only valuable if adapters are truly replaceable.

## When NOT to Use

Hexagonal Architecture adds indirection that may not be worth it for:
- Simple CRUD applications with minimal business logic
- Very small teams or single-developer projects
- Prototypes that will be discarded

## Real Implementation Reference

`apps/server/src/main/java/com/ai/` — Domain entities at the core. Application use cases as inbound ports. Repository interfaces as outbound ports. Infrastructure adapters implementing ports.

## Related References

- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Layer model — the same architecture expressed differently
- [Repository Pattern](./repository-pattern.md) — Outbound port for persistence
- [Domain Event Pattern](./domain-event-pattern.md) — Outbound port for event publishing
- [Entity Pattern](./entity-pattern.md) — Domain core entities
- [CQRS Pattern](./cqrs-pattern.md) — Separate command/query ports
- [Event-Driven Architecture](./event-driven-architecture.md) — Event-driven adapters
- [Software Architecture](../SKILL.md)
- [Software Development](../../software-development/SKILL.md)
