# Event Sourcing Pattern

An architectural pattern that stores the complete history of domain state changes as a sequence of immutable events, rather than storing the current state directly.

## When to Use

Consult this file when you need a complete audit trail of all changes, when state reconstruction from history is valuable, when you need temporal queries ("what was the state at time T?"), or when building event-driven systems where events are the source of truth.

## Core Idea

Instead of storing the current state of an aggregate, event sourcing stores all events that have ever occurred. The current state is reconstructed by replaying all events. This creates an immutable, append-only log of everything that has happened.

### Traditional vs Event Sourcing

```
Traditional:
┌─────────────┐
│   Current   │  ← State is stored directly
│   State     │
│  (Order #1) │
└─────────────┘

Event Sourcing:
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ OrderCreated │→│LineAdded    │→│LineAdded    │→│ OrderPlaced │
│   Event     │ │   Event     │ │   Event     │ │   Event     │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
     ↑                                               │
     └───────────────────────────────────────────────┘
                    Replay to reconstruct state
```

### Benefits

1. **Complete audit trail**: Every change is recorded as an immutable event
2. **Temporal queries**: Query the state at any point in time
3. **Event replay**: Rebuild state by replaying events
4. **Eventual consistency**: Multiple read models can be built from the same event stream
5. **Debugging**: Event log provides exact sequence of what happened

## Core Idea

```java
// Event store instead of state store
public class BankAccount {
    private AccountId id;
    private Money balance = Money.zero(Currency.USD);
    private List<DomainEvent> events = new ArrayList<>();

    // Reconstruct state from event replay
    public void replay(Iterable<DomainEvent> events) {
        events.forEach(this::mutate);
    }

    private void mutate(DomainEvent event) {
        switch (event) {
            case DepositedEvent e -> apply(e);
            case WithdrawnEvent e -> apply(e);
        }
    }

    private void apply(DepositedEvent e) {
        this.balance = this.balance.add(e.amount());
    }

    private void apply(WithdrawnEvent e) {
        this.balance = this.balance.subtract(e.amount());
    }

    // Commands produce events
    public void deposit(Money amount) {
        if (amount.isNegative()) throw new InvalidAmountException();
        events.add(new DepositedEvent(id, amount, Instant.now()));
        mutate(events.get(events.size() - 1));
    }

    public void withdraw(Money amount) {
        if (amount.isNegative()) throw new InvalidAmountException();
        if (balance.isLessThan(amount)) throw new InsufficientFundsException();
        events.add(new WithdrawnEvent(id, amount, Instant.now()));
        mutate(events.get(events.size() - 1));
    }

    // For event sourcing: expose the event stream
    public List<DomainEvent> getEvents() {
        return List.copyOf(events);
    }

    public void clearEvents() {
        events.clear();
    }
}
```

## Event Definitions

```java
// Domain events
public record DepositedEvent(
    AccountId accountId,
    Money amount,
    Instant occurredAt
) implements DomainEvent {}

public record WithdrawnEvent(
    AccountId accountId,
    Money amount,
    Instant occurredAt
) implements DomainEvent {}

public record AccountCreatedEvent(
    AccountId accountId,
    AccountOwner owner,
    Money initialBalance,
    Instant occurredAt
) implements DomainEvent {}
```

## Event Store

```java
// Event store interface
public interface EventStore {
    void append(String aggregateId, DomainEvent event);
    void append(String aggregateId, List<DomainEvent> events);
    List<DomainEvent> getEvents(String aggregateId);
    List<DomainEvent> getEvents(String aggregateId, Instant from, Instant to);
}

// Event store implementation
public class InMemoryEventStore implements EventStore {
    private final Map<String, List<DomainEvent>> store = new ConcurrentHashMap<>();

    @Override
    public void append(String aggregateId, DomainEvent event) {
        store.computeIfAbsent(aggregateId, k -> new ArrayList<>()).add(event);
    }

    @Override
    public void append(String aggregateId, List<DomainEvent> events) {
        store.computeIfAbsent(aggregateId, k -> new ArrayList<>()).addAll(events);
    }

    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return List.copyOf(store.getOrDefault(aggregateId, List.of()));
    }
}
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: Mixing state storage with event sourcing
public class BadBankAccount {
    private Money balance;
    private List<Transaction> transactions; // Mixing state and events!

    public void deposit(Money amount) {
        this.balance = this.balance.add(amount);
        transactions.add(new Transaction(amount, TransactionType.DEPOSIT)); // Not immutable!
    }
}

// ❌ BAD: Events with side effects during command
// Commands should add events, not directly modify state AND call services
public class BadBankAccount {
    public void deposit(Money amount, NotificationService notificationService) {
        // Side effect in command — violates event sourcing principle
        notificationService.notify(amount);
        this.balance = this.balance.add(amount);
    }
}
```

