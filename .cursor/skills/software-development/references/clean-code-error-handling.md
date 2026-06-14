# Clean Code: Error Handling

Error handling is not separate from the logic of a program; it is part of the logic. How a system handles failures determines whether it recovers gracefully or propagates corruption. Robust error handling separates the happy path from exceptional paths, provides meaningful context, and fails in a way that aids diagnosis.

## Why It Matters

Every non-trivial system encounters failures: network timeouts, invalid inputs, resource exhaustion, race conditions. The quality of error handling determines whether these failures result in graceful degradation or catastrophic cascades. Poor error handling hides bugs, corrupts data, and produces unhelpful diagnostics that slow down incident response.

Well-designed error handling makes three guarantees: errors are detected at the right boundary, meaningful context is preserved as errors propagate, and callers receive enough information to respond appropriately. This requires intentional design, not defensive sprinkling of try-catch blocks.

## Smell Catalog

### Use Exceptions (Not Return Codes) for Unexpected Errors

Return codes force every caller to check for special values, creating boilerplate that obscures intent. Exceptions separate the happy path from the error path, allowing callers to handle errors explicitly where appropriate. Use exceptions for conditions that the caller is not expected to handle routinely.

#### Bad (Java)

```java
public int findCustomerIndex(List<Customer> customers, String email) {
    for (int i = 0; i < customers.size(); i++) {
        if (customers.get(i).getEmail().equals(email)) {
            return i;
        }
    }
    return -1;  // Magic value for "not found"
}

public void processByEmail(List<Customer> customers, String email) {
    int index = findCustomerIndex(customers, email);
    if (index == -1) {  // Must remember to check
        System.out.println("Customer not found");
        return;
    }
    process(customers.get(index));
}
```

The magic value `-1` requires the caller to remember the contract. There is no type safety.

#### Good (Java)

```java
public Optional<Integer> findCustomerIndex(List<Customer> customers, String email) {
    IntStream.range(0, customers.size())
        .filter(i -> customers.get(i).getEmail().equals(email))
        .findFirst();
}

public void processByEmail(List<Customer> customers, String email) {
    findCustomerIndex(customers, email)
        .ifPresentOrElse(
            index -> process(customers.get(index)),
            () -> System.out.println("Customer not found")
        );
}
```

`Optional` makes absence explicit in the type system. The caller cannot forget to check.

#### Bad (TypeScript)

```typescript
function parseConfig(config: string): number {
  const value = parseInt(config, 10);
  if (isNaN(value)) return -1;
  return value;
}

const result = parseConfig('invalid');
if (result === -1) {
  console.log('Parse failed');
} else {
  console.log('Parsed:', result);
}
```

The magic value `-1` is indistinguishable from a valid result.

#### Good (TypeScript)

```typescript
function parseConfig(config: string): number | null {
  const value = parseInt(config, 10);
  if (isNaN(value)) return null;
  return value;
}

const result = parseConfig('invalid');
if (result === null) {
  console.log('Parse failed');
} else {
  console.log('Parsed:', result);
}
```

`null` explicitly marks the failure case. The type system enforces the check.

### Unchecked vs Checked Exceptions in Java

Use unchecked exceptions for programming errors and contract violations that indicate bugs in the system. Use checked exceptions sparingly for recoverable conditions that the caller is expected to handle. Checked exceptions can become an API maintenance burden; prefer Result types or unchecked exceptions in most cases.

#### Bad (Java)

```java
// Overusing checked exceptions creates a tangled API
public void processOrder(String orderId) throws OrderNotFoundException,
    OrderValidationException, OrderPersistenceException, OrderNotificationException {
    Order order = orderRepository.findById(orderId);  // throws
    orderValidator.validate(order);  // throws
    orderRepository.save(order);  // throws
    notificationService.notify(order);  // throws
}
```

Every caller must declare or catch four checked exceptions. The exception hierarchy leaks into every layer.

#### Good (Java)

```java
// Use unchecked exceptions with clear hierarchy
public class OrderProcessingException extends RuntimeException {
    private final OrderId orderId;

    public OrderProcessingException(String message, OrderId orderId) {
        super(message);
        this.orderId = orderId;
    }

    public OrderId orderId() { return orderId; }
}

// At the boundary, translate to appropriate response
public void processOrder(OrderId orderId) {
    try {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        orderProcessor.process(order);
    } catch (OrderNotFoundException e) {
        throw new OrderNotFoundHttpException(orderId);
    }
}
```

Unchecked exceptions simplify the happy path. The boundary layer translates to HTTP responses.

#### TypeScript Equivalent

```typescript
// TypeScript does not have checked exceptions, so use Result types
type Result<T, E extends Error = Error> =
  | { success: true; value: T }
  | { success: false; error: E };

function parseOrderId(raw: string): Result<OrderId, InvalidOrderIdError> {
  try {
    return { success: true, value: new OrderId(raw) };
  } catch (e) {
    return { success: false, error: new InvalidOrderIdError(raw) };
  }
}
```

