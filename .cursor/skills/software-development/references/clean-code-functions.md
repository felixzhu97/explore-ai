# Clean Code: Functions

Functions are the fundamental unit of abstraction in procedural and functional code. A well-designed function does one thing, does it well, and does it only. Functions that are too long, do too many things, or have unclear contracts are the primary source of code that resists comprehension and modification.

## Why It Matters

Every function is a promise to its callers: call me with these inputs, and I will produce this output or side effect. When functions violate this contract by doing multiple things, accepting too many parameters, or producing hidden side effects, the codebase becomes a web of implicit dependencies that resist change.

Small, focused functions improve every downstream concern: testing becomes trivial, reuse becomes natural, and naming becomes easier. The discipline of writing small functions is the foundation of code that can be read, tested, and maintained by a team over years.

## Smell Catalog

### Small Functions

Functions should be small. The ideal function fits on a single screen, typically under 20 lines. When a function exceeds 40 lines, it almost always does more than one thing. Extract until each function is a coherent unit of work.

#### Bad (Java)

```java
public void processOrder(Order order) {
    // Validate order
    if (order == null) throw new IllegalArgumentException("Order cannot be null");
    if (order.getLines().isEmpty()) throw new IllegalStateException("Order has no lines");
    // Calculate total
    BigDecimal total = BigDecimal.ZERO;
    for (OrderLine line : order.getLines()) {
        total = total.add(line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
    }
    // Apply discount
    if (total.compareTo(new BigDecimal("100")) > 0) {
        total = total.multiply(new BigDecimal("0.9"));
    }
    // Save order
    order.setTotal(total);
    order.setStatus(OrderStatus.PROCESSED);
    orderRepository.save(order);
    // Send notification
    emailService.sendOrderConfirmation(order.getCustomerEmail(), order);
    // Log
    logger.info("Order processed: " + order.getId());
}
```

This function does validation, calculation, discount application, persistence, notification, and logging. It should be split.

#### Good (Java)

```java
public void processOrder(Order order) {
    validateOrder(order);
    Money total = calculateOrderTotal(order);
    Money discountedTotal = applyDiscountIfEligible(total);
    persistOrder(order, discountedTotal);
    notifyCustomer(order);
    logOrderProcessing(order);
}

private void validateOrder(Order order) {
    Objects.requireNonNull(order, "Order cannot be null");
    if (order.getLines().isEmpty()) {
        throw new IllegalStateException("Order has no lines");
    }
}

private Money calculateOrderTotal(Order order) {
    return order.getLines().stream()
        .map(line -> line.getPrice().multiply(line.getQuantity()))
        .reduce(Money.ZERO, Money::add);
}
```

Each function does one thing. The top-level function reads like a specification.

#### Bad (TypeScript)

```typescript
function handleSubmit(form: any) {
  let errors: string[] = [];
  if (!form.name) errors.push('Name is required');
  if (!form.email) errors.push('Email is required');
  if (!form.email.includes('@')) errors.push('Invalid email');
  if (form.age < 18) errors.push('Must be 18 or older');
  if (errors.length > 0) {
    console.log('Validation errors:', errors);
    return;
  }
  const user = { ...form, status: 'active', createdAt: new Date() };
  db.save(user);
  email.send(user.email, 'Welcome');
  console.log('User created:', user.id);
}
```

Multiple responsibilities bundled into a single function: validation, transformation, persistence, notification, and logging.

#### Good (TypeScript)

```typescript
type FormData = { name: string; email: string; age: number };
type ValidationError = string;

function validateForm(form: FormData): ValidationError[] {
  const errors: ValidationError[] = [];
  if (!form.name) errors.push('Name is required');
  if (!form.email || !form.email.includes('@')) errors.push('Invalid email');
  if (form.age < 18) errors.push('Must be 18 or older');
  return errors;
}

function createUser(form: FormData): User {
  return { ...form, status: 'active' as const, createdAt: new Date() };
}
```

Each function has a single responsibility. Composition happens at the call site.

### Single Responsibility

A function should have one reason to change. If you find yourself conjuring scenarios where the function would need modification for reasons unrelated to its current purpose, it likely violates this principle.

#### Bad (Java)