```java
// ✅ GOOD: Pure event sourcing — commands only add events
public class GoodBankAccount {
    private AccountId id;
    private Money balance = Money.zero(Currency.USD);
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public void deposit(Money amount) {
        if (amount.isNegative()) {
            throw new InvalidAmountException("Deposit amount cannot be negative");
        }
        // Only add event — state is derived
        addEvent(new DepositedEvent(id, amount, Instant.now()));
    }

    private void addEvent(DomainEvent event) {
        pendingEvents.add(event);
        apply(event);
    }

    private void apply(DomainEvent event) {
        switch (event) {
            case DepositedEvent e -> this.balance = this.balance.add(e.amount());
            case WithdrawnEvent e -> this.balance = this.balance.subtract(e.amount());
        }
    }

    // For snapshotting optimization
    public void snapshot(Money currentBalance) {
        addEvent(new AccountSnapshotEvent(id, currentBalance, Instant.now()));
    }
}
```

## Snapshots

For aggregates with many events, snapshots optimize reconstruction time:

```java
public interface SnapshotStore {
    Optional<AggregateSnapshot> getSnapshot(String aggregateId);
    void saveSnapshot(AggregateSnapshot snapshot);
}

public class AccountSnapshot {
    private final String aggregateId;
    private final Money balance;
    private final int lastEventIndex;
}

// Reconstruction with snapshot
public class SnapshottingAccountRepository {
    public BankAccount findById(AccountId id) {
        Optional<Snapshot> snapshot = snapshotStore.getSnapshot(id.value());
        List<DomainEvent> events;

        if (snapshot.isPresent()) {
            events = eventStore.getEventsAfter(id.value(), snapshot.get().lastEventIndex());
        } else {
            events = eventStore.getEvents(id.value());
        }

        BankAccount account = new BankAccount(id);
        if (snapshot.isPresent()) {
            account.restore(snapshot.get());
        }
        account.replay(events);
        return account;
    }
}
```

## Common Pitfalls

- **Large event stores**: Aggregates with many events become slow to reconstruct. Use snapshots.
- **Event schema evolution**: Changing event structure breaks backward compatibility. Use versioning and upcasting.
- **Projections getting out of sync**: Read models built from events can fall behind. Monitor lag.
- **Performance**: High-throughput systems may struggle with event store write throughput.
- **Upcasting**: Old events may need transformation to match new schemas.

## Event Upcasting

```java
// Old event without amount field
public record OldDepositedEvent(String accountId, Instant occurredAt) {}

// Upcaster transforms old events to new format
public class DepositedEventUpcaster implements EventUpcaster {
    @Override
    public boolean canUpcast(DomainEvent event) {
        return event instanceof OldDepositedEvent;
    }

    @Override
    public DomainEvent upcast(DomainEvent event) {
        OldDepositedEvent old = (OldDepositedEvent) event;
        // Old events had amount in metadata — reconstruct
        return new DepositedEvent(
            AccountId.of(old.accountId()),
            Money.of(0), // Default, will be overridden by subsequent event
            old.occurredAt()
        );
    }
}
```

## When NOT to Use Event Sourcing

- Simple CRUD applications without audit requirements
- When eventual consistency is unacceptable
- When storage costs are a concern (every change creates an event)
- When team is new to event sourcing and project is under time pressure

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/event/` — Domain event definitions for event sourcing.

## Related References

- [Domain Event Pattern](./domain-event-pattern.md) — Basic domain events
- [CQRS Pattern](./cqrs-pattern.md) — Read models built from event streams
- [Event-Driven Architecture](./event-driven-architecture.md) — System-level event patterns
- [Saga Pattern](./saga-pattern.md) — Sagas with event sourcing
- [Software Architecture](../SKILL.md)
