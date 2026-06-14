# Clean Code: Formatting and Structure

Code formatting is not aesthetic preference; it is communication. Consistent formatting reduces cognitive load by eliminating superficial variation, allowing readers to focus on substance. Structural conventions signal intent and make code navigable by establishing predictable patterns.

## Why It Matters

A codebase that looks consistent appears well-maintained, even if it is not. More importantly, consistent formatting eliminates debates during code review and reduces the cognitive overhead of reading code written by multiple authors. Formatting decisions should be made once, automated by tooling, and enforced consistently.

Structural conventions like class organization and package hierarchy serve a dual purpose: they make code predictable to navigate and they reinforce architectural boundaries. When the physical structure of code mirrors its logical structure, the codebase becomes easier to understand and change.

## Smell Catalog

### Vertical Formatting (Newspaper Metaphor)

Code should be organized like a newspaper article: the most important content at the top, with details expanding as you read down. The name of the file or class should be at the top, followed by the most important concepts, followed by details and supporting functions.

#### Bad (Java)

```java
package com.example.order;

class OrderProcessor {
    private BigDecimal shippingCost;
    private static final int MAX_ITEMS = 100;
    private OrderRepository orderRepository;
    private NotificationService notificationService;
    private BigDecimal calculateTotal(Order order) { }
    public void shipOrder(Order order) { }
    public void processOrder(Order order) {
        validateOrder(order);
        BigDecimal total = calculateTotal(order);
        saveOrder(order);
        sendNotification(order);
    }
    private void sendNotification(Order order) { }
    private void saveOrder(Order order) { }
    private void validateOrder(Order order) { }
    private boolean canBeProcessed(Order order) { }
}
```

The reader encounters private helper methods before understanding what the class does.

#### Good (Java)

```java
package com.example.order;

/**
 * Processes customer orders from placement through shipment.
 *
 * <p>This class orchestrates the order fulfillment workflow including
 * validation, total calculation, persistence, and customer notification.</p>
 */
public class OrderProcessor {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    // Package-private constants
    static final int MAX_ORDER_ITEMS = 100;

    // Constructor
    public OrderProcessor(OrderRepository orderRepository,
                         NotificationService notificationService) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    // Public API
    public void processOrder(Order order) {
        validateOrder(order);
        BigDecimal total = calculateTotal(order);
        persistOrder(order, total);
        notifyCustomer(order);
    }

    public void shipOrder(Order order) {
        if (!order.isReadyForShipment()) {
            throw new OrderNotShippableException(order.getId());
        }
        logisticsService.scheduleShipment(order);
    }

    // Private helpers
    private void validateOrder(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        if (order.getLines().isEmpty()) {
            throw new EmptyOrderException();
        }
    }
}
```

The public API comes first. Supporting details follow. The reader understands the class contract immediately.

#### Bad (TypeScript)

```typescript
const MAX_ITEMS = 100;

function notifyCustomer(order: Order): void {
  emailService.send(order.customerEmail, orderConfirmation(order));
}

function calculateTotal(order: Order): Money {
  return order.lines.reduce((sum, line) => sum.add(line.subtotal()), Money.ZERO);
}

class OrderProcessor {
  private shippingCost: Money;
  processOrder(order: Order): void {
    this.validate(order);
    const total = calculateTotal(order);
    this.save(order);
    notifyCustomer(order);
  }
}
```

Helper functions are interspersed with class methods without clear organization.

#### Good (TypeScript)

```typescript
const MAX_ORDER_ITEMS = 100;

type OrderProcessorDeps = {
  orderRepository: OrderRepository;
  emailService: EmailService;
};

class OrderProcessor {
  private readonly orderRepository: OrderRepository;
  private readonly emailService: EmailService;

  constructor(deps: OrderProcessorDeps) {
    this.orderRepository = deps.orderRepository;
    this.emailService = deps.emailService;
  }

  async processOrder(order: Order): Promise<void> {
    this.validateOrder(order);
    const total = this.calculateTotal(order);
    await this.persistOrder(order, total);
    await this.notifyCustomer(order);
  }

  private validateOrder(order: Order): void {
    if (order.lines.length === 0) {
      throw new EmptyOrderError();
    }
  }

  private calculateTotal(order: Order): Money {
    return order.lines.reduce((sum, line) => sum.add(line.subtotal()), Money.ZERO);
  }
}
```

The class has clear sections: constructor, public methods, private methods. Dependencies are explicit.

### Horizontal Formatting (Line Length)

Line length affects readability. The traditional 80-character limit dates to punch cards but remains a useful guideline. Modern screens can display more, but excessively long lines force horizontal scrolling and suggest that the code is doing too much. Break long lines at natural boundaries.

#### Bad (Java)

