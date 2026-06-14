# Event-Driven Architecture

An architectural style where system components communicate by producing and consuming events, enabling loose coupling, scalability, and temporal decoupling between services.

## When to Use

Consult this file when designing systems where multiple independent components need to react to the same business occurrences, when you need to decouple producers from consumers, when audit trails or historical replay are required, or when building microservices that need to maintain data consistency without distributed transactions.

## Core Idea

Event-Driven Architecture (EDA) centers on the concept of **events** — records of something that happened in the domain. Instead of a service calling another service directly (synchronous coupling), a service publishes an event that other services can consume asynchronously.

### Key Patterns

See [Patterns](./patterns.md) for detailed code examples:

| Pattern | Description |
|---------|-------------|
| **Event Sourcing** | Store events instead of state, reconstruct from event replay |
| **CQRS** | Separate Command (write) and Query (read) models |
| **Saga** | Distributed transaction management via compensating transactions |

## Why Event-Driven?

### Decoupling

```
Synchronous coupling:           Event-driven decoupling:
┌─────────┐    calls    ┌─────────┐   ┌─────────┐ publishes ┌─────────┐
│   A     │ ──────────► │    B     │   │   A     │ ─────────► │ Broker  │
└─────────┘             └─────────┘   └─────────┘           └────┬────┘
      │                      │                                       │
      │ waits for response   │                              ┌────────┴────────┐
      │                      │                           ┌──▼───┐          ┌───▼───┐
      ▼                      ▼                           │   B  │          │   C   │
   (blocking)           (blocking)                       └───┬───┘          └───┬───┘
                                                         (consumes)        (consumes)
```

- **Producer** publishes events and continues without waiting
- **Consumer** processes events independently, at its own pace
- **Temporal decoupling**: Producer and consumer don't need to be online simultaneously

### Benefits

1. **Scalability**: Each consumer processes events at its own rate, independent of producers
2. **Resilience**: If a consumer is down, events queue up and are processed when it recovers
3. **Audit trail**: Events are the source of truth — you can reconstruct any past state
4. **Extensibility**: Add new consumers without modifying producers
5. **Audit and compliance**: Event log serves as an immutable audit trail

## Event Types

### Domain Events

Events that represent meaningful business occurrences within a bounded context.

```java
public record OrderPlacedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    Instant occurredAt
) {}
```

Published by domain entities (aggregates) when state changes occur.

### Integration Events

Events that cross bounded context or system boundaries. Usually contain a simplified payload with IDs rather than full domain objects.

```java
public record OrderIntegrationEvent(
    String eventId,
    String eventType,
    String sourceSystem,
    OrderPayload payload,
    Instant timestamp
) {}
```

### Commands vs Events

| Aspect | Command | Event |
|--------|---------|-------|
| Intent | Request to do something | Report that something happened |
| Sender expectation | Expects a response | No expectation of response |
| Coupling | Tighter (knows the handler) | Looser (doesn't know consumers) |
| Naming | Imperative ("PlaceOrder") | Past tense ("OrderPlaced") |

## Event Processing Patterns

### Point-to-Point

One producer, one consumer. Simple but provides no fan-out.

### Publish/Subscribe

One producer, multiple consumers. Each consumer receives its own copy of the event.

### Event Streaming

Events are stored in a log (Kafka, EventStore). Consumers can read from any offset, replay events, or start from scratch.

## Technology Choices

| Tool | Best For | Characteristics |
|------|----------|----------------|
| **Apache Kafka** | High-throughput streaming, event sourcing | Durable, ordered per partition, replayable |
| **RabbitMQ** | Task queues, flexible routing | Flexible exchanges, TTL, dead-letter queues |
| **EventStoreDB** | Event sourcing | Native event store, projections |
| **AWS EventBridge** | Serverless event routing | Cloud-native, schema registry |

## Common Pitfalls

- **Eventual consistency confusion**: Event-driven systems are eventually consistent. If your business requires strong consistency, EDA may not be suitable.
- **Event schema evolution**: Changing event schemas breaks consumers. Use versioning and schema registries.
- **Duplicate event processing**: Consumers must be idempotent since events can be delivered more than once.
- **Silent failures**: If a consumer fails to process an event, it may silently drop events. Use dead-letter queues and monitoring.
- **Circular event chains**: A publishes B, B publishes C, C publishes A — leads to infinite loops.

## When NOT to Use

Event-Driven Architecture is not suitable when:

- **Strong consistency is required**: If a write must be immediately visible to subsequent reads within the same request, EDA's eventual consistency is problematic
- **Simple CRUD operations**: The overhead of events is not worth it for simple create/read/update/delete
- **Small team, simple domain**: EDA adds operational complexity that may not pay off
- **Low latency requirements**: The async nature of event processing introduces latency

## Related Patterns

These patterns are detailed in their own reference files:

| Pattern | File | Description |
|---------|------|-------------|
| Event Sourcing | [Event Sourcing Pattern](./event-sourcing-pattern.md) | Store state as a sequence of events |
| CQRS | [CQRS Pattern](./cqrs-pattern.md) | Separate read and write models |
| Saga | [Saga Pattern](./saga-pattern.md) | Manage distributed transactions via events |

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/event/` — Domain events defined as immutable records. Infrastructure layer handles event publishing via message brokers.

## Related References

- [Domain Event Pattern](./domain-event-pattern.md) — Tactical pattern for defining and publishing domain events
- [Event Sourcing Pattern](./event-sourcing-pattern.md) — Storing state as a sequence of events
- [CQRS Pattern](./cqrs-pattern.md) — Separate command and query models
- [Saga Pattern](./saga-pattern.md) — Distributed transaction management
- [Microservices Patterns](./microservices-patterns.md) — How EDA fits into microservices communication
- [Software Architecture](../SKILL.md)
