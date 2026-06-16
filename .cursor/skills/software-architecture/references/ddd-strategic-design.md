# DDD Strategic Design

A methodology for managing complexity in large domains by partitioning the system into bounded contexts and establishing relationships between them.

## When to Use

Consult this file when decomposing a large domain into manageable pieces, designing microservice boundaries, resolving naming conflicts between teams, or mapping integrations between systems.

## Core Idea

Domain-Driven Design has two sides: **Strategic Design** (what to build, boundaries, relationships) and **Tactical Design** (how to build, patterns, code). Strategic Design comes first.

### Bounded Context

A Bounded Context is an explicit boundary around a semantic area. Each context has its own:

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
│  Team: Order    │    │ Team: Warehouse │    │  Team: Payment  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Why Bounded Contexts?

Without bounded contexts, teams fight over:
- Shared entity names that mean different things (e.g., "Customer" in Order vs. "Customer" in CRM)
- One team's changes breaking another's models
- Ubiquitous Language that nobody actually uses

With bounded contexts:
- Each team owns their model and language
- Cross-context communication happens through well-defined interfaces
- Models stay coherent and small

### Core Domain Classification

Not all parts of your domain deserve equal investment. Classify each bounded context:

| Type | Description | Investment |
|------|-------------|------------|
| **Core Domain** | Core competency, unique value proposition | Maximum investment, carefully crafted |
| **Supporting Domain** | Supports the core domain | Moderate investment |
| **Generic Domain** | Generic solutions, can be purchased | Minimal investment |

### Context Mapping Patterns

How bounded contexts relate to each other:

- **Shared Kernel**: Subset of shared domain model between two contexts (common but must be kept small)
- **Customer/Supplier**: Upstream/downstream relationship where downstream is a customer of upstream
- **Conformist**: Downstream team follows the upstream team's model (eliminates translation but creates coupling)
- **Anticorruption Layer**: Translation layer that isolates a downstream context from an upstream model it doesn't control
- **Open Host Service**: Define a protocol that others can use to interact with your context
- **Published Language**: A common language (e.g.,UBL, CCTS) used for integration

### Tactical Design Index

Tactical patterns implement the domain model inside a bounded context:

| Pattern | Description | Reference |
|---------|-------------|-----------|
| **Entity** | Objects with unique identity whose lifecycle can continue | [Entity Pattern](./entity-pattern.md) |
| **Value Object** | Immutable, equality based on attribute values | [Value Object Pattern](./value-object-pattern.md) |
| **Aggregate** | Consistency boundary, accessed externally through the root | [Aggregate Pattern](./aggregate-pattern.md) |
| **Repository** | Collection abstraction for aggregates | [Repository Pattern](./repository-pattern.md) |
| **Domain Service** | Cross-entity business logic | [Domain Service Pattern](./domain-service-pattern.md) |
| **Domain Event** | Decoupling via important domain events | [Domain Event Pattern](./domain-event-pattern.md) |

## Bad/Good Examples (Java)

```java
// ❌ BAD: Single God Model spanning multiple bounded contexts
// This "Customer" entity tries to be everything to everyone
public class Customer {
    private String customerId;
    private OrderHistory orders;      // Order Context
    private CreditScore creditScore;   // Payment Context
    private WarehousePreference warehouse; // Inventory Context
    // This entity will become a maintenance nightmare
}
```

```java
// ✅ GOOD: Separate domain models per bounded context
// Order Context
public class OrderContext {
    // Order aggregate owns "order customer" — a subset relevant to ordering
    public record OrderCustomer(
        CustomerId id,
        Email email,
        ShippingAddress shippingAddress
    ) {}
}

// Inventory Context
public class InventoryContext {
    // Inventory's own "customer" model — focused on fulfillment
    public record FulfillmentCustomer(
        CustomerId id,
        Warehouse warehouse,
        PickPreference preference
    ) {}
}

// Context Mapping: Anticorruption Layer at boundary
public class CustomerContextMapper {
    public FulfillmentCustomer toFulfillmentCustomer(OrderCustomer customer) {
        return new FulfillmentCustomer(
            customer.id(),
            determineWarehouse(customer.shippingAddress()),
            determinePreference(customer)
        );
    }
}
```

```java
// ✅ GOOD: Context Mapping — Customer/Supplier relationship
// The Order Context (downstream) depends on the Customer Context (upstream)
// Define a Published Language / Open Host Service in Customer Context

// Customer Context exposes a well-defined API
public interface CustomerService {
    CustomerDto findById(CustomerId id);
    CustomerCreditInfo getCreditInfo(CustomerId id);
}

// Order Context uses the published interface (not the internal model)
public class OrderApplicationService {
    private final CustomerService customerService; // Defined and owned by Customer Context

    public void placeOrder(OrderCommand command) {
        CustomerCreditInfo credit = customerService.getCreditInfo(command.customerId());
        // Use credit info for risk assessment
    }
}
```

## Common Pitfalls

- **Creating too many bounded contexts**: Start with larger contexts and split when needed, not the other way around
- **Shared Kernel abuse**: It seems convenient but creates coupling. Use only when two contexts truly share a small, stable model
- **Fighting over a "canonical" model**: If contexts can't agree on a single model, that's a signal to keep them separate
- **Ignoring Generic Domains**: Don't over-invest in generic capabilities. Buy rather than build.
- **Cross-context references**: Direct references across bounded contexts (e.g., `order.getCustomer().getCreditScore()`) break the boundary

## When NOT to Use

Strategic DDD is overkill for:
- Small, simple applications with a single team
- CRUD-heavy systems with minimal business rules
- Projects where the domain is well-understood and stable
- When the cost of boundary management outweighs the benefits

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/` — Domain models scoped to a bounded context. Cross-context integration points should be in infrastructure layer.

## Related References

- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Layer model for implementing a bounded context
- [Aggregate Pattern](./aggregate-pattern.md) — Consistency boundaries within a bounded context
- [Entity Pattern](./entity-pattern.md) — Entities with unique identity
- [Domain Event Pattern](./domain-event-pattern.md) — Cross-context communication via events
- [C4 Model](./c4-model.md) — Documentation method complementary to DDD bounded contexts
- [Software Architecture](../SKILL.md)
- [Software Development](../../software-development/SKILL.md)
