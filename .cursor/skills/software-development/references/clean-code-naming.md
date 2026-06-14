# Clean Code: Naming

Naming is the act of describing intent through identifiers. Good names eliminate guesswork, reduce cognitive load, and make code a form of documentation. Bad names obscure meaning and force readers to reverse-engineer intent from implementation.

## Why It Matters

Names appear everywhere in code: classes, methods, variables, constants, packages, files. The cumulative effect of naming decisions determines whether a codebase can be maintained by a team over time. A poorly named variable called `x` or a method called `process()` forces the reader to infer context that should have been explicit. Conversely, a well-named variable like `ordersPendingShipment` communicates intent at a glance.

The cost of bad naming compounds with scale. In a 10,000-line codebase, every unclear name adds friction to every future read, review, and modification. Naming is not a superficial concern; it is one of the most impactful forms of communication in software.

## Smell Catalog

### Class Naming

Class names should be nouns or noun phrases that describe the entity they represent. Avoid generic names like `Manager`, `Handler`, or `Processor` unless the role is genuinely generic and documented.

#### Bad (Java)

```java
public class Data {
    private String n;
    private Object o;
}
```

A class named `Data` conveys nothing about its purpose. Fields named `n` and `o` are uninterpretable without deep context.

#### Good (Java)

```java
public record EmailAddress(String localPart, String domain) {

    public EmailAddress {
        Objects.requireNonNull(localPart, "Local part is required");
        Objects.requireNonNull(domain, "Domain is required");
        if (localPart.isBlank() || domain.isBlank()) {
            throw new IllegalArgumentException("Email parts cannot be blank");
        }
    }
}
```

A `record` named `EmailAddress` immediately conveys purpose. Fields have descriptive names that match their roles. This is a value object that models an email address with self-validation.

#### Bad (TypeScript)

```typescript
class Utils {
  static doStuff(x: any): any {
    return x.filter((i: any) => i.active);
  }
}
```

A class named `Utils` is a dumping ground for unrelated helpers. Method `doStuff` reveals nothing about what it does.

#### Good (TypeScript)

```typescript
type Customer = {
  id: CustomerId;
  email: EmailAddress;
  tier: CustomerTier;
  activatedAt: Date | null;
};

type CustomerId = string & { readonly brand: unique symbol };
type CustomerTier = 'standard' | 'premium' | 'enterprise';
```

Branded types and discriminated structures make illegal states unrepresentable. Names are precise and self-documenting.

### Method Naming

Methods perform actions, so their names should be verbs or verb phrases. The name should describe what the method does, not how it does it. Prefer specific verbs over generic ones like `handle`, `manage`, or `process`.

#### Bad (Java)

```java
public void handle(Order o) {
    if (o.getStatus() == 1) {
        service.doThing(o);
    }
}
```

The method name `handle` is vague. The status check uses a magic number `1`. The call to `doThing` is equally opaque.

#### Good (Java)

```java
public void shipOrder(Order order) {
    if (order.canBeShipped()) {
        logisticsService.scheduleShipment(order);
        order.markAsShipped();
    }
}
```

`shipOrder` is an explicit verb phrase. `canBeShipped()` is a predicate that encapsulates the status logic. The intent is clear without reading the implementation.

#### Bad (TypeScript)

```typescript
function proc(data: any[]) {
  return data.map((item: any) => {
    if (item.s === 'active') {
      return { ...item, v: true };
    }
    return item;
  });
}
```

Single-letter variable names and magic strings like `s` and `v` make this code unreadable without context.

#### Good (TypeScript)

```typescript
type Item = { status: ItemStatus; value: unknown };
type ItemStatus = 'active' | 'inactive';

function activateItems(items: Item[]): Item[] {
  return items.map((item) =>
    item.status === 'active' ? { ...item, value: true } : item
  );
}
```

Descriptive names and explicit types eliminate ambiguity. The function name `activateItems` describes exactly what it does.