```java
public List<Order> findOrdersByCustomerAndStatus(UUID customerId, OrderStatus status, LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
    return orderRepository.findByCustomerIdAndStatusAndCreatedAtBetween(customerId, status, fromDate, toDate, PageRequest.of(page, pageSize));
}
```

A single line of 180 characters requires horizontal scrolling and is difficult to read.

#### Good (Java)

```java
public List<Order> findOrdersByCustomerAndStatus(
        UUID customerId,
        OrderStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int pageSize
) {
    Pageable pageable = PageRequest.of(page, pageSize);
    return orderRepository.findByCustomerIdAndStatusAndCreatedAtBetween(
        customerId, status, fromDate, toDate, pageable
    );
}
```

Each parameter is on its own line. The method call is also broken into multiple lines.

#### Bad (TypeScript)

```typescript
const result = someFunction(argument1, argument2, argument3, argument4, argument5, argument6, argument7, argument8);
```

Long lines force horizontal scrolling and reduce readability.

#### Good (TypeScript)

```typescript
const result = await someFunction(
  argument1,
  argument2,
  argument3,
  argument4,
  argument5,
  argument6,
  argument7,
  argument8
);
```

Each argument is on its own line. The call site is scannable.

### Team Conventions

Teams should adopt and automate formatting conventions. Human judgment about formatting is slow and inconsistent. Automated formatting is fast and unambiguous. Use language-appropriate tools and integrate them into the pre-commit workflow.

#### Bad (Java)

```java
// Inconsistent formatting across a team
public void process(Order order){
    if(order.getStatus()==OrderStatus.PAID){
        calculateTotal(order);
    }
}

public void ship(Order order) {
  if (order.isReady()) {
    logisticsService.ship(order);
  }
}
```

Mixed brace styles, spacing, and indentation create noise during code review.

#### Good (Java)

```java
// spotlessApply() enforces consistent formatting
public void process(Order order) {
    if (order.getStatus() == OrderStatus.PAID) {
        calculateTotal(order);
    }
}

public void ship(Order order) {
    if (order.isReady()) {
        logisticsService.ship(order);
    }
}
```

Spotless (palantir) enforces Google Java Format or an equivalent standard.

#### Bad (TypeScript)

```typescript
// Mixed formatting
const x=1;
const y   =   2;
function foo(){
  return 3;
}
const arr=[1,2,3];
```

No enforced convention leads to inconsistent formatting.

#### Good (TypeScript)

```typescript
// Prettier enforces consistent formatting
const x = 1;
const y = 2;

function foo(): number {
  return 3;
}

const arr = [1, 2, 3];
```

Prettier and ESLint together enforce and fix formatting issues automatically.

### Lint Automation (Spotless, ESLint)

Automated linting catches style violations before code review, freeing reviewers to focus on substance. Integrate linting into the CI pipeline so that violations cannot be merged.

#### Bad (Java)

```java
// Manually formatted, style violations undetected
public class OrderService{
    private String name;
    private int count;
    public void setName(String n){this.name=n;}
    public void setCount(int c){this.count=c;}
}
```

Manual formatting is error-prone and wastes review time.

#### Good (Java)

```java
// spotlessApply() enforces:
// - 4-space indentation
// - One declaration per line
// - K&R brace style
// - No unused imports
public class OrderService {
    private String name;
    private int count;

    public void setName(String name) {
        this.name = name;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
```

Spotless runs in CI. Violations fail the build.

#### Bad (TypeScript)

```typescript
// ESLint violations undetected
const x:number=1;
const arr:any[]=[1,2,3];
function foo(){return 1;}

const unused = 5;
```

Type and style errors go undetected without linting.

#### Good (TypeScript)

```typescript
// ESLint + TypeScript strict mode
const x: number = 1;
const arr: number[] = [1, 2, 3];
function foo(): number {
  return 1;
}
```

ESLint catches style and type issues. No suppressions without justification.

### Class Organization (Public First, Fields at Top)

Class members should be organized in a consistent order: constants, fields, constructors, public methods, private methods. This order signals what is stable (public API) versus what is volatile (implementation details).

#### Bad (Java)

```java
public class Order {
    private void validate() { }  // Private method at top
    public void place() { }
    private Money calculateTotal() { }
    private UUID id;  // Field after methods
    private OrderStatus status;
    public Order(UUID id) {  // Constructor after field
        this.id = id;
    }
}
```

The reader must scan the entire file to find the public API.

#### Good (Java)

```java
public class Order {

    // Fields
    private final UUID id;
    private OrderStatus status;
    private final List<OrderLine> lines;

    // Constructor
    public Order(UUID id) {
        this.id = Objects.requireNonNull(id);
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }

    // Public API
    public void place() {
        validate();
        this.status = OrderStatus.PLACED;
    }

    public void cancel() {
        if (this.status != OrderStatus.DRAFT) {
            throw new OrderCannotBeCancelledException();
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Private helpers
    private void validate() {
        if (this.lines.isEmpty()) {
            throw new EmptyOrderException();
        }
    }
}
```

