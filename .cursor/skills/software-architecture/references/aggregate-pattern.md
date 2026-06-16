# Aggregate Pattern

A cluster of related domain objects (entities and value objects) treated as a single unit for data changes. External access is only permitted through the Aggregate Root.

## When to Use

Consult this file when designing the consistency boundaries of your domain, when deciding which entities can be directly accessed from outside, or when defining transaction scopes.

## Core Idea

An Aggregate is a consistency boundary that groups related objects. Everything inside the boundary is kept consistent together. Everything outside only interacts with the aggregate through a single entry point — the Aggregate Root.

### Key Principles

1. **Aggregate Root**: One entity per aggregate — the single entry point for all external access
2. **External references only to Root**: External code holds references to the root, never to internal entities
3. **Invariant enforcement**: The aggregate root ensures all business rules within the boundary are satisfied
4. **Single transaction**: All changes within an aggregate are persisted atomically

### Aggregate vs Entity

| Aspect | Aggregate | Entity |
|--------|-----------|--------|
| Scope | Group of related objects | Single object with identity |
| Access | Through root only | Directly accessible |
| Transaction | Entire aggregate in one transaction | Part of an aggregate |
| Boundary | Defines consistency boundary | Can be root or part of aggregate |

### When to Use Aggregates

- When a group of objects must be kept consistent together
- When external code should not directly manipulate internal entities
- When a transaction boundary is needed for a group of objects

## Core Idea

```java
// Aggregate root: Order is the access point for OrderLine
public class Order extends AggregateRoot {
    private OrderId id;
    private List<OrderLine> lines;  // Internally managed, not directly exposed

    // External access only through aggregate root
    public void addLine(Product product, int quantity) {
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

    private void validateLine(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        // Business rules for adding lines
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// Incorrect: Exposing internal implementation
// public class BadOrder {
//     public List<OrderLine> lines;  // Directly exposed, can be modified externally
// }
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: Exposing internal entities directly
public class BadOrder {
    public List<OrderLine> lines; // Exposed! External code can modify directly

    public void place() {
        // This validation is fragile — if lines is modified directly, place() is invalid
        if (lines.isEmpty()) throw new OrderEmptyException();
    }
}

// Usage of BAD pattern
BadOrder order = new BadOrder();
order.lines.add(new OrderLine(...)); // Bypass validation!
order.lines.clear(); // Bypass validation!
order.place(); // Now in invalid state
```

```java
// ❌ BAD: External code manipulating internal entities
public class BadOrderService {
    private final OrderRepository orderRepository;

    public void modifyOrderLine(OrderId orderId, OrderLineId lineId, int newQuantity) {
        Order order = orderRepository.findById(orderId);
        // Directly accessing internal OrderLine — breaks aggregate encapsulation
        OrderLine line = order.getLines().stream()
            .filter(l -> l.getId().equals(lineId))
            .findFirst()
            .orElseThrow();
        line.setQuantity(newQuantity); // Direct modification!
        orderRepository.save(order);
    }
}
```

```java
// ✅ GOOD: All modifications through aggregate root
public class Order extends AggregateRoot {
    private OrderId id;
    private List<OrderLine> lines;

    public void addLine(Product product, int quantity) {
        validateCanModify();
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        lines.add(new OrderLine(product.getId(), product.getPrice(), quantity));
    }

    public void updateLineQuantity(OrderLineId lineId, int newQuantity) {
        validateCanModify();
        OrderLine line = findLine(lineId);
        line.updateQuantity(newQuantity);
        recalculateTotal();
    }

    public void removeLine(OrderLineId lineId) {
        validateCanModify();
        lines.removeIf(line -> line.getId().equals(lineId));
        recalculateTotal();
    }

    private OrderLine findLine(OrderLineId lineId) {
        return lines.stream()
            .filter(line -> line.getId().equals(lineId))
            .findFirst()
            .orElseThrow(() -> new OrderLineNotFoundException(lineId));
    }

    private void validateCanModify() {
        if (this.status != OrderStatus.DRAFT) {
            throw new OrderCannotBeModifiedException("Only draft orders can be modified");
        }
    }

    // Expose immutable view
    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }
}

// ✅ GOOD: Service works through aggregate root
public class OrderService {
    private final OrderRepository orderRepository;

    public void updateLineQuantity(OrderId orderId, OrderLineId lineId, int newQuantity) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.updateLineQuantity(lineId, newQuantity); // All logic in aggregate
        orderRepository.save(order);
    }
}
```

## Aggregate Design Guidelines

### Keep Aggregates Small

- One aggregate = one transaction = one consistency boundary
- Large aggregates cause concurrency issues and performance problems
- If two entities must be changed together frequently, they may belong in the same aggregate

### Reference Other Aggregates by ID

```java
public class Order extends AggregateRoot {
    private CustomerId customerId; // Reference by ID, not by reference
    private List<OrderLine> lines;

    // If you need the customer:
    // - Call customerRepository.findById(customerId)
    // - Do NOT hold a reference to Customer entity
}
```

### Separate Aggregates for Separate Consistency Requirements

```
┌─────────────────────────────────────────────────────┐
│ Order Aggregate                      │ Inventory    │
│                                      │ Aggregate    │
│ Order ← OrderLine                    │ Inventory ←  │
│                                      │ StockItem    │
│ Each aggregate has its own consistency boundary      │
│ Changes to Order do NOT automatically change        │
│ Inventory — use events/Saga for coordination        │
└─────────────────────────────────────────────────────┘
```

## Common Pitfalls

- **Large aggregates**: Including too many entities in one aggregate causes lock contention and performance issues
- **Direct exposure of internal entities**: Returning `List<OrderLine>` lets external code modify the list
- **Cross-aggregate references**: Holding references to entities in other aggregates creates coupling and violates aggregate boundaries
- **Transaction too wide**: Trying to persist multiple large aggregates in one transaction causes performance issues

## When NOT to Use Aggregates

- For simple CRUD where every entity is independent
- When consistency across aggregates is not required
- When performance demands single-object transactions only

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/model/` — Aggregates like `Order` that manage internal entities.

## Related References

- [Entity Pattern](./entity-pattern.md) — Entities that can serve as aggregate roots
- [Value Object Pattern](./value-object-pattern.md) — Value objects contained within aggregates
- [Repository Pattern](./repository-pattern.md) — Repository operates on aggregate roots
- [Domain Event Pattern](./domain-event-pattern.md) — Aggregates publish events
- [Saga Pattern](./saga-pattern.md) — Managing consistency across aggregates
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Aggregates in layered architecture
- [Software Architecture](../SKILL.md)
