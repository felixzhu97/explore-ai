# Repository Pattern

A collection-like abstraction that provides access to aggregates. Repositories persist and retrieve aggregates, hiding the storage mechanism from domain code.

## When to Use

Consult this file when designing data access for aggregates, when you need to decouple domain code from persistence details, or when deciding where repository implementations should live.

## Core Idea

The Repository pattern provides the illusion of an in-memory collection of aggregates. Domain code uses repositories without knowing whether data is stored in a database, a file, or an external service. Repository interfaces are defined in the Domain layer, and implementations live in the Infrastructure layer.

### Key Principles

1. **Repository interfaces in Domain layer**: The domain defines what data access it needs
2. **Implementations in Infrastructure layer**: Implementations handle the actual persistence technology
3. **Only aggregate roots**: Repositories work with aggregates, not individual entities or value objects
4. **Collection-like API**: `findById`, `save`, `delete`, and optionally `findBy...` queries

### Repository vs DAO

| Aspect | Repository | DAO (Data Access Object) |
|--------|-----------|--------------------------|
| Focus | Aggregate roots, domain concepts | Data structures, tables |
| API | Collection-like (find, save, delete) | CRUD operations, arbitrary queries |
| Location | Domain layer interface, Infrastructure implementation | Often tied to a specific table |
| Philosophy | Domain-driven, persistence ignorance | Data-source driven |

## Core Idea

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

## Bad/Good Examples (Java)

```java
// ❌ BAD: Repository interface in Infrastructure layer
// This violates dependency rules — domain depends on infrastructure
package com.ai.infrastructure.persistence;

public interface JpaOrderRepository { // Wrong layer!
    Order findById(UUID id);
    void save(Order order);
}

// ❌ BAD: Repository returning JPA entities
public interface BadOrderRepository {
    Optional<OrderEntity> findById(UUID id); // Entity with JPA annotations!
}

// ❌ BAD: Repository operating on non-aggregate-root entities
public interface BadOrderRepository {
    void saveLine(OrderLine line); // Wrong! OrderLine is not an aggregate root
    void updateLineQuantity(OrderLineId id, int quantity); // Direct access to internal entity
}
```

```java
// ✅ GOOD: Repository interface in Domain layer
package com.ai.domain.repository;

public interface OrderRepository {
    Optional<Order> findById(OrderId id);

    // Query methods return aggregates or value objects, not entities
    List<Order> findByCustomerId(CustomerId customerId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    void save(Order order);

    void delete(OrderId id);
}
```

```java
// ✅ GOOD: Infrastructure implementation
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
    public Optional<Order> findByIdWithLines(OrderId id) {
        // Explicit loading strategy for complex aggregates
        return delegate.findWithLinesById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Order order) {
        JpaOrder entity = mapper.toEntity(order);
        delegate.save(entity);
    }
}
```

```java
// ✅ GOOD: In-memory implementation for testing
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Order order) {
        store.put(order.getId(), order);
    }
}
```

## Repository Design Patterns

### Specification Pattern

For complex queries, define specifications that can be composed:

```java
public interface OrderSpecification {
    boolean isSatisfiedBy(Order order);
}

public class OrderStatusSpecification implements OrderSpecification {
    private final OrderStatus status;

    public OrderStatusSpecification(OrderStatus status) {
        this.status = status;
    }

    @Override
    public boolean isSatisfiedBy(Order order) {
        return order.getStatus() == status;
    }
}

// Usage
List<Order> orders = orderRepository.findAll(
    order -> order.getStatus() == OrderStatus.PLACED &&
             order.getCustomerId().equals(customerId)
);
```

### Lazy Loading vs Eager Loading

```java
public interface OrderRepository {
    // Lazy load — lines loaded separately
    Optional<Order> findById(OrderId id);

    // Eager load — load order with all lines in one query
    Optional<Order> findByIdWithLines(OrderId id);

    // Batch loading for lists
    List<Order> findByCustomerIdWithLines(CustomerId customerId);
}
```

## Common Pitfalls

- **Repository in wrong layer**: Interface must be in Domain, implementation in Infrastructure
- **Repository returning infrastructure entities**: Domain should not know about JPA entities
- **Repository operating on non-roots**: Only aggregate roots have repositories; internal entities are modified through their root
- **Leaky abstractions**: Exposing JPA-specific concepts (e.g., `Pageable`) in domain interfaces
- **Transactional boundaries**: Repositories should not manage transactions; the application layer does

## When NOT to Use Repository

- For simple CRUD with no domain logic
- When aggregates are very small and simple
- For read-only projections that don't map to aggregates (use query objects instead)

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/repository/` — Repository interfaces. `apps/server/src/main/java/com/ai/infrastructure/persistence/repository/` — JPA implementations.

## Related References

- [Aggregate Pattern](./aggregate-pattern.md) — Repositories manage aggregates
- [Entity Pattern](./entity-pattern.md) — Entities that can be aggregate roots
- [CQRS Pattern](./cqrs-pattern.md) — Separate read/write repositories
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Repository placement in layers
- [Software Architecture](../SKILL.md)