The public API is immediately visible. Implementation details are below.

#### Bad (TypeScript)

```typescript
class OrderProcessor {
  private validate(order: Order): void { }  // Private first
  private calculateTotal(order: Order): Money { }

  processOrder(order: Order): void {
    this.validate(order);
    const total = this.calculateTotal(order);
    this.save(order);
  }

  private save(order: Order): void { }
  private id: string;  // Field after methods
  private repository: OrderRepository;
}
```

The public API is buried.

#### Good (TypeScript)

```typescript
class OrderProcessor {
  // Dependencies
  private readonly repository: OrderRepository;

  // Constructor
  constructor(repository: OrderRepository) {
    this.repository = repository;
  }

  // Public API
  processOrder(order: Order): void {
    this.validate(order);
    const total = this.calculateTotal(order);
    this.save(order);
  }

  // Private helpers
  private validate(order: Order): void {
    if (order.lines.length === 0) {
      throw new EmptyOrderError();
    }
  }

  private calculateTotal(order: Order): Money {
    return order.lines.reduce((sum, line) => sum.add(line.subtotal()), Money.ZERO);
  }

  private save(order: Order): void {
    this.repository.save(order);
  }
}
```

The constructor comes before methods. Public methods come before private methods.

### Package Dependency Direction

In Clean Architecture, dependencies point inward: infrastructure depends on application, application depends on domain. Package structure should reinforce this by placing domain classes in packages that do not import from application, application classes in packages that do not import from infrastructure.

#### Bad (Java)

```java
// Domain layer importing from Infrastructure (violates dependency rule)
package com.example.domain.model;

import com.example.infrastructure.persistence.JpaOrderRepository;

public class Order {
    private JpaOrderRepository repository;  // Domain depends on Infrastructure
}
```

Domain should never depend on infrastructure.

#### Good (Java)

```java
// Domain layer: defines only domain concepts
package com.example.domain.model;

public class Order {
    private OrderId id;
    private OrderStatus status;
}

// Application layer: defines repository interface
package com.example.application.port;

import com.example.domain.model.Order;

public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
}

// Infrastructure layer: implements the interface
package com.example.infrastructure.persistence;

@Repository
public class JpaOrderRepository implements OrderRepository {
    // Implementation
}
```

Domain defines the interface. Infrastructure implements it. Dependencies point inward.

#### Bad (TypeScript)

```typescript
// Application layer importing from infrastructure
// src/application/OrderService.ts
import { PrismaOrderRepository } from '../infrastructure/prisma/PrismaOrderRepository';

class OrderService {
  private repository: PrismaOrderRepository;  // Application depends on Infrastructure
}
```

The application layer should not depend on infrastructure implementations.

#### Good (TypeScript)

```typescript
// Domain layer: defines interface
// src/domain/OrderRepository.ts
export interface OrderRepository {
  findById(id: OrderId): Promise<Order | undefined>;
  save(order: Order): Promise<void>;
}

// Application layer: depends on interface
// src/application/OrderService.ts
import type { OrderRepository } from '../domain/OrderRepository';

class OrderService {
  constructor(private readonly repository: OrderRepository) {}
}

// Infrastructure layer: implements interface
// src/infrastructure/prisma/PrismaOrderRepository.ts
import type { OrderRepository } from '../../domain/OrderRepository';
import { prisma } from './client';

export class PrismaOrderRepository implements OrderRepository {
  async findById(id: OrderId): Promise<Order | undefined> {
    return prisma.order.findUnique({ where: { id: id.value } });
  }
}
```

The domain defines the contract. Infrastructure provides the implementation. Application depends only on the domain interface.

## Checklist

- [ ] Class organization follows: fields, constructor, public methods, private methods.
- [ ] Public API is at the top of the file; details follow below.
- [ ] Line length is reasonable (80-120 characters); long lines are broken at natural boundaries.
- [ ] Formatting is automated with Spotless (Java) or Prettier + ESLint (TypeScript).
- [ ] Pre-commit hooks prevent unformatted code from being committed.
- [ ] CI pipeline enforces formatting standards.
- [ ] Package structure reinforces architectural boundaries.
- [ ] Domain layer has no imports from infrastructure or application layers.
- [ ] Dependency direction is inward: domain <- application <- infrastructure.

## Cross-References

- [`../code-quality.md`](./code-quality.md) - General code quality principles
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) - Domain layer structure
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) - Application layer structure
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) - Infrastructure layer structure
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) - Interface layer structure
- [`../clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) - End-to-end architecture
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) - Strategic design decisions
