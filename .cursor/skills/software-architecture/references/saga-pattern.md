# Saga Pattern

A pattern for managing distributed transactions across multiple services using a sequence of local transactions with compensating actions for rollback.

## When to Use

Consult this file when you need to maintain data consistency across multiple services or databases without distributed transactions (2PC), when services are independent and communicate via events, or when building workflows that span multiple bounded contexts.

## Core Idea

Traditional ACID transactions don't work across services. Saga replaces distributed transactions with a series of local transactions. If a step fails, Saga executes compensating transactions to undo the completed steps.

### Saga vs Two-Phase Commit

| Aspect | Saga | Two-Phase Commit (2PC) |
|--------|------|----------------------|
| Transaction scope | Distributed | Distributed |
| Blocking | Non-blocking | Blocking (during commit phase) |
| Coordinator failure | Saga state preserved | In-doubt transactions |
| Performance | High | Low (coordination overhead) |
| Consistency | Eventual | Strong |
| Use case | Microservices, polyglot persistence | Single database |

### Saga Types

1. **Choreography**: Each service publishes events; other services react. Decentralized.
2. **Orchestration**: A central orchestrator coordinates the saga steps. More control, but centralization.

## Core Idea

```java
// Orchestration-based Saga
public class OrderSagaOrchestrator {
    private final List<SagaStep> steps = List.of(
        new ReserveInventoryStep(),
        new ProcessPaymentStep(),
        new ConfirmShipmentStep()
    );

    public void execute(OrderCreatedEvent event) {
        SagaContext context = new SagaContext(event.orderId());

        for (SagaStep step : steps) {
            try {
                step.execute(context);
            } catch (Exception e) {
                // Compensating transactions
                for (int i = steps.indexOf(step) - 1; i >= 0; i--) {
                    steps.get(i).compensate(context);
                }
                break;
            }
        }
    }
}

// Compensating transaction example
public class ReserveInventoryStep {
    private final InventoryService inventoryService;

    public void execute(SagaContext context) {
        InventoryReservation reservation = inventoryService.reserve(
            context.orderId(),
            context.items()
        );
        context.setInventoryReservation(reservation);
    }

    public void compensate(SagaContext context) {
        if (context.hasInventoryReservation()) {
            inventoryService.release(context.getInventoryReservation());
        }
    }
}
```

## Choreography-Based Saga

```java
// Event-driven choreography
public class OrderService {
    private final EventPublisher eventPublisher;

    public void handle(CreateOrderCommand command) {
        Order order = Order.create(command.customerId(), command.items());
        orderRepository.save(order);

        // Publish event to start the saga
        eventPublisher.publish(new OrderCreatedEvent(order.getId(), order.getItems()));
    }
}

public class InventoryService {
    @EventListener
    public void handle(OrderCreatedEvent event) {
        try {
            Reservation reservation = inventoryService.reserve(event.orderId(), event.items());
            eventPublisher.publish(new InventoryReservedEvent(event.orderId(), reservation));
        } catch (InsufficientStockException e) {
            eventPublisher.publish(new OrderFailedEvent(event.orderId(), "INSUFFICIENT_STOCK"));
        }
    }

    @EventListener
    public void handle(OrderCancelledEvent event) {
        // Compensate: release reservation
        inventoryService.release(event.orderId());
    }
}

public class PaymentService {
    @EventListener
    public void handle(InventoryReservedEvent event) {
        try {
            PaymentResult result = paymentService.charge(event.orderId(), event.amount());
            eventPublisher.publish(new PaymentProcessedEvent(event.orderId(), result));
        } catch (PaymentFailedException e) {
            eventPublisher.publish(new PaymentFailedEvent(event.orderId(), e.getMessage()));
        }
    }

    @EventListener
    public void handle(OrderCancelledEvent event) {
        // Compensate: refund if already charged
        paymentService.refund(event.orderId());
    }
}
```

## Saga Orchestrator

