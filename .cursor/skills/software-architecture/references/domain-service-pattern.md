# Domain Service Pattern

A stateless service that encapsulates business logic which cannot be attributed to a single entity or value object. Used when an operation conceptually belongs to the domain but doesn't fit inside any one entity.

## When to Use

Consult this file when you find business logic that involves multiple entities, when an operation doesn't naturally belong to any single entity, or when placing logic in an entity would break encapsulation.

## Core Idea

Domain Services host domain logic that doesn't naturally fit inside a single entity or value object. They are stateless — they don't hold state, they perform operations. Unlike Application Services (which orchestrate use cases), Domain Services contain genuine domain logic.

### When Entity vs Domain Service

| Situation | Solution |
|-----------|----------|
| Behavior belongs to one entity | Put it in the entity |
| Behavior involves one entity + value objects | Put it in the entity |
| Behavior involves multiple entities of the same type | Put it in the entity (with a collection parameter) |
| Behavior involves multiple different entities | Domain Service |
| An operation on one entity produces a different entity | Entity method returning the new entity |
| Pure calculation with domain concepts | Domain Service |

### Key Characteristics

1. **Stateless**: No instance fields that represent domain state
2. **Domain logic**: Contains genuine business rules, not just delegation
3. **Multiple entities**: Operates on two or more domain objects
4. **Named after the operation**: Often a verb or verb phrase (e.g., `PricingService`, `TransferService`)

## Core Idea

```java
// Cross-entity business rules
public class PricingService {

    // Calculate order total, considering discount rules
    public Money calculateOrderPrice(List<OrderLine> lines, Customer customer, Promotion promotion) {
        Money subtotal = lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(new Money(BigDecimal.ZERO, Currency.USD), Money::add);

        Money discount = calculateDiscount(subtotal, customer, promotion);
        return subtotal.add(discount.negate());
    }

    private Money calculateDiscount(Money subtotal, Customer customer, Promotion promotion) {
        Money discount = Money.ZERO;

        // VIP customer discount
        if (customer.isVip()) {
            discount = discount.add(subtotal.multiply(0.1));
        }

        // Promotion discount
        if (promotion != null && promotion.isActive()) {
            discount = discount.add(promotion.applyTo(subtotal));
        }

        // Cannot exceed order amount
        return discount.isGreaterThan(subtotal) ? subtotal : discount;
    }
}
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: Business logic scattered across multiple services
public class BadOrderService {
    public void placeOrder(Order order) {
        // Scattered discount logic
        Money discount = Money.ZERO;
        if (order.getCustomer().isVip()) {
            discount = discount.add(order.getTotalAmount().multiply(0.1));
        }
        if (order.getPromotion() != null) {
            discount = discount.add(order.getPromotion().applyTo(order.getTotalAmount()));
        }
        // ...
    }
}

// ❌ BAD: Logic that should be in entity but is in service
public class BadOrderService {
    public void placeOrder(Order order) {
        // This logic belongs in Order entity!
        if (order.getLines().isEmpty()) {
            throw new OrderEmptyException();
        }
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException();
        }
        order.setStatus(OrderStatus.PLACED); // Direct field modification!
    }
}

// ❌ BAD: Application service doing domain logic
// Application services should orchestrate, not contain domain rules
public class BadCreateOrderUseCase {
    public OrderResult execute(CreateOrderCommand command) {
        // Domain logic in application layer — violation of clean architecture
        if (command.items().isEmpty()) throw new OrderEmptyException();

        // ... creates order with business rules
    }
}
```

```java
// ✅ GOOD: Domain Service with cross-entity logic
public class TransferService {
    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public void transfer(AccountId fromId, AccountId toId, Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw new InvalidTransferAmountException(amount);
        }

        Account from = accountRepository.findById(fromId)
            .orElseThrow(() -> new AccountNotFoundException(fromId));
        Account to = accountRepository.findById(toId)
            .orElseThrow(() -> new AccountNotFoundException(toId));

        // Domain logic spanning two entities
        from.debit(amount);
        to.credit(amount);

        // Business rule: overdraft not allowed
        if (from.getBalance().isNegative()) {
            throw new InsufficientFundsException(fromId, amount);
        }

        accountRepository.save(from);
        accountRepository.save(to);

        eventPublisher.publish(new TransferCompletedEvent(fromId, toId, amount));
    }
}

// ✅ GOOD: Domain Service with value object creation
public class InventoryAllocationService {

    public List<Allocation> allocate(Order order, List<Warehouse> warehouses) {
        List<Allocation> allocations = new ArrayList<>();
        List<OrderLine> remainingLines = new ArrayList<>(order.getLines());

        for (Warehouse warehouse : warehouses) {
            if (remainingLines.isEmpty()) break;

            for (OrderLine line : new ArrayList<>(remainingLines)) {
                StockItem stock = warehouse.findStock(line.getProductId());
                if (stock != null && stock.getAvailableQuantity() >= line.getQuantity()) {
                    allocations.add(new Allocation(warehouse.getId(), line, stock));
                    remainingLines.remove(line);
                }
            }
        }

        if (!remainingLines.isEmpty()) {
            throw new InsufficientStockException(remainingLines);
        }

        return allocations;
    }
}
```

## Domain Service vs Application Service

| Aspect | Domain Service | Application Service |
|--------|---------------|---------------------|
| Purpose | Contains domain logic | Orchestrates use cases |
| Dependencies | Domain concepts, other Domain Services | Repositories, Domain Services, ports |
| State | Stateless | Stateless |
| Location | Domain layer | Application layer |
| Example | `PricingService`, `TransferService` | `CreateOrderUseCase`, `OrderWorkflowService` |

```java
// Domain Service: contains domain logic
public class PricingService {
    public Money calculatePrice(List<CartItem> items, Customer customer) {
        // Pure domain logic — discount rules, pricing calculations
    }
}

// Application Service: orchestrates
public class CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final PricingService pricingService;

    public OrderResult execute(CreateOrderCommand command) {
        // Orchestration: get data, call domain service, save result
        Order order = Order.create(command.customerId());
        for (OrderItemCommand item : command.items()) {
            order.addLine(item.productId(), item.quantity());
        }

        // Call domain service for pricing
        Money finalPrice = pricingService.calculatePrice(
            order.getLines(), findCustomer(command.customerId())
        );

        orderRepository.save(order);
        return OrderResult.from(order);
    }
}
```

## Common Pitfalls

- **Domain Service doing too much**: If a service has many unrelated methods, consider splitting or checking if logic belongs in entities
- **Leaking infrastructure into Domain Service**: Domain Services should use domain objects, not infrastructure (e.g., don't inject JPA repositories into a Domain Service)
- **Stateful Domain Services**: If a service holds mutable state between method calls, it's no longer a Domain Service
- **Application logic in Domain Service**: Domain Services contain domain logic. Use Application Services for orchestration.

## When NOT to Use Domain Service

- When the logic can reasonably belong to a single entity — put it there
- For simple data transformation or aggregation — a method or query handler can do this
- When the logic is use-case specific and doesn't represent domain concepts — Application Service

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/service/` — Domain services with pure domain logic.

## Related References

- [Entity Pattern](./entity-pattern.md) — Entities that host their own behavior
- [Aggregate Pattern](./aggregate-pattern.md) — Aggregates managing internal consistency
- [Value Object Pattern](./value-object-pattern.md) — Immutable domain concepts
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Domain Service placement in layers
- [Software Architecture](../SKILL.md)