```java
public class UserService {
    public void registerUser(String email, String password) {
        // Validate email format
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email");
        // Hash password
        String hashed = hash(password);
        // Save to database
        User user = new User(email, hashed);
        db.save(user);
        // Send welcome email
        emailService.send(email, "Welcome!");
    }
}
```

`registerUser` mixes validation, hashing, persistence, and notification. Each should be separately testable and reusable.

#### Good (Java)

```java
public class UserRegistrationService {
    private final EmailValidator emailValidator;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public void registerUser(String email, String password) {
        emailValidator.validate(email);
        String hashedPassword = passwordHasher.hash(password);
        User user = User.create(email, hashedPassword);
        userRepository.save(user);
        notificationService.sendWelcomeEmail(user);
    }
}
```

Each dependency handles one concern. The service orchestrates them.

#### Bad (TypeScript)

```typescript
async function importAndProcessCSV(filePath: string) {
  const content = await fs.readFile(filePath, 'utf-8');
  const rows = content.split('\n').map((line) => line.split(','));
  const validated = rows.filter((row) => row.length === 4);
  const transformed = validated.map(([a, b, c, d]) => ({ a, b, c, d }));
  await db.insertMany(transformed);
  await audit.log('CSV imported', transformed.length);
}
```

File reading, parsing, validation, transformation, persistence, and auditing are all in one function.

#### Good (TypeScript)

```typescript
async function importAndProcessCSV(filePath: string): Promise<void> {
  const rawContent = await readFileContent(filePath);
  const rows = parseCSV(rawContent);
  const validRows = filterValidRows(rows);
  const records = transformToRecords(validRows);
  await persistRecords(records);
  await audit.log('CSV imported', records.length);
}
```

Each step is a named function. The top-level function reads as an executive summary.

### Parameter Count

Functions with many parameters are difficult to call, difficult to test, and often violate Single Responsibility. Aim for three or fewer parameters. When more are needed, use a parameter object.

#### Bad (Java)

```java
public Order createOrder(UUID customerId, String productName, int quantity,
    BigDecimal price, String shippingAddress, String billingAddress,
    PaymentMethod paymentMethod, String couponCode) {
    // ...
}
```

Seven parameters require the caller to remember the exact order. This is error-prone and hard to read.

#### Good (Java)

```java
public record CreateOrderCommand(
    UUID customerId,
    String productName,
    int quantity,
    BigDecimal price,
    Address shippingAddress,
    Address billingAddress,
    PaymentMethod paymentMethod,
    String couponCode
) {}

public Order createOrder(CreateOrderCommand command) {
    // ...
}
```

A parameter object groups related inputs. Named parameters at the call site improve readability.

#### Bad (TypeScript)

```typescript
function calculateFinalPrice(
  basePrice: number,
  quantity: number,
  discountPercent: number,
  taxRate: number,
  shippingCost: number,
  currency: string
): number {
  const subtotal = basePrice * quantity;
  const discount = subtotal * (discountPercent / 100);
  const taxable = subtotal - discount;
  const tax = taxable * taxRate;
  return taxable + tax + shippingCost;
}
```

Six parameters, each with a different unit and purpose, make the function signature confusing.

#### Good (TypeScript)

```typescript
type PricingInput = {
  basePrice: number;
  quantity: number;
  discountPercent: number;
  taxRate: number;
  shippingCost: number;
  currency: string;
};

function calculateFinalPrice(input: PricingInput): number {
  const { basePrice, quantity, discountPercent, taxRate, shippingCost } = input;
  const subtotal = basePrice * quantity;
  const discount = subtotal * (discountPercent / 100);
  const taxable = subtotal - discount;
  const tax = taxable * taxRate;
  return taxable + tax + shippingCost;
}
```

The parameter object groups related inputs. Each field is named at the call site.

### Command-Query Separation

A function should either perform an action (command) or return information (query), but not both. Mixing commands and queries leads to confusing APIs and subtle bugs.

#### Bad (Java)

```java
public String saveAndReturnId(Order order) {
    orderRepository.save(order);
    return order.getId().toString();
}
```

This function both mutates state and returns a value. Callers may not expect the mutation.

#### Good (Java)

```java
public void saveOrder(Order order) {
    orderRepository.save(order);
}

public String getOrderIdAsString(Order order) {
    return order.getId().toString();
}
```

Command and query are separate. Each function has a single purpose.

#### Bad (TypeScript)