```java
// Saga orchestrator definition
public class PlaceOrderSaga {
    private final SagaOrchestrator orchestrator;

    public PlaceOrderSaga(
            InventoryService inventoryService,
            PaymentService paymentService,
            ShippingService shippingService
    ) {
        this.orchestrator = SagaOrchestrator.builder()
            .step(ReserveInventoryStep.class, ctx -> inventoryService.reserve(ctx.orderId(), ctx.items()))
            .compensate(ReserveInventoryStep.class, ctx -> inventoryService.release(ctx.reservation()))
            .step(ChargePaymentStep.class, ctx -> paymentService.charge(ctx.orderId(), ctx.amount()))
            .compensate(ChargePaymentStep.class, ctx -> paymentService.refund(ctx.orderId()))
            .step(ScheduleShippingStep.class, ctx -> shippingService.schedule(ctx.orderId()))
            .compensate(ScheduleShippingStep.class, ctx -> shippingService.cancel(ctx.shipmentId()))
            .build();
    }

    public SagaResult execute(PlaceOrderSagaCommand command) {
        return orchestrator.execute(command);
    }
}

// Saga context carries data between steps
public record SagaContext(
    OrderId orderId,
    List<OrderItem> items,
    Reservation reservation,
    PaymentResult paymentResult,
    ShipmentId shipmentId
) {}
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: No saga — direct cross-service calls with no compensation
public class BadOrderService {
    public void placeOrder(Order order) {
        // No compensation if this fails
        inventoryService.reserve(order.getItems());
        paymentService.charge(order.getTotal());
        shippingService.schedule(order.getId());
        orderRepository.save(order);
        // If shippingService fails here, inventory and payment are already done!
    }
}

// ❌ BAD: Saga without compensation logic
public class BadPlaceOrderSaga {
    public void execute(Order order) {
        inventoryService.reserve(order.getItems()); // No compensation on failure
        paymentService.charge(order.getTotal());    // No compensation
        // ...
    }
}

// ❌ BAD: Synchronous saga in distributed system
// If orchestrator crashes mid-execution, saga state is lost
```

```java
// ✅ GOOD: Saga with proper compensation
public class OrderSaga {
    private final SagaStateStore stateStore;

    public void handleOrderCreated(OrderCreatedEvent event) {
        // Persist saga state before starting
        SagaState state = new SagaState(event.orderId(), SagaStatus.IN_PROGRESS);
        stateStore.save(state);

        // Start saga with compensation-aware steps
        try {
            inventoryService.reserve(event.items());
            state.addCompletedStep("RESERVE_INVENTORY");
            stateStore.save(state);

            paymentService.charge(event.amount());
            state.addCompletedStep("PROCESS_PAYMENT");
            stateStore.save(state);

            shippingService.schedule(event.orderId());
            state.addCompletedStep("SCHEDULE_SHIPPING");
            state.setStatus(SagaStatus.COMPLETED);
            stateStore.save(state);
        } catch (Exception e) {
            compensate(state);
        }
    }

    private void compensate(SagaState state) {
        for (String step : state.getCompletedSteps().reversed()) {
            switch (step) {
                case "SCHEDULE_SHIPPING" -> shippingService.cancel(state.orderId());
                case "PROCESS_PAYMENT" -> paymentService.refund(state.orderId());
                case "RESERVE_INVENTORY" -> inventoryService.release(state.orderId());
            }
        }
        state.setStatus(SagaStatus.COMPENSATED);
        stateStore.save(state);
    }
}
```

## Saga State Persistence

```java
// Saga state store for recovery
public interface SagaStateStore {
    void save(SagaState state);
    Optional<SagaState> findById(String sagaId);
    List<SagaState> findPendingSagas();
}

// Recovery from failure
@Scheduled(fixedDelay = 60000)
public void recoverSagas() {
    List<SagaState> pending = sagaStateStore.findPendingSagas();
    for (SagaState state : pending) {
        // Resume or compensate incomplete sagas
        if (state.isTimedOut()) {
            compensate(state);
        } else {
            resume(state);
        }
    }
}
```

## Common Pitfalls

- **Missing compensation logic**: Every step must have a corresponding compensation
- **Non-idempotent operations**: Sagas may retry steps, so operations must be idempotent or use idempotency keys
- **No saga state persistence**: If the orchestrator crashes, the saga can't recover
- **Circular dependencies in choreography**: A publishes B, B publishes A — infinite loops
- **Order of compensation**: Must compensate in reverse order of execution
- **Saga vs distributed transaction**: Saga provides eventual consistency, not strong consistency

## When NOT to Use Saga

- When strong consistency is required within a single request
- For simple two-phase operations (use a simpler pattern)
- When compensation logic is complex or expensive (e.g., can't undo a sent email)
- For short-duration operations where distributed transactions are acceptable

## Saga with Event Sourcing

Sagas pair well with event sourcing. The event log provides an audit trail of the saga execution:

```
OrderCreated → InventoryReserved → PaymentCharged → ShippingScheduled
                                          ↓
                               PaymentRefunded ← InventoryReleased
                                   (compensation on failure)
```

## Real Implementation Reference

`apps/server/src/main/java/com/ai/application/saga/` — Saga orchestrator implementations.

## Related References

- [Event-Driven Architecture](./event-driven-architecture.md) — System-level event patterns
- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Events as source of truth
- [CQRS Pattern](./cqrs-pattern.md) — Read/write separation in saga
- [Microservices Patterns](./microservices-patterns.md) — Saga for distributed systems
- [Domain Event Pattern](./domain-event-pattern.md) — Events used in saga choreography
- [Software Architecture](../SKILL.md)
