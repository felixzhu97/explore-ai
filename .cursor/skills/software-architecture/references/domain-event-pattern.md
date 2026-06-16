# Domain Event Pattern

Immutable records of significant business occurrences within the domain. Domain events enable loose coupling between bounded contexts and within the domain itself.

## When to Use

Consult this file when decoupling parts of the system, when you need to trigger side effects from domain changes, when building audit trails, or when implementing event-driven architectures.

## Core Idea

A Domain Event is a record of something that happened in the domain. When an aggregate's state changes in a significant way, it publishes an event. Other parts of the system can subscribe to these events and react accordingly — without the aggregate knowing who consumes its events.

### Event Anatomy

- **Name**: Past tense verb describing what happened (e.g., `OrderPlaced`, `PaymentReceived`)
- **Payload**: Data relevant to the event (IDs, timestamps, changed values)
- **Metadata**: When it happened, what caused it

### Event vs Command

| Aspect | Domain Event | Command |
|--------|-------------|---------|
| Direction | Outbound (from domain to outside) | Inbound (from outside to domain) |
| Naming | Past tense (`OrderPlaced`) | Imperative (`PlaceOrder`) |
| Expectation | No response expected | Response expected |
| Consumer | Unknown to publisher | Known to sender |

## Core Idea

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

## Domain Event Base Class

```java
// Base class for all domain events (optional but recommended)
public interface DomainEvent {
    Instant occurredAt();
}

// Implementation using record
public record OrderPlacedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    Instant occurredAt
) implements DomainEvent {

    public static OrderPlacedEvent create(Order order) {
        return new OrderPlacedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            Instant.now()
        );
    }
}
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: Domain event with mutable state (not immutable!)
public class MutableOrderPlacedEvent {
    private OrderId orderId; // Mutable!

    public void setOrderId(OrderId id) { this.orderId = id; } // Setters break immutability!
}

// ❌ BAD: Event carrying infrastructure concepts
public class OrderPlacedEvent {
    private OrderId orderId;
    private UUID jpaPersistenceVersion; // Infrastructure concern!
    private Connection dbConnection; // Absolutely not!
}

// ❌ BAD: Side effects directly in entity
public class Order {
    public void place() {
        this.status = OrderStatus.PLACED;
        // Directly calling infrastructure — violates dependency rules
        emailService.sendConfirmationEmail(this);
        inventoryService.reserveStock(this.lines);
    }
}
```

```java
// ✅ GOOD: Immutable domain event using Java record
public record OrderPlacedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    List<OrderLineItem> lineItems,
    Instant occurredAt
) implements DomainEvent {

    // Validation in compact constructor
    public OrderPlacedEvent {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(totalAmount);
        Objects.requireNonNull(lineItems);
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }
}
```

```java
// ✅ GOOD: Aggregate publishes events, infrastructure handles them
public class Order extends AggregateRoot {
    private final List<DomainEvent> events = new ArrayList<>();

    public void place() {
        if (status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException();
        }
        if (lines.isEmpty()) {
            throw new OrderEmptyException();
        }

        this.status = OrderStatus.PLACED;

        // Publish domain event — domain doesn't know who handles it
        addDomainEvent(OrderPlacedEvent.create(this));
    }

    private void addDomainEvent(DomainEvent event) {
        events.add(event);
    }

    @Override
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = new ArrayList<>(events);
        events.clear();
        return pulled;
    }
}
```

```java
// ✅ GOOD: Event handler in infrastructure layer
@EventListener
public class OrderDomainEventHandler {

    private final EmailService emailService;
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    @Async
    public void handle(OrderPlacedEvent event) {
        emailService.sendOrderConfirmation(event.orderId());
        // Publish integration event for other bounded contexts
        eventPublisher.publish(
            new OrderPlacedIntegrationEvent(event.orderId(), event.customerId())
        );
    }

    @Async
    public void handle(OrderCancelledEvent event) {
        inventoryService.releaseReservedStock(event.orderId());
        emailService.sendCancellationNotice(event.orderId(), event.reason());
    }
}
```

## Event Publishing Pipeline

```
┌─────────────┐     pullDomainEvents()     ┌──────────────────┐
│   Aggregate │ ─────────────────────────► │  Application     │
│   (Domain)  │                            │  (pulls events)  │
└─────────────┘                            └────────┬─────────┘
                                                      │
                                                      ▼
                                             ┌──────────────────┐
                                             │  Event Publisher │
                                             │  (Infrastructure) │
                                             └────────┬─────────┘
                                                      │
                                    ┌─────────────────┼─────────────────┐
                                    ▼                 ▼                 ▼
                             ┌───────────┐    ┌───────────┐    ┌───────────┐
                             │  Handler  │    │  Handler  │    │  Handler  │
                             │  Email    │    │  Inventory│    │  Audit    │
                             └───────────┘    └───────────┘    └───────────┘
```

## Common Pitfalls

- **Events carrying mutable state**: Events must be immutable. Once published, they cannot change.
- **Events containing infrastructure concepts**: Events are part of the domain. Don't include JPA entities, database IDs, or infrastructure classes.
- **Side effects in entities**: Entities should publish events, not directly call services. The side effects happen in handlers.
- **Too many events**: Not every state change needs an event. Publish events for significant business occurrences, not every minor change.
- **Events without versioning**: When event schema changes, old events may not deserialize correctly. Include a version field.
- **Forgetting to clear events**: Always call `pullDomainEvents()` after processing. Forgetting causes events to be processed twice.

## When NOT to Use Domain Events

- For simple CRUD where side effects are not needed
- When strong consistency is required and async processing is not acceptable
- When the overhead of event infrastructure is not worth the benefit

## Event Versioning

```java
// Include version in event for schema evolution
public record OrderPlacedEvent(
    int version, // Always include version
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    Instant occurredAt
) implements DomainEvent {

    public static final int CURRENT_VERSION = 1;
}
```

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/event/` — Domain event definitions. `apps/server/src/main/java/com/ai/infrastructure/messaging/` — Event publishers and handlers.

## Related References

- [Entity Pattern](./entity-pattern.md) — Entities that publish events
- [Aggregate Pattern](./aggregate-pattern.md) — Aggregates manage event publishing
- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Events as the source of truth
- [Event-Driven Architecture](./event-driven-architecture.md) — System-level event patterns
- [Saga Pattern](./saga-pattern.md) — Events for distributed transactions
- [Software Architecture](../SKILL.md)
