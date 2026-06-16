# SOLID Principles

Fundamental object-oriented design principles for creating maintainable and flexible software.

## When to Use

Consult this file when designing classes, modules, or packages; reviewing code for design quality; or refactoring towards better architecture.

## Core Idea

The SOLID principles are five design principles that help developers create maintainable, flexible, and understandable software systems.

### Single Responsibility Principle (SRP)

A class should have only one reason to change. Every module or class should have responsibility over a single part of the functionality provided by the software.

### Open/Closed Principle (OCP)

Software entities should be open for extension but closed for modification. You should be able to add new functionality without changing existing code.

### Liskov Substitution Principle (LSP)

Objects of a superclass should be replaceable with objects of a subclass without affecting correctness. Subclasses must honor the contract of their parent class.

### Interface Segregation Principle (ISP)

Clients should not be forced to depend on interfaces they do not use. Prefer small, focused interfaces over large, general-purpose ones.

### Dependency Inversion Principle (DIP)

High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details; details should depend on abstractions.

## Detailed Principles

### Single Responsibility

| Violation Symptom | What It Looks Like |
|-------------------|--------------------|
| A class does too many things | A `UserManager` that validates, saves, and sends emails |
| Multiple reasons to change | Changing persistence affects reporting logic |

### Open/Closed

| Violation Symptom | What It Looks Like |
|-------------------|--------------------|
| Modifying existing code to add features | Adding a new payment type requires changing the payment service |
| Frequent changes to core classes | Every new requirement touches the same 3 classes |

### Liskov Substitution

| Violation Symptom | What It Looks Like |
|-------------------|--------------------|
| instanceof checks | `if (obj instanceof Square s)` |
| Type casting | `(Rectangle) obj` |
| Throwing exceptions in overridden methods | `@Override void resize() { throw new UnsupportedOperationException(); }` |

### Interface Segregation

| Violation Symptom | What It Looks Like |
|-------------------|--------------------|
| Fat interfaces | An `Animal` interface with `fly()`, `swim()`, `run()` |
| Forced implementation | A `Machine` that must implement `print()` but has nothing to print |

### Dependency Inversion

| Violation Symptom | What It Looks Like |
|-------------------|--------------------|
| Direct dependency on concrete classes | `new SqlRepository()` inside a service |
| Hard-coded database connections | `DriverManager.getConnection(url, user, pass)` in business logic |

## Bad/Good Examples (Java)

```java
// BAD: Single Responsibility Principle violation
// This class has multiple reasons to change (persistence, validation, email)
public class UserManager {
    public void save(User user) { /* SQL insert */ }
    public boolean validateEmail(String email) { /* regex */ }
    public void sendWelcomeEmail(User user) { /* SMTP */ }
}

// GOOD: Each class has one responsibility
public class UserRepository {
    public void save(User user) { /* persistence */ }
}

public class EmailValidator {
    public boolean isValid(String email) { /* validation */ }
}

public class NotificationService {
    public void sendWelcome(User user) { /* email */ }
}
```

```java
// BAD: Liskov Substitution Principle violation
// Square IS-A Rectangle mathematically, but cannot substitute it
public class Rectangle {
    protected int width;
    protected int height;
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        this.width = w;
        this.height = w; // Violates LSP: setters don't behave consistently
    }
    @Override
    public void setHeight(int h) {
        this.width = h;
        this.height = h;
    }
}

@Test
void testRectangleArea(Rectangle r) {
    r.setWidth(5);
    r.setHeight(4);
    assertThat(r.area()).isEqualTo(20); // Fails for Square!
}
```

```java
// GOOD: Separate abstractions for different behaviors
public interface Resizable {
    void resize(int width, int height);
}

public interface UniformResizable {
    void resize(int size);
}

public class Rectangle implements Resizable {
    public void resize(int w, int h) { ... }
}

public class Square implements UniformResizable {
    public void resize(int size) { ... }
}
```

```java
// BAD: Dependency Inversion violation
// High-level module depends on low-level module
public class OrderService {
    private final JpaOrderRepository repository = new JpaOrderRepository(); // Direct dependency!

    public void place(Order order) {
        repository.save(order);
    }
}

// GOOD: Depend on abstraction
public class OrderService {
    private final OrderRepository repository; // Interface, not implementation

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public void place(Order order) {
        repository.save(order);
    }
}
```

## Common Pitfalls

- **Confusing SRP with "one method per class"**: SRP is about reasons to change, not method count. A class with 5 methods that all change for the same reason still follows SRP.
- **Over-engineering with interfaces**: Don't create an interface for every class. Create interfaces when you have actual or planned multiple implementations.
- **Violating OCP with "easy" modifications**: Adding a simple if-statement seems harmless but accumulates technical debt.
- **ISP confusion**: If a refactor changes an interface, existing implementations break. Use ISP to prevent this.

## Signs of Architecture Decay

- **Circular dependencies**: Module A → B → C → A
- **Shotgun surgery**: Changing one feature requires modifying multiple classes
- **Feature envy**: A class spends more time accessing other class's data than its own
- **Duplicated code**: Repeated code scattered across the codebase
- **Premature abstraction**: Violating YAGNI, adding unnecessary indirection

## Real Implementation Reference

`apps/server/src/main/java/com/ai/domain/` — Domain entities follow SRP and LSP. Repository interfaces in domain layer follow DIP.

## Related References

- [Clean Architecture Deep Dive](./clean-architecture-deep-dive.md) — Layer model and dependency rules that enforce SOLID
- [Entity Pattern](./entity-pattern.md) — Applying SOLID to domain entity design
- [Repository Pattern](./repository-pattern.md) — DIP applied to data access
- [Software Architecture](../SKILL.md)
- [Software Development](../../software-development/SKILL.md)