Result types make errors explicit without relying on exception handling.

### Error Context Wrapping

When catching and rethrowing exceptions, include context about what was being attempted. The original exception should be preserved as the cause so that the full stack trace is available for diagnosis.

#### Bad (Java)

```java
try {
    orderRepository.save(order);
} catch (DataAccessException e) {
    throw new RuntimeException("Save failed");  // Original cause lost
}
```

The original stack trace is lost. The message is too generic to be useful.

#### Good (Java)

```java
try {
    orderRepository.save(order);
} catch (DataAccessException e) {
    throw new OrderPersistenceException(
        "Failed to persist order " + order.getId(), order.getId(), e
    );
}
```

The new exception wraps the original as a cause. Context about which order failed is included.

#### Bad (TypeScript)

```typescript
try {
  await saveOrder(order);
} catch (e) {
  throw new Error('Save failed');
}
```

The original error is lost. The message is uninformative.

#### Good (TypeScript)

```typescript
try {
  await saveOrder(order);
} catch (e) {
  if (e instanceof DatabaseError) {
    throw new OrderPersistenceError(
      `Failed to persist order ${order.id}`,
      { orderId: order.id, cause: e }
    );
  }
  throw e;
}
```

Domain-specific error types carry context. Original errors are preserved.

### Null Handling

Null references are a billion-dollar mistake (Hoare, 2009). In Java, use `Objects.requireNonNull` for mandatory arguments, `Optional` for return values that may be absent, and defensive copies for mutable objects. In TypeScript, prefer `undefined` over `null` and use optional chaining and nullish coalescing for safe access.

#### Bad (Java)

```java
public void sendEmail(String to, String subject, String body) {
    if (to == null) {
        // Silently ignore
        return;
    }
    emailService.send(to, subject, body);
}
```

Silently ignoring null parameters hides bugs. The caller does not know the email was not sent.

#### Good (Java)

```java
public void sendEmail(String to, String subject, String body) {
    Objects.requireNonNull(to, "Recipient email is required");
    Objects.requireNonNull(subject, "Subject is required");
    Objects.requireNonNull(body, "Body is required");
    emailService.send(to, subject, body);
}
```

Fail fast at the boundary. The caller receives an immediate, clear exception.

#### Bad (Java)

```java
public User findUserById(UUID id) {
    return userRepository.findById(id).orElse(null);  // Returns null
}

public void greet() {
    User user = userService.findUserById(id);
    System.out.println(user.getName());  // NullPointerException if not found
}
```

Returning null forces the caller to null-check. Forgetting the check causes NPE.

#### Good (Java)

```java
public Optional<User> findUserById(UUID id) {
    return userRepository.findById(id);
}

public void greet() {
    userService.findUserById(id)
        .ifPresentOrElse(
            user -> System.out.println(user.getName()),
            () -> System.out.println("Guest")
        );
}
```

`Optional` makes absence explicit. The caller must handle both cases.

#### Bad (TypeScript)

```typescript
function getUser(id: string): User | null {
  return db.users.findById(id) ?? null;
}

const user = getUser('123');
console.log(user.name);  // Object is possibly 'null'
```

Null return type forces the caller to check.

#### Good (TypeScript)

```typescript
function getUser(id: string): User | undefined {
  return db.users.findById(id);
}

function greet(id: string): void {
  const user = getUser(id);
  console.log(user?.name ?? 'Guest');
}
```

Optional chaining and nullish coalescing handle absence safely.

### Result/Either Type for Expected Errors in TypeScript

For operations where failure is expected and recoverable, use a discriminated union Result type rather than throwing exceptions. This makes error handling explicit in the type system and forces callers to handle both cases.

#### Bad (TypeScript)

```typescript
// Throwing exceptions for expected failures
function validateAge(age: number): number {
  if (age < 0 || age > 150) {
    throw new ValidationError('Age out of range');
  }
  return age;
}

function process() {
  try {
    const age = validateAge(input);
    console.log('Valid age:', age);
  } catch (e) {
    if (e instanceof ValidationError) {
      console.log('Validation failed:', e.message);
    }
  }
}
```

Exceptions for expected failures force try-catch wrapping everywhere.

#### Good (TypeScript)

```typescript
type ValidationResult = { success: true; value: number } |
                         { success: false; error: string };

function validateAge(age: number): ValidationResult {
  if (age < 0 || age > 150) {
    return { success: false, error: 'Age out of range' };
  }
  return { success: true, value: age };
}

function process(): void {
  const result = validateAge(input);
  if (result.success) {
    console.log('Valid age:', result.value);
  } else {
    console.log('Validation failed:', result.error);
  }
}
```

The Result type makes the error path explicit. Pattern matching handles both cases.

### Handle Errors at Boundaries

