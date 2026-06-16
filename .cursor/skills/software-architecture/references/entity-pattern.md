# Entity Pattern

Objects with unique identity whose lifecycle can continue over time, as opposed to value objects which are identified by their attributes.

## When to Use

Consult this file when designing domain objects that represent something with a distinct identity (e.g., Order, Customer, Product), when modeling business concepts whose state changes matter over time, or when deciding between Entity and Value Object for a domain concept.

## Core Idea

An Entity (or Reference Object) is defined by its identity, not by its attributes. Two entities with identical attributes but different IDs are considered different objects. Entities encapsulate state and behavior related to that identity's lifecycle.

### Entity vs Value Object

| Aspect | Entity | Value Object |
|--------|--------|--------------|
| Identity | Has a unique, stable identity | No identity; identified by attributes |
| Mutability | Usually mutable (state changes) | Immutable (replace rather than modify) |
| Equality | By identity (ID) | By attribute values |
| Lifecycle | Matters (order placed, shipped, delivered) | Doesn't matter independently |

### When to Use Entity

Use an Entity when:
- The object represents a "thing" in the domain with a distinct identity
- The object has a lifecycle (created, modified, deleted)
- You need to track changes to a specific instance
- Two instances with the same attributes are still different (e.g., two orders for the same product)

### When to Use Value Object

Use a Value Object when:
- The object describes a characteristic or measurement
- You care about the attributes, not the identity
- The object should be immutable
- Multiple instances with same attributes are interchangeable (e.g., two $20 Money objects)

## Core Idea

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

### Entity Identity

```java
// Value-based ID (recommended for DDD)
public record OrderId(UUID value) {
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
}

// Alternative: String-based ID for external representation
public record OrderId {
    private final String value;

    public OrderId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public String value() { return value; }
}
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: Anemic Entity — only fields and getters/setters
public class BadOrder {
    private UUID id;
    private List<OrderLine> lines;
    private OrderStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    // No behavior! Business logic must live somewhere else
}

// ❌ BAD: Business logic in service layer — not encapsulated
public class BadOrderService {
    public void placeOrder(Order order) {
        // Business logic should be in the entity
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException();
        }
        if (order.getLines().isEmpty()) {
            throw new OrderEmptyException();
        }
        order.setStatus(OrderStatus.PLACED); // Direct field modification!
    }
}

// ✅ GOOD: Rich Entity with encapsulated behavior
public class Order extends AggregateRoot {
    private OrderId id;
    private CustomerId customerId;
    private List<OrderLine> lines;
    private OrderStatus status;

    public static Order create(CustomerId customerId) {
        Order order = new Order();
        order.id = OrderId.generate();
        order.customerId = customerId;
        order.lines = new ArrayList<>();
        order.status = OrderStatus.DRAFT;
        return order;
    }

    public void addLine(Product product, int quantity) {
        if (status != OrderStatus.DRAFT) {
            throw new OrderCannotBeModifiedException("Only draft orders can be modified");
        }
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        lines.add(new OrderLine(product.getId(), product.getPrice(), quantity));
    }

    public void place() {
        if (status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException("Only draft order can be placed");
        }
        if (lines.isEmpty()) {
            throw new OrderEmptyException("Order must have at least one line");
        }
        this.status = OrderStatus.PLACED;
        addDomainEvent(new OrderPlacedEvent(this.id, this.customerId, calculateTotal(), Instant.now()));
    }

    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new OrderCannotBeCancelledException("Shipped or delivered orders cannot be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        addDomainEvent(new OrderCancelledEvent(this.id, reason));
    }

    private Money calculateTotal() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    // Getters — no setters for state fields
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return List.copyOf(lines); } // Immutable view
}

// ✅ GOOD: Service only orchestrates, entity handles business logic
public class GoodOrderService {
    private final OrderRepository orderRepository;

    public void placeOrder(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.place(); // Entity encapsulates business logic

        orderRepository.save(order); // Service handles persistence coordination
    }
}
```

## Common Pitfalls

- **Anemic Domain Model**: Entities with only getters/setters and no behavior. Business logic scatters across services.
- **Exposing internal state**: Returning mutable collections or directly exposing fields breaks encapsulation.
- **Multiple identity concepts**: Some objects have multiple ID fields. Choose one as the primary identity.
- **Identity generation timing**: Decide when to generate IDs — at creation, at persistence, or from external systems.
- **Entity equals/hashCode**: For JPA/Hibernate, entities need proper equals/hashCode. Use the ID field, not all attributes.

## Entity Lifecycle

```java
// Creation phase
public class Order {
    public static Order create(CustomerId customerId) { ... } // Factory method
    private Order() { } // Prevent direct instantiation
}

// Modification phase
public void addLine(Product product, int quantity) { ... }
public void place() { ... }
public void cancel(String reason) { ... }

// Deletion phase — usually handled by repository
// repository.delete(orderId);
```

## When NOT to Use Entity

- For transient data objects that have no identity beyond their content — use a Value Object or DTO
- For pure data transfer between layers without domain logic — use a DTO
- For database record representations — use a JPA entity in Infrastructure layer, not in Domain

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/model/` — Domain entities with rich behavior.

## Related References

- [Value Object Pattern](./value-object-pattern.md) — Immutable objects identified by value, not identity
- [Aggregate Pattern](./aggregate-pattern.md) — Entity that serves as access point for related objects
- [Repository Pattern](./repository-pattern.md) — Collection abstraction for entities
- [Domain Event Pattern](./domain-event-pattern.md) — Entities publish events on state changes
- [Rich vs Anemic Model](./rich-vs-anemic-model.md) — Comparison of rich vs anemic domain models
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Entity placement in layered architecture
- [Software Architecture](../SKILL.md)