```typescript
function authenticateAndReturnToken(credentials: Credentials): string {
  const user = db.findUser(credentials);
  if (!user) throw new Error('Invalid credentials');
  user.lastLogin = new Date();
  db.save(user);
  return generateToken(user);
}
```

This function authenticates, updates last login, and returns a token. Three distinct operations.

#### Good (TypeScript)

```typescript
function authenticate(credentials: Credentials): User {
  const user = db.findUser(credentials);
  if (!user) throw new Error('Invalid credentials');
  return user;
}

function updateLastLogin(user: User): void {
  user.lastLogin = new Date();
  db.save(user);
}

function generateSessionToken(user: User): string {
  return generateToken(user);
}
```

Each operation is isolated and composable.

### No Side Effects

A function should not produce side effects unless that is its explicit purpose. Side effects include modifying parameters, mutating global state, writing to files, or sending network requests. Hidden side effects make code unpredictable and difficult to test.

#### Bad (Java)

```java
public class OrderProcessor {
    private int processedCount = 0;

    public void processOrder(Order order) {
        orderRepository.save(order);
        processedCount++;  // Side effect: modifies internal state
        logger.info("Processed order: " + order.getId());  // Side effect: logging
    }
}
```

The caller cannot tell that `processOrder` modifies `processedCount` and logs. This creates hidden dependencies.

#### Good (Java)

```java
public record OrderProcessingResult(
    Order order,
    int totalProcessedCount
) {}

public OrderProcessingResult processOrder(Order order, int currentCount) {
    Order savedOrder = orderRepository.save(order);
    int newCount = currentCount + 1;
    logger.info("Processed order: " + order.getId());
    return new OrderProcessingResult(savedOrder, newCount);
}
```

The function accepts its inputs and returns its outputs. State changes are explicit.

#### Bad (TypeScript)

```typescript
let processedCount = 0;
function processOrder(order: Order) {
  db.orders.save(order);
  processedCount++;
  console.log('Processed:', order.id);
}
```

Global state mutation and console logging are hidden side effects.

#### Good (TypeScript)

```typescript
function processOrder(order: Order): { order: Order; totalCount: number } {
  const savedOrder = await db.orders.save(order);
  return { order: savedOrder, totalCount: savedOrder.processedCount };
}
```

The function returns its results. Side effects are explicit in the return value.

### Prefer Exceptions Over Error Codes

Returning error codes forces the caller to check for special values. Exceptions create a clear separation between the happy path and error handling. Use unchecked exceptions for programming errors and checked exceptions sparingly for recoverable conditions that the caller must handle.

#### Bad (Java)

```java
public int findCustomerIndex(List<Customer> customers, String email) {
    for (int i = 0; i < customers.size(); i++) {
        if (customers.get(i).getEmail().equals(email)) {
            return i;
        }
    }
    return -1;  // Error code: -1 means not found
}

public void handleOrder() {
    int index = findCustomerIndex(customers, email);
    if (index == -1) {
        System.out.println("Customer not found");
    } else {
        processCustomer(customers.get(index));
    }
}
```

The caller must remember that `-1` indicates absence. There is no type safety.

#### Good (Java)

```java
public Optional<Integer> findCustomerIndex(List<Customer> customers, String email) {
    for (int i = 0; i < customers.size(); i++) {
        if (customers.get(i).getEmail().equals(email)) {
            return Optional.of(i);
        }
    }
    return Optional.empty();
}

public void handleOrder() {
    int index = findCustomerIndex(customers, email)
        .orElseThrow(() -> new CustomerNotFoundException(email));
    processCustomer(customers.get(index));
}
```

`Optional` makes the possibility of absence explicit in the type system.

#### Bad (TypeScript)

```typescript
function parseInteger(value: string): number {
  const parsed = parseInt(value, 10);
  if (isNaN(parsed)) return -1;
  return parsed;
}

const result = parseInteger('abc');
if (result === -1) {
  console.log('Invalid input');
} else {
  console.log('Parsed:', result);
}
```

Return value of `-1` for an error case is a magic value that the caller must remember.

#### Good (TypeScript)

```typescript
function parseInteger(value: string): number | null {
  const parsed = parseInt(value, 10);
  if (isNaN(parsed)) return null;
  return parsed;
}

const result = parseInteger('abc');
if (result === null) {
  console.log('Invalid input');
} else {
  console.log('Parsed:', result);
}
```

