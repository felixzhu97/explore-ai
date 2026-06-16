# CQRS Pattern

Command Query Responsibility Segregation — the practice of separating read and write operations into different models, each optimized for its purpose.

## When to Use

Consult this file when read and write workloads have very different characteristics, when you need to optimize query performance independently from write performance, when building event-driven systems with multiple read models, or when different parts of the system need different data representations of the same information.

## Core Idea

In most systems, reads far outnumber writes, and read and write patterns are fundamentally different. CQRS acknowledges this by using separate models: a **Command model** (or write model) optimized for capturing state changes, and a **Query model** (or read model) optimized for serving data to clients.

### CQRS Diagram

```
┌─────────────────┐         ┌─────────────────┐
│   Commands      │         │     Queries     │
│  (Write Model)  │         │  (Read Model)   │
│                 │         │                 │
│  CreateOrder    │────────►│  OrderSummary   │
│  UpdateOrder    │  sync   │  OrderDetails   │
│  CancelOrder    │  async  │  OrderHistory   │
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

### Traditional vs CQRS

| Aspect | Traditional (Same Model) | CQRS |
|--------|------------------------|------|
| Model | Single model for read/write | Separate command and query models |
| Complexity | Simpler for basic cases | More moving parts |
| Read optimization | Limited | Fully independent |
| Write optimization | Limited | Fully independent |
| Consistency | Strong (same model) | Eventual (between models) |

### When CQRS Shines

1. **Different read/write ratios**: 100:1 read:write ratio — separate optimization is valuable
2. **Complex read queries**: Reports, dashboards, analytics that join many tables
3. **Multiple representations**: Different clients need different views of the same data
4. **Event-driven architecture**: Events naturally support multiple read models

## Command Model (Write Side)

```java
// Commands represent intent to change state
public sealed interface OrderCommand permits CreateOrderCommand, UpdateOrderCommand, CancelOrderCommand {}

public record CreateOrderCommand(
    CustomerId customerId,
    List<OrderItemCommand> items
) implements OrderCommand {
    public record OrderItemCommand(ProductId productId, int quantity) {}
}

public record CancelOrderCommand(
    OrderId orderId,
    String reason
) implements OrderCommand {}

// Command handler
public class OrderCommandHandler {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public void handle(CreateOrderCommand command) {
        Order order = Order.create(command.customerId());
        for (var item : command.items()) {
            order.addLine(item.productId(), item.quantity());
        }
        order.place();
        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
    }
}
```

## Query Model (Read Side)

```java
// Read models are projections optimized for queries
public record OrderSummary(
    OrderId orderId,
    String customerName,
    Money totalAmount,
    String status,
    Instant createdAt
) {}

public record OrderDetails(
    OrderId orderId,
    CustomerInfo customer,
    List<OrderLineSummary> lines,
    Money subtotal,
    Money tax,
    Money total,
    String status,
    Instant createdAt,
    Instant placedAt
) {}

// Read model updater (reacts to events)
public class OrderReadModelUpdater {
    private final JdbcTemplate jdbcTemplate;

    @EventListener
    public void handle(OrderPlacedEvent event) {
        jdbcTemplate.update(
            "INSERT INTO order_read_models (id, customer_id, total, status, created_at) VALUES (?, ?, ?, ?, ?)",
            event.orderId().value(),
            event.customerId().value(),
            event.totalAmount().amount(),
            "PLACED",
            event.occurredAt()
        );
    }
}

// Query handler
public class OrderQueryHandler {
    private final JdbcTemplate jdbcTemplate;

    public OrderSummary getOrderSummary(OrderId orderId) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM order_read_models WHERE id = ?",
            (rs, rowNum) -> new OrderSummary(...),
            orderId.value()
        );
    }
}
```

## Bad/Good Examples

```java
// ❌ BAD: Traditional approach with same model for read/write
// Order entity has 50 fields, but summary view only needs 5
public class BadOrderService {
    public Order getOrder(OrderId id) {
        return orderRepository.findById(id); // Returns full entity with all 50 fields
    }

    public List<Order> getOrderSummaries() {
        // N+1 query problem or loading all fields for each row
        return orderRepository.findAll().stream()
            .map(order -> new OrderSummary(order.getId(), ...))
            .toList();
    }
}
```

```java
// ✅ GOOD: CQRS with optimized read and write models
// Write side: Rich domain model
public class Order extends AggregateRoot {
    private OrderId id;
    private CustomerId customerId;
    private List<OrderLine> lines;
    private OrderStatus status;

    public void place() { /* complex business logic */ }
}

// Read side: Optimized projection
public record OrderSummaryView(
    UUID orderId,
    String customerName,
    BigDecimal total,
    String status,
    LocalDateTime createdAt
) {}

// No business logic, just data
```

## Synchronization Strategies

### Synchronous (Same Process)

```java
public class OrderCommandHandler {
    private final OrderRepository orderRepository;
    private final OrderReadModelUpdater readModelUpdater;

    public void handle(CreateOrderCommand command) {
        Order order = Order.create(command.customerId());
        // ... create order ...
        orderRepository.save(order);

        // Update read model synchronously
        readModelUpdater.updateFrom(order.pullDomainEvents());
    }
}
```

### Asynchronous (Event-Driven)

```java
public class OrderCommandHandler {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public void handle(CreateOrderCommand command) {
        // ... create order ...
        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents()); // Async
    }
}

// Separate projection service consumes events
@EventListener
public class OrderProjectionService {
    public void handle(OrderPlacedEvent event) {
        // Update read model asynchronously
    }
}
```

## Common Pitfalls

- **CQRS for simple cases**: If read/write patterns are similar, CQRS adds unnecessary complexity
- **Eventual consistency confusion**: Read model lags behind write model. Users may see stale data.
- **Multiple read model drift**: Different consumers may receive events at different times, leading to inconsistent views
- **Command model leakage**: Read models accidentally capturing command-side structure
- **Over-engineering**: Starting with CQRS when a simpler approach would work

## When NOT to Use CQRS

- Simple CRUD applications with similar read/write patterns
- When strong consistency is required across all views
- Small teams that can't maintain multiple models
- When the complexity of synchronization is not worth the benefit

## CQRS + Event Sourcing

CQRS pairs naturally with Event Sourcing. The event store is the command-side persistence, and read models are projections built from the event stream.

```
Command Side:                     Query Side:
┌─────────────┐                  ┌─────────────┐
│  Aggregate  │ ───► Events ───► │ Read Model  │
│  (writes)   │                  │ (projections)│
└─────────────┘                  └─────────────┘
       │                                  ▲
       ▼                                  │
┌─────────────┐                           │
│ Event Store │ ───────────────────────────┘
└─────────────┘
```

## Real Implementation Reference

`apps/server/src/main/java/com/ai/application/command/` — Command handlers. `apps/server/src/main/java/com/ai/application/query/` — Query handlers with read models.

## Related References

- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Storing events as source of truth
- [Event-Driven Architecture](./event-driven-architecture.md) — System-level event patterns
- [Domain Event Pattern](./domain-event-pattern.md) — Events in CQRS
- [Saga Pattern](./saga-pattern.md) — Transactions in CQRS
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Layer separation
- [Software Architecture](../SKILL.md)