### Variable Naming

Variables should be named at a level of abstraction that matches their scope. Longer, more descriptive names are appropriate for variables with broader scope. Short names are acceptable for tight-scope variables like loop counters.

#### Bad (Java)

```java
public boolean check(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        if (arr[i] > 100) return true;
    }
    return false;
}
```

The function `check` and variable `arr` reveal nothing. A reader must inspect the implementation to understand what "check" means.

#### Good (Java)

```java
public boolean hasOverdueOrders(List<Order> orders) {
    for (Order order : orders) {
        if (order.isOverdue()) {
            return true;
        }
    }
    return false;
}
```

`hasOverdueOrders` names the intent. `orders` is the collection being examined. The loop variable `order` is clear within its tight scope.

#### Bad (TypeScript)

```typescript
function calc(o: any) {
  let r = 0;
  for (let i of o.items) {
    r += i.p * i.q;
  }
  return r;
}
```

The parameter `o`, accumulator `r`, and fields `p` and `q` require the reader to decode the domain model.

#### Good (TypeScript)

```typescript
type OrderLine = { productPrice: number; quantity: number };

function calculateSubtotal(orderLines: OrderLine[]): number {
  let subtotal = 0;
  for (const line of orderLines) {
    subtotal += line.productPrice * line.quantity;
  }
  return subtotal;
}
```

Domain-appropriate names make the calculation self-explanatory.

### Boolean Naming

Boolean variables and methods that return boolean values should be named as predicates or conditions. Prefix boolean variables with `is`, `has`, `can`, `should`, or `was` to indicate their nature.

#### Bad (Java)

```java
private boolean active = false;
private boolean done = false;

public boolean check() {
    return active && !done;
}
```

`check()` is not a predicate. The boolean fields lack context.

#### Good (Java)

```java
private boolean isActivated = false;
private boolean isCompleted = false;

public boolean isEligibleForPromotion() {
    return isActivated && !isCompleted;
}
```

`isEligibleForPromotion()` is an unambiguous predicate that describes the condition.

#### Bad (TypeScript)

```typescript
const flag = true;
const check = () => flag;
```

`flag` and `check` are uninterpretable without context.

#### Good (TypeScript)

```typescript
const isAccountVerified = true;
const canProceedToCheckout = (): boolean => isAccountVerified;
```

`isAccountVerified` and `canProceedToCheckout` are self-documenting predicates.

### Constant Naming

Constants should use UPPER_SNAKE_CASE to distinguish them from variables. The name should describe the value it represents, not just the fact that it is constant.

#### Bad (Java)

```java
public static final int X = 100;
public static final String MSG = "Error";
```

`X` and `MSG` are meaningless without context.

#### Good (Java)

```java
public static final int MAX_RETRY_ATTEMPTS = 3;
public static final String DEFAULT_ERROR_MESSAGE = "An unexpected error occurred. Please try again.";
public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100.00");
```

Constants have names that convey their purpose and, where applicable, their units.

#### Bad (TypeScript)

```typescript
const MAX = 1000;
const URL = 'https://api.example.com';
```

`MAX` lacks context. `URL` could refer to any URL.

#### Good (TypeScript)

```typescript
const MAX_UPLOAD_SIZE_BYTES = 1_000_000;
const API_BASE_URL = 'https://api.example.com' as const;
const DEFAULT_PAGE_SIZE = 20;
```

Names are descriptive and scoped to their purpose.

### Package and File Naming

Package names should follow the reversed domain convention in Java. File names in TypeScript should match the primary exported symbol. Avoid generic file names like `utils.ts` or `helpers.java` that bundle unrelated functionality.

#### Bad (Java)

```java
package com.example.app.util;
package com.example.app.helpers;
```

Packages named `util` and `helpers` become dumping grounds for unrelated code.

#### Good (Java)

