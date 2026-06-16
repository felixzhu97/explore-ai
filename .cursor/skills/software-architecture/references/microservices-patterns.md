# Microservices Design Patterns

A collection of patterns for designing and coordinating microservices — covering decomposition, communication, data management, and transaction consistency.

## When to Use

Consult this file when decomposing a monolith into services, designing service boundaries, choosing communication protocols between services, or solving consistency challenges in distributed systems.

## Core Idea

Microservices is not just "small services" — it's an architectural style defined by independently deployable services, own databases, and organizational alignment. The patterns below address the key challenges.

### Pattern Comparison

| Pattern | Characteristics | Applicable Scenarios |
|---------|----------------|----------------------|
| **Aggregates** | Monolithic architecture, bounded contexts | Small teams, moderate complexity |
| **Event-Driven** | Async collaboration via events | Independent deployment, high concurrency |
| **Saga** | Distributed transactions via compensating actions | Cross-service consistency |
| **CQRS** | Read/write model separation | Large read/write ratio differences |

### Communication Patterns

```
Sync communication:                        Async communication:
┌─────┐    REST/gRPC    ┌─────┐   ┌─────┐    Event    ┌─────┐
│  A  │ ───────────────► │  B  │   │  A  │ ─────────► │  B  │
└─────┘                  └─────┘   └─────┘            └─────┘
      Response                  Publish/Subscribe     Consume/Process
```

### Sync vs Async Communication

| Aspect | Synchronous (REST/gRPC) | Asynchronous (Events) |
|--------|------------------------|-----------------------|
| Coupling | Tight — caller knows callee | Loose — publisher doesn't know consumers |
| Latency | Low for simple calls | Higher due to broker overhead |
| Consistency | Can use distributed transactions | Eventual consistency only |
| Use case | Query, simple commands | Complex workflows, fan-out |

## Decomposition Patterns

### Decompose by Business Capability

Organize services around business capabilities (e.g., Order Management, Customer Management, Payment).

**Good for**: Well-understood domains with stable boundaries.

### Decompose by Subdomain (DDD)

Use DDD bounded contexts as service boundaries. See [DDD Strategic Design](./ddd-strategic-design.md).

**Good for**: Complex domains where bounded contexts are already identified.

### Strangler Fig

Incrementally migrate from a monolith by routing new functionality to services while leaving the monolith to handle existing features.

**Good for**: Gradual migration without big-bang rewrites.

## Data Management Patterns

### Database per Service

Each service owns its database (schema). No direct cross-database joins.

**Benefits**: Services are truly independent, different databases for different needs.

**Challenges**: Transactions across services require saga patterns.

### Shared Database Anti-Pattern

Multiple services sharing the same database/schema.

**Symptoms**: Services become tightly coupled, deployment conflicts, shared schema changes affect multiple services.

### Event Sourcing

Store events rather than current state. Enables audit trails and state reconstruction.

See [Event Sourcing Pattern](./event-sourcing-pattern.md).

### CQRS

Separate read and write models. Write side uses events; read side materializes projections.

See [CQRS Pattern](./cqrs-pattern.md).

## Transaction Patterns

### Saga Pattern

Orchestrate distributed transactions through a sequence of local transactions, with compensating actions for rollback.

See [Saga Pattern](./saga-pattern.md).

### Two-Phase Commit (Avoid)

Distributed locking protocol. Blocks resources during commit phase. Not recommended for high-throughput systems due to coordinator becoming a single point of failure.

## Communication Patterns

### API Gateway

Single entry point for all clients. Handles authentication, routing, protocol translation.

**Benefits**: Simplified client experience, cross-cutting concerns centralized.

**Drawbacks**: Additional hop, potential bottleneck.

### Backend for Frontend (BFF)

Separate API gateway per frontend type (web, mobile, third-party). Each BFF is optimized for its client's needs.

### Service Mesh

Infrastructure layer that handles service-to-service communication (mTLS, retries, circuit breakers, observability) without application code changes.

Tools: Istio, Linkerd, Envoy.

## Cross-Cutting Patterns

### Circuit Breaker

Prevent cascading failures by failing fast when a downstream service is unhealthy.

```java
public class CircuitBreaker {
    private State state = State.CLOSED;
    private int failureCount = 0;

    public void recordSuccess() {
        failureCount = 0;
        state = State.CLOSED;
    }

    public void recordFailure() {
        if (++failureCount >= threshold) {
            state = State.OPEN;
        }
    }

    public boolean allowRequest() {
        return state != State.OPEN;
    }
}
```

### Retry with Exponential Backoff

Automatically retry failed requests with increasing delays.

### Bulkhead

Isolate resources (connection pools, thread pools) per service to prevent one service's failures from taking down others.

## Bad/Good Examples

```java
// ❌ BAD: Chatty synchronous communication between microservices
// OrderService calls InventoryService for every line item
public class BadOrderService {
    public void placeOrder(Order order) {
        for (OrderLine line : order.getLines()) {
            // This creates N calls to InventoryService
            boolean available = inventoryClient.checkStock(line.productId(), line.quantity());
            if (!available) throw new OutOfStockException();
        }
        paymentClient.charge(order.getTotal());
        orderRepository.save(order);
    }
}

// ✅ GOOD: Batch check with circuit breaker
public class GoodOrderService {
    private final CircuitBreaker inventoryBreaker = new CircuitBreaker();

    public void placeOrder(Order order) {
        if (!inventoryBreaker.allowRequest()) {
            throw new ServiceUnavailableException("Inventory service unavailable");
        }
        // Single batch call
        Map<ProductId, StockStatus> stockStatuses = inventoryClient
            .checkStockBatch(order.getLineItems());
        // ... rest of logic
    }
}
```

```java
// ❌ BAD: Nanoservice — too many tiny services
// Every entity becomes a microservice
// CustomerService, CustomerAddressService, CustomerPreferencesService,
// CustomerContactService... — operational nightmare
```

## Common Pitfalls

- **Microservice premium**: Over-decomposing into too many services adds operational overhead
- **Shared database**: Defeats the purpose of microservices — creates tight coupling
- **Chatty services**: Excessive service-to-service calls cause latency and cascading failures
- **Ignoring failures**: No circuit breaker, retry logic, or fallback — cascading failures
- **Distributed monolith**: Services are deployed independently but share a database and synchronously call each other for every operation — gains none of the benefits

## When NOT to Use Microservices

- Small team (< 5 developers)
- Simple, stable domain
- High operational maturity required (monitoring, deployment, service mesh)
- Strict latency requirements (synchronous overhead)

## Real Implementation Reference

`apps/server/` — If services share a module, they may still be a modular monolith rather than true microservices.

## Related References

- [DDD Strategic Design](./ddd-strategic-design.md) — Bounded contexts as service boundaries
- [Saga Pattern](./saga-pattern.md) — Distributed transaction management
- [CQRS Pattern](./cqrs-pattern.md) — Read/write separation
- [Event-Driven Architecture](./event-driven-architecture.md) — Async communication between services
- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Event store for microservices
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Layer model within a service
- [Software Architecture](../SKILL.md)