External systems (databases, file systems, network services) are where errors occur. Handle them at the boundary layer, translating infrastructure exceptions into domain exceptions that the rest of the application understands.

#### Bad (Java)

```java
// Throwing raw SQLException up the call stack
public List<Order> findOrdersByCustomer(UUID customerId) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT * FROM orders WHERE customer_id = ?"
        );
        stmt.setObject(1, customerId);
        ResultSet rs = stmt.executeQuery();
        // map results
    }
}
```

`SQLException` is an infrastructure concern that should not leak into the domain layer.

#### Good (Java)

```java
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomer(CustomerId customerId);
}

@Repository
public class JpaOrderRepository implements OrderRepository {

    @Override
    public List<Order> findByCustomer(CustomerId customerId) {
        try {
            return jdbcTemplate.query(
                "SELECT * FROM orders WHERE customer_id = ?",
                orderRowMapper,
                customerId.value()
            );
        } catch (DataAccessException e) {
            throw new OrderQueryException(
                "Failed to query orders for customer " + customerId, e
            );
        }
    }
}
```

The infrastructure layer translates `DataAccessException` into a domain exception. The domain layer remains ignorant of persistence details.

#### Bad (TypeScript)

```typescript
// Raw fetch errors propagating everywhere
async function fetchUser(id: string) {
  const response = await fetch(`/api/users/${id}`);
  const data = await response.json();
  return data;
}
```

Network errors, HTTP errors, and parse errors all propagate without context.

#### Good (TypeScript)

```typescript
type ApiResult<T> = { success: true; data: T } |
                     { success: false; error: ApiError };

class ApiError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
    public readonly endpoint: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function fetchUser(id: string): Promise<ApiResult<User>> {
  try {
    const response = await fetch(`/api/users/${id}`);
    if (!response.ok) {
      return {
        success: false,
        error: new ApiError('Failed to fetch user', response.status, `/api/users/${id}`)
      };
    }
    const data = await response.json();
    return { success: true, data };
  } catch (e) {
    return {
      success: false,
      error: new ApiError('Network error', 0, `/api/users/${id}`)
    };
  }
}
```

The boundary returns a discriminated union. Callers handle success and failure explicitly.

### Do Not Return Null

Returning null forces every caller to check for null, creating boilerplate and potential for null pointer exceptions. Return empty collections instead of null for collection-returning methods, and return Optional in Java or `undefined` in TypeScript for optional values.

#### Bad (Java)

```java
public List<Order> findOrdersByStatus(OrderStatus status) {
    List<Order> orders = orderRepository.findByStatus(status);
    if (orders == null) {  // Caller must check
        return new ArrayList<>();
    }
    return orders;
}

public void displayOrders() {
    List<Order> orders = orderService.findOrdersByStatus(status);
    for (Order order : orders) {  // NPE if orders is null
        System.out.println(order.getId());
    }
}
```

Returning null from a collection method is a design error.

#### Good (Java)

```java
public List<Order> findOrdersByStatus(OrderStatus status) {
    return orderRepository.findByStatus(status);  // Never null
}

public void displayOrders() {
    List<Order> orders = orderService.findOrdersByStatus(status);
    for (Order order : orders) {  // Safe even if empty
        System.out.println(order.getId());
    }
}
```

The repository never returns null. An empty list is handled identically to a populated list.

#### Bad (TypeScript)

```typescript
function getItems(): Item[] | null {
  return db.items.findAll() ?? null;
}

const items = getItems();
if (items !== null) {  // Verbose null check
  items.forEach((item) => console.log(item.id));
}
```

Null return type is unnecessary and forces verbose checks.

#### Good (TypeScript)

```typescript
function getItems(): Item[] {
  return db.items.findAll();  // Always returns array
}

const items = getItems();
items.forEach((item) => console.log(item.id));  // Safe with empty array
```

Empty arrays are handled naturally. No null checks needed.

## Checklist

- [ ] Exceptions are used for unexpected errors; return codes are not used.
- [ ] Checked exceptions are used sparingly; unchecked exceptions are preferred.
- [ ] Exception messages include context about what failed.
- [ ] Original exceptions are preserved as causes when wrapping.
- [ ] Null parameters are validated at boundaries with `Objects.requireNonNull` (Java).
- [ ] Optional is used for return values that may be absent (Java).
- [ ] Optional chaining and nullish coalescing are used for safe access (TypeScript).
- [ ] Domain-specific error types exist for each error category.
- [ ] Infrastructure errors are translated to domain errors at boundaries.
- [ ] Empty collections are returned instead of null.
- [ ] Errors are logged at the boundary, not scattered throughout the domain.

## Cross-References

- [`../code-quality.md`](./code-quality.md) - General code quality principles
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) - Domain layer error handling
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) - Application layer error handling
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) - Infrastructure layer error translation
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) - Interface layer error responses
- [`../clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) - End-to-end error flow
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) - Strategic design decisions