```java
package com.example.domain.model.order;
package com.example.domain.model.customer;
package com.example.application.usecase.order;
```

Domain-driven package naming reflects the domain model and makes the codebase navigable.

#### Bad (TypeScript)

```typescript
// src/utils/helpers.ts
export function helper1() {}
export function helper2() {}
```

A file containing unrelated functions with generic names obscures the codebase structure.

#### Good (TypeScript)

```typescript
// src/domain/validation/EmailValidator.ts
export class EmailValidator {
  static isValid(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}
```

The file name matches the class name. The class is in a package that reflects its role.

### Scope and Length

The scope of a name should inversely correlate with its length. Variables used in a few lines can have short names. Variables used across a function or class need longer, more descriptive names. The goal is to minimize cognitive load at every usage point.

#### Bad (Java)

```java
public void processOrders(List<Order> ordersList) {
    for (Order orderObject : ordersList) {
        if (orderObject.getStatus() == OrderStatus.PENDING) {
            orderObject.activate();
        }
    }
}
```

The names `ordersList` and `orderObject` are redundant. The variable `order` is sufficient within the loop.

#### Good (Java)

```java
public void activatePendingOrders(List<Order> orders) {
    for (Order order : orders) {
        if (order.isPending()) {
            order.activate();
        }
    }
}
```

Single-word loop variable `order` is clear within the tight scope. The method name captures the overall intent.

#### Bad (TypeScript)

```typescript
function findCustomerByEmail(customersList: Customer[], emailToFind: string) {
  const foundCustomer = customersList.find(
    (customerObject) => customerObject.email === emailToFind
  );
  return foundCustomer;
}
```

Verbose variable names within a narrow scope add noise without value.

#### Good (TypeScript)

```typescript
function findCustomerByEmail(customers: Customer[], email: string): Customer | undefined {
  return customers.find((customer) => customer.email === email);
}
```

Terse names within the lambda are appropriate. The public API uses descriptive names.

### Search-Friendly Names

Names should be searchable. Avoid single-letter names (except loop counters), homoglyphs, and encoding tricks. Names that appear frequently should be distinctive enough to find via grep or IDE search.

#### Bad (Java)

```java
public class OrderService {
    public Order o(UUID id) { return repo.find(id); }
}
```

Method `o` is unsearchable and will match dozens of unrelated patterns.

#### Good (Java)

```java
public class OrderService {
    public Optional<Order> findOrderById(UUID orderId) {
        return orderRepository.findById(orderId);
    }
}
```

`findOrderById` is searchable, distinctive, and describes the operation.

#### Bad (TypeScript)

```typescript
const l = (arr: any[], n: number) => arr[arr.length - n];
```

Single-letter identifiers are impossible to search meaningfully.

#### Good (TypeScript)

```typescript
function getLastNElements<T>(array: T[], count: number): T[] {
  return array.slice(-count);
}
```

`getLastNElements` is searchable and descriptive.

## Checklist

- [ ] Class names are nouns that describe the entity, not generic catch-alls.
- [ ] Method names are verbs that describe the action performed.
- [ ] Boolean variables and predicates use `is`, `has`, `can`, `should`, or `was` prefixes.
- [ ] Constants are named in UPPER_SNAKE_CASE and describe their purpose.
- [ ] Variable names are proportional to their scope: longer for broad scope, shorter for tight scope.
- [ ] No single-letter variable names outside loop counters.
- [ ] No magic numbers or magic strings; extract to named constants.
- [ ] Package names reflect the domain model, not generic utility buckets.
- [ ] File names match the primary exported symbol.
- [ ] Names are searchable and distinctive within the codebase.

## Cross-References

- [`../code-quality.md`](./code-quality.md) - General code quality principles
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) - Domain layer naming conventions
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) - Application layer naming conventions
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) - Infrastructure layer naming conventions
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) - Interface layer naming conventions
- [`../clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) - End-to-end naming consistency
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) - Strategic design decisions
