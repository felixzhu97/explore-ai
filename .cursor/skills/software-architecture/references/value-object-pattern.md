# Value Object Pattern

Immutable objects that are defined by their attributes rather than a unique identity. Two value objects with identical attributes are considered equal and interchangeable.

## When to Use

Consult this file when modeling quantities, measurements, dates, money, addresses, or any domain concept where identity doesn't matter and immutability is desirable.

## Core Idea

Value Objects describe things that are characterized by their attributes, not by a unique identity. They are immutable — created once, never modified. If you need to change a value object, you replace it with a new instance.

### Value Object vs Entity

| Aspect | Value Object | Entity |
|--------|-------------|--------|
| Identity | No identity (or derived from attributes) | Has stable, unique identity |
| Mutability | Immutable | Usually mutable |
| Lifespan | Short-lived, created and replaced | Long-lived, tracked over time |
| Equality | By attribute values | By identity (ID) |
| Examples | Money, Address, DateRange | Order, Customer, Product |

### When to Use Value Objects

- Modeling quantities with units (Money, Weight, Distance)
- Modeling descriptive characteristics (Address, Email, PhoneNumber)
- Modeling measurements or calculations (Percentage, Temperature)
- Modeling combinations of related attributes (DateRange, PostalAddress)

### Benefits

1. **Immutability**: Thread-safe, no defensive copies needed
2. **Self-validation**: Validate attributes at construction time
3. **Equality by value**: Two instances with same values are equal
4. **Replaceability**: Modify by creating new instance, old instance unchanged
5. **Expressive domain model**: Richer than primitives

## Core Idea

```java
// Immutable value object
public record Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        this.amount = amount.stripTrailingZeros();
        this.currency = Objects.requireNonNull(currency, "Currency is required");
    }

    // Value object operations return new instances
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    // equals/hashCode based on value
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    // No setters, all fields final
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }
}
```

### Java Record Implementation (Java 16+)

```java
// Preferred: Java record for value objects
public record Money(BigDecimal amount, Currency currency) {

    // Compact constructor for validation
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        amount = amount.stripTrailingZeros(); // Normalize
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value), Currency.USD);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }
}
```

## Bad/Good Examples (Java)

```java
// ❌ BAD: Primitive obsession — using primitives where domain concepts are needed
public class BadOrderLine {
    private UUID productId;
    private BigDecimal price;         // Just a number — what currency?
    private int quantity;             // Just an int — what unit?
    private BigDecimal total;         // Calculated but not enforced

    public void setPrice(BigDecimal price) {
        this.price = price; // No validation!
    }
}

// ❌ BAD: Mutable value object — breaks immutability guarantee
public class MutableMoney {
    private BigDecimal amount;
    private Currency currency;

    public void setAmount(BigDecimal amount) { this.amount = amount; } // Mutable!
    public void add(MutableMoney other) { this.amount = this.amount.add(other.amount); } // Mutates!
}
```

```java
// ✅ GOOD: Immutable value object with validation
public record EmailAddress(String value) {
    public EmailAddress {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        value = value.toLowerCase(); // Normalize
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
}

// ✅ GOOD: Value object used in entity
public class Customer {
    private CustomerId id;
    private EmailAddress email; // Richer than String
    private Money creditLimit;

    public void updateEmail(EmailAddress newEmail) {
        // Replace, don't mutate
        this.email = newEmail;
    }
}

// ✅ GOOD: Value objects composing each other
public record PostalAddress(
    String street,
    String city,
    String state,
    String postalCode,
    Country country
) {
    public PostalAddress {
        Objects.requireNonNull(street, "Street is required");
        Objects.requireNonNull(city, "City is required");
        Objects.requireNonNull(postalCode, "Postal code is required");
        Objects.requireNonNull(country, "Country is required");
    }
}

// ✅ GOOD: Value object with operations
public record Percentage(decimal value) {
    public Percentage {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
    }

    public static Percentage of(int value) {
        return new Percentage(value);
    }

    public decimal applyTo(Money amount) {
        return amount.multiply(this.value).divide(100);
    }

    public Percentage add(Percentage other) {
        return new Percentage(this.value + other.value);
    }
}
```

## Common Pitfalls

- **Mutable value objects**: Adding setters or mutating methods defeats the purpose. Use records or make all fields final.
- **Primitive obsession**: Using `String` for email, `BigDecimal` for money, `int` for quantity — lose type safety and validation.
- **Identity confusion**: Passing a value object between layers may require conversion (e.g., to a String for serialization). Don't confuse the representation with the concept.
- **Over-engineering**: Not every string field needs a value object. Use judgment — value objects are most valuable for:
  - Domain concepts that appear in multiple places
  - Concepts with validation rules
  - Concepts that have operations

## When NOT to Use Value Objects

- For simple strings that won't be reused (e.g., a user's display name)
- When identity matters (e.g., two users with the same name are still different users)
- When the object needs to be tracked in a database as a separate entity
- For transient objects in DTOs or application layer only

## Value Object in Entity Context

```java
public class Order {
    private OrderId id;
    private CustomerId customerId;
    private Money totalAmount; // Value object

    public void addLine(Product product, int quantity) {
        // ...
    }

    public Money calculateTotal() {
        return lines.stream()
            .map(line -> line.getPrice().multiply(quantity))
            .reduce(Money.ZERO, Money::add);
    }

    // Expose as value object, not primitive
    public Money getTotalAmount() {
        return totalAmount;
    }
}
```

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/model/` — Value objects alongside entities. `Money`, `EmailAddress`, `PostalAddress`.

## Related References

- [Entity Pattern](./entity-pattern.md) — Entities contrasted with value objects
- [Aggregate Pattern](./aggregate-pattern.md) — Aggregates contain and manage value objects
- [Rich vs Anemic Model](./rich-vs-anemic-model.md) — Value objects support a rich domain model
- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Value objects in the domain layer
- [Software Architecture](../SKILL.md)
