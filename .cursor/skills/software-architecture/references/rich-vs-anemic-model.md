# Rich vs Anemic Domain Model

A comparison of two fundamentally different approaches to structuring domain objects: the Anemic Domain Model (anti-pattern) and the Rich Domain Model (recommended).

## When to Use

Consult this file when designing domain entities, when reviewing code for architecture quality, when deciding where business logic should live, or when refactoring from anemic to rich models.

## Core Idea

The debate between rich and anemic domain models is really about where business logic belongs. In a **Rich Domain Model**, entities contain both data and behavior. In an **Anemic Domain Model**, entities are just data containers with getters/setters, and all behavior lives in services.

## Comparison Table

| Characteristic | Anemic Domain Model (Anti-Pattern) | Rich Domain Model (Recommended) |
|----------------|-----------------------------------|--------------------------------|
| Entity content | Only fields + getter/setter | Fields + business behavior |
| Business logic location | Service layer | Inside domain objects |
| State changes | Service directly modifies fields | Domain object methods encapsulate changes |
| Validation logic | Service or utility classes | Domain object self-validation |
| Testability | Test service | Test domain objects |
| Encapsulation | Broken — fields exposed | Preserved — logic behind methods |
| Consistency | Hard to guarantee | Enforced by domain |

## Anemic Domain Model (Anti-Pattern)

```java
// Anemic Domain Model — fields with getter/setter only
public class AnemicOrder {
    private UUID id;
    private List<OrderLine> lines;
    private OrderStatus status;
    private Money totalAmount;

    // Only getter/setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Money getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Money totalAmount) { this.totalAmount = totalAmount; }
}
```

### Anemic Service Layer

```java
// Business logic lives in service — NOT in domain
public class AnemicOrderService {

    public void placeOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId);

        // Validation scattered across service methods
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException();
        }
        if (order.getLines().isEmpty()) {
            throw new OrderEmptyException();
        }

        // Direct field modification — no encapsulation
        order.setStatus(OrderStatus.PLACED);

        // Calculation outside of domain object
        Money total = Money.ZERO;
        for (OrderLine line : order.getLines()) {
            total = total.add(line.getPrice().multiply(line.getQuantity()));
        }
        order.setTotalAmount(total);

        orderRepository.save(order);
    }

    public void cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId);

        // Same validations duplicated
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new OrderCannotBeCancelledException();
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}
```

### Problems with Anemic Model

1. **Business logic duplication**: Same validations appear in multiple service methods
2. **Broken encapsulation**: Direct field modification bypasses business rules
3. **Hidden invariants**: It's unclear what states an Order can be in
4. **Hard to test in isolation**: Tests depend on the full service
5. **Domain model tells lies**: An Order object looks simple but has complex rules

## Rich Domain Model (Recommended)

```java
// Rich Domain Model — behavior inside the entity
public class Order extends AggregateRoot {
    private final OrderId id;
    private CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLine> lines;

    // Private constructor — use factory method
    private Order(CustomerId customerId) {
        this.id = OrderId.generate();
        this.customerId = customerId;
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }

    public static Order create(CustomerId customerId) {
        return new Order(customerId);
    }

    // Business behavior: Place order
    public void place() {
        if (this.status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException("Only draft order can be placed");
        }
        if (this.lines.isEmpty()) {
            throw new OrderEmptyException("Order must have at least one line");
        }
        this.status = OrderStatus.PLACED;
        addDomainEvent(new OrderPlacedEvent(this.id, this.customerId, calculateTotal(), Instant.now()));
    }

    // Business behavior: Cancel order
    public void cancel(String reason) {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new OrderCannotBeCancelledException(
                "Shipped or delivered orders cannot be cancelled"
            );
        }
        this.status = OrderStatus.CANCELLED;
        addDomainEvent(new OrderCancelledEvent(this.id, reason));
    }

    // Business behavior: Add line item
    public void addLine(Product product, int quantity) {
        if (this.status != OrderStatus.DRAFT) {
            throw new OrderCannotBeModifiedException(
                "Only draft orders can be modified"
            );
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.lines.add(new OrderLine(product.getId(), product.getPrice(), quantity));
    }

    // Calculation encapsulated
    public Money calculateTotal() {
        return lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(Money.ZERO, Money::add);
    }

    // Getters — no setters for state
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
    public Money getTotalAmount() { return calculateTotal(); }
}
```

### Lean Service Layer

```java
// Service only orchestrates — no business logic
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public void placeOrder(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.place(); // Domain object enforces rules

        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
    }

    public void cancelOrder(OrderId orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.cancel(reason); // Domain object enforces rules

        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
    }
}
```

## Benefits of Rich Domain Model

1. **Encapsulation preserved**: State can only change through valid operations
2. **Invariants enforced**: Business rules are guaranteed by the domain object
3. **Self-documenting code**: The entity shows what operations are valid
4. **Testability**: Business logic tested in domain objects, not services
5. **Consistency**: Rules applied consistently — can't be bypassed
6. **Rich Ubiquitous Language**: Entity methods match domain terminology

## Common Pitfalls

- **Over-rich models**: Putting ALL logic in entities leads to bloated objects. Use Domain Services for cross-entity logic.
- **Leaking persistence concerns**: Don't let domain objects know they're being persisted (avoid JPA annotations in domain).
- **Testing the wrong thing**: If you're unit-testing service methods that just delegate to entities, test the entity instead.
- **The "Transaction Script" pattern disguised as Rich Model**: Adding a few methods to an entity doesn't make it rich. True richness means all relevant business rules are encapsulated.

## Transitioning from Anemic to Rich

1. **Start with invariants**: What rules must always be true for an Order? Put them in the entity.
2. **Move validation**: From service to entity constructor or factory.
3. **Replace setters**: Replace `setStatus()` with meaningful methods like `place()`, `cancel()`, `ship()`.
4. **Hide collections**: Return immutable copies, not mutable lists.
5. **Extract services**: Move cross-entity logic to Domain Services.

## When Anemic Might Be Acceptable

- Simple CRUD applications with minimal business rules
- When the domain is trivial (e.g., a TODO app)
- Integration layers that are just data transfer
- Legacy systems where refactoring cost exceeds benefit

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/model/` — Rich domain entities with encapsulated behavior.

## Related References

- [Entity Pattern](./entity-pattern.md) — Designing rich entities
- [Value Object Pattern](./value-object-pattern.md) — Immutable domain concepts supporting rich models
- [Aggregate Pattern](./aggregate-pattern.md) — Rich aggregates with encapsulated consistency
- [Domain Service Pattern](./domain-service-pattern.md) — Domain logic for cross-entity operations
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Architecture that supports rich models
- [Software Architecture](../SKILL.md)