`null` makes the error case explicit without magic values.

### Encapsulate Conditionals

Conditionals embedded in code obscure intent. Extract them to named predicates to make the code read like a specification.

#### Bad (Java)

```java
public void shipOrder(Order order) {
    if (order.getStatus() == OrderStatus.PAID &&
        order.getShippingAddress() != null &&
        order.getItems().stream().allMatch(Item::isInStock)) {
        logisticsService.ship(order);
    }
}
```

The conditional bundles multiple checks into an opaque expression.

#### Good (Java)

```java
public void shipOrder(Order order) {
    if (order.isReadyForShipment()) {
        logisticsService.ship(order);
    }
}

public boolean isReadyForShipment(this Order order) {
    return order.getStatus() == OrderStatus.PAID
        && order.getShippingAddress() != null
        && order.getItems().stream().allMatch(Item::isInStock);
}
```

The predicate name `isReadyForShipment` makes the intent explicit.

#### Bad (TypeScript)

```typescript
function processUser(user: User) {
  if (
    user.accountStatus === 'active' &&
    user.emailVerified &&
    !user.isSuspended &&
    user.role !== 'banned'
  ) {
    // process user
  }
}
```

Multiple conditions mixed together obscure what combination of states triggers the block.

#### Good (TypeScript)

```typescript
function isEligibleForService(user: User): boolean {
  return (
    user.accountStatus === 'active' &&
    user.emailVerified &&
    !user.isSuspended &&
    user.role !== 'banned'
  );
}

function processUser(user: User) {
  if (isEligibleForService(user)) {
    // process user
  }
}
```

The predicate name captures the business rule.

### Structured Programming

While Dijkstra's structured programming theorem allows multiple return points, consistency improves readability. Prefer a single entry and single exit point for functions that contain complex control flow. Use guard clauses for early exits at the top of functions.

#### Bad (Java)

```java
public Money calculateDiscount(Order order, Customer customer) {
    Money discount = Money.ZERO;
    if (order != null) {  // Nested conditional
        if (customer != null) {
            if (customer.isVip()) {
                discount = order.getTotal().multiply(0.1);
            }
        }
    }
    return discount;
}
```

Deeply nested conditionals obscure the control flow.

#### Good (Java)

```java
public Money calculateDiscount(Order order, Customer customer) {
    if (order == null || customer == null) {
        return Money.ZERO;
    }
    if (!customer.isVip()) {
        return Money.ZERO;
    }
    return order.getTotal().multiply(0.1);
}
```

Guard clauses eliminate nesting. The happy path flows straight down.

#### Bad (TypeScript)

```typescript
function processPayment(payment: Payment): string {
  let result = 'failed';
  if (payment.method === 'credit_card') {
    if (payment.amount > 0) {
      if (validateCard(payment.card)) {
        result = 'success';
      }
    }
  }
  return result;
}
```

Nested conditionals make the success path hard to follow.

#### Good (TypeScript)

```typescript
function processPayment(payment: Payment): PaymentResult {
  if (payment.method !== 'credit_card') {
    return { status: 'failed', reason: 'Unsupported payment method' };
  }
  if (payment.amount <= 0) {
    return { status: 'failed', reason: 'Invalid amount' };
  }
  if (!validateCard(payment.card)) {
    return { status: 'failed', reason: 'Invalid card' };
  }
  return { status: 'success', transactionId: generateTransactionId() };
}
```

Early returns for each failure case. The success path is the final statement.

## Checklist

- [ ] Each function fits on a single screen (under 40 lines).
- [ ] Each function does one thing and does it well.
- [ ] Function names are verbs that describe the action.
- [ ] Parameter count is three or fewer; use parameter objects for more.
- [ ] Commands and queries are separated.
- [ ] Side effects are explicit and minimal.
- [ ] Exceptions are used for error conditions, not error codes.
- [ ] Complex conditionals are extracted to named predicates.
- [ ] Guard clauses eliminate nested conditionals.
- [ ] Functions are tested in isolation.

## Cross-References

- [`../code-quality.md`](./code-quality.md) - General code quality principles
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) - Domain layer function design
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) - Application layer function orchestration
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) - Infrastructure layer function patterns
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) - Interface layer function contracts
- [`../clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) - End-to-end function composition
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) - Strategic design decisions
