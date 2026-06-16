# Clean Code: Comments

Comments are a liability, not an asset. The best comment is the code itself, written so clearly that no explanation is needed. When comments are necessary, they should explain why something is done, not what is done. Outdated comments are worse than no comments because they mislead future readers.

## Why It Matters

Code tells you how; comments should tell you why. The decisions behind a piece of code are not always apparent from reading it. Business rules, regulatory constraints, performance trade-offs, and historical context are all reasons that might warrant a comment. However, comments that restate what the code already says become noise that obscures the signal.

The maintenance cost of comments is often underestimated. A comment written today reflects the understanding of the author at that moment. As the codebase evolves, comments drift from the code they describe. Stale comments are actively harmful because they erode trust in all comments.

## Smell Catalog

### Good vs Bad Comments

Good comments capture intent, explain trade-offs, or warn about consequences. Bad comments restate the obvious, apologize for the code, or explain what the code does line by line.

#### Bad (Java)

```java
// Increment counter
counter++;

// Check if user is null
if (user != null) {
    // Process user
    processUser(user);
}
```

These comments add no value. The code is self-explanatory.

#### Good (Java)

```java
// Regulatory requirement: retain order history for 7 years per GDPR Article 17
// even after the order is cancelled or deleted.
private static final int ORDER_RETENTION_YEARS = 7;
```

This comment explains a business constraint that is not obvious from the code.

#### Bad (TypeScript)

```typescript
// Check if user is logged in
if (user.isLoggedIn) {
  // Call API
  const response = await api.getData();
}
```

The code already says this.

#### Good (TypeScript)

```typescript
// Rate limit: downstream service allows 100 requests/minute.
// Throttle bursts to prevent 429 responses.
const THROTTLE_MS = 600;
```

This comment explains a constraint that justifies the implementation.

### JavaDoc and TSDoc

Formal documentation comments (JavaDoc in Java, JSDoc/TSDoc in TypeScript) serve a different purpose than inline comments. They document the public API surface: classes, interfaces, and public methods. Keep them accurate; remove them when they become stale.

#### Bad (Java)

```java
/**
 * Processes an order.
 *
 * @param order The order to process
 */
public void processOrder(Order order) {
    // ...
}
```

"Processes an order" is too vague to be useful. What does "process" mean?

#### Good (Java)

```java
/**
 * Cancels an order and initiates the refund workflow.
 *
 * <p>Cancellation is only permitted for orders in DRAFT or PLACED status.
 * Orders that have already shipped cannot be cancelled through this method;
 * use {@link #requestReturn()} instead.</p>
 *
 * @param orderId the identifier of the order to cancel
 * @param reason the cancellation reason, recorded for audit purposes
 * @throws OrderNotFoundException if no order exists with the given ID
 * @throws OrderCancellationException if the order is not in a cancellable state
 */
public void cancelOrder(OrderId orderId, String reason) {
    // ...
}
```

The JavaDoc explains when to use this method, what preconditions apply, and what exceptions can be thrown.

#### Bad (TypeScript)

```typescript
/**
 * Validates a string
 * @param str - string to validate
 * @returns true if valid
 */
function validate(str: string): boolean {
  return str.length > 0;
}
```

"Validates a string" is too vague. "What constitutes validity?" is the unasked question.

#### Good (TypeScript)

```typescript
/**
 * Validates that an email address conforms to RFC 5322 format.
 *
 * Does NOT verify that the domain has an MX record.
 * For MX verification, use {@link verifyEmailDomain}.
 *
 * @param email - the email address string to validate
 * @returns true if the email format is structurally valid
 *
 * @example
 * validateEmail('user@example.com') // true
 * validateEmail('invalid')           // false
 */
function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
```

The TSDoc explains what the function does, what it does not do, and provides examples.

### Legal Comments

Some comments are required by law or contract. These should be preserved and kept accurate. Place them in a standard location, such as the file header, rather than scattered throughout the code.

#### Bad (Java)

```java
// Copyright (c) Acme Corp 2024  // <- scattered throughout file
public class OrderService {
    // Copyright (c) Acme Corp 2024
    public void process() { }
}
```

Copyright notices scattered throughout the file are hard to maintain.

#### Good (Java)

```java
/*
 * Copyright (c) 2024 Acme Corporation. All rights reserved.
 * This software is the confidential and proprietary information of Acme Corporation.
 * Unauthorized reproduction or distribution is prohibited.
 */
package com.acme.orderservice;

public class OrderService {
    // ...
}
```

A single copyright header at the top of each source file.

#### Bad (TypeScript)

```typescript
// (c) Acme Corp
const API_KEY = 'xxx'; // (c) Acme Corp
const ENDPOINT = '/api'; // (c) Acme Corp
```

Repeated copyright notices clutter the file.

#### Good (TypeScript)

```typescript
/**
 * @license
 * Copyright (c) 2024 Acme Corporation. All rights reserved.
 * https://www.acmecorp.com/legal
 */

const API_KEY = process.env.ACME_API_KEY;
const ENDPOINT = process.env.ACME_API_ENDPOINT;
```

A single license comment at the top. Secrets are loaded from environment variables.

### TODO Policy

TODOs are markers for work that needs to be done but cannot be done now. Every TODO should have a date and an owner so it can be tracked and eventually addressed. Avoid TODOs that persist for months without action.

#### Bad (Java)

```java
// TODO: fix this later
public String formatDate(Date date) {
    return date.toString();
}
```

"TODO: fix this later" has no owner, no date, and no context about what needs fixing.

#### Good (Java)

```java
// TODO(2024-06-15, alice): Remove this workaround after upgrading to
// PaymentGateway v3.0, which natively supports partial refunds.
// Tracking ticket: PAY-1234
public Money calculatePartialRefund(Order order) {
    // Workaround: simulate partial refund by calculating difference
    // Remove this entire method when gateway supports partial refunds.
    return order.getTotal().subtract(order.getRefundedAmount());
}
```

The TODO has a date, an owner, an explanation of what the work involves, and a tracking reference.

#### Bad (TypeScript)

```typescript
// TODO: refactor this
async function fetchData() {
  return await api.get('/data');
}
```

No context about what needs refactoring.

#### Good (TypeScript)

```typescript
// TODO(2024-06-20, @alice): Replace this mock with actual OAuth2 token refresh
// once the auth service exposes a refresh endpoint. See JIRA ticket AUTH-89.
async function fetchData(): Promise<Data> {
  return await api.get('/data');
}
```

The TODO explains the scope of the work and links to the tracking system.

### Explain WHY Not WHAT

Comments should explain the reasoning behind a decision, not the mechanics of the implementation. The code shows what happens; the comment explains why that approach was chosen.

#### Bad (Java)

```java
// Create a new ArrayList
List<String> names = new ArrayList<>();

// Loop through the users
for (User user : users) {
    // Add the user name to the list
    names.add(user.getName());
}
```

The comments restate what the code does.

#### Good (Java)

```java
// Use ArrayList instead of List.of() because we need to add
// additional fields from the join table that are not part of User.
List<String> names = new ArrayList<>();
```

This comment explains why a mutable list was chosen, which is not obvious from the code alone.

#### Bad (TypeScript)

```typescript
// Create a new array
const items = [];

// Loop through the products
for (const product of products) {
  // Add active products to the array
  if (product.isActive) {
    items.push(product);
  }
}
```

These comments describe the mechanics without adding information.

#### Good (TypeScript)

```typescript
// Filter in JavaScript rather than SQL because the filtering logic
// depends on a client-side feature flag that is not available in the query layer.
// This is a temporary solution until the feature flag is promoted to a
// database column. See: FEATURE-456
const items = products.filter((product) => product.isActive);
```

This comment explains the business reason for the implementation choice.

### Remove Commented-Out Code

Commented-out code is dead code that accumulates over time. It confuses readers, pollutes diffs, and provides no value. Use version control to preserve old code; remove it from the source file.

#### Bad (Java)

```java
public void process(Order order) {
    // old implementation
    // order.setStatus(OrderStatus.PENDING);
    // orderRepository.save(order);
    // emailService.send(order.getCustomer(), "Order received");

    order.activate();
    orderRepository.save(order);
}
```

Commented-out code leaves questions about whether it should be restored.

#### Good (Java)

```java
public void process(Order order) {
    order.activate();
    orderRepository.save(order);
}
```

The current implementation is present. History is in version control.

#### Bad (TypeScript)

```typescript
async function submitOrder(order: Order) {
  // await analytics.track('order:submit', order);
  // await syncInventory(order);
  await saveOrder(order);
  await sendConfirmationEmail(order);
}
```

Dead code creates noise and uncertainty.

#### Good (TypeScript)

```typescript
async function submitOrder(order: Order): Promise<void> {
  await saveOrder(order);
  await sendConfirmationEmail(order);
}
```

Active code only. Use git blame to recover removed code if needed.

### Restating-the-Obvious Comments

Comments that say nothing beyond what the code expresses are noise. They increase reading time without adding information. Delete them or replace them with comments that explain why.

#### Bad (Java)

```java
// The following loop iterates over all customers
for (Customer customer : customers) {
    // If the customer is active
    if (customer.isActive()) {
        // Send them a notification
        notificationService.send(customer, message);
    }
}
```

Every comment restates the obvious.

#### Good (Java)

```java
// Notify only active customers to comply with CAN-SPAM regulations.
// Inactive accounts have had email disabled by user choice.
for (Customer customer : customers) {
    if (customer.isActive()) {
        notificationService.send(customer, message);
    }
}
```

The comment explains a business constraint that justifies the filter.

#### Bad (TypeScript)

```typescript
// Get the user ID from the session
const userId = session.user.id;

// If the user ID exists
if (userId) {
  // Load the user profile
  const profile = await loadProfile(userId);
}
```

The comments are redundant.

#### Good (TypeScript)

```typescript
// Profile is loaded eagerly here because the subsequent UI rendering
// requires the full profile object. Lazy loading would cause a layout shift.
const userId = session.user.id;
if (userId) {
  const profile = await loadProfile(userId);
}
```

The comment explains a non-obvious performance consideration.

### Noise Comments

Noise comments are formulaic phrases that add clutter without value. They often appear from habit or auto-generated templates. Remove them.

#### Bad (Java)

```java
/**
 * Default constructor.
 */
public Order() {
}

/**
 * Gets the order ID.
 *
 * @return the order ID
 */
public OrderId getOrderId() {
    return orderId;
}
```

These comments provide no information beyond what the method signature conveys.

#### Good (Java)

```java
/**
 * Creates an order in DRAFT status for the given customer.
 *
 * @param customerId the customer placing the order
 * @return a new Order instance in DRAFT status
 */
public static Order createDraftOrder(CustomerId customerId) {
    // ...
}
```

The comment explains what the factory method does and the preconditions.

#### Bad (TypeScript)

```typescript
/**
 * Default value
 */
const DEFAULT_TIMEOUT = 5000;

/**
 * The name
 */
const USER_NAME = 'Guest';
```

Noise comments that say nothing useful.

#### Good (TypeScript)

```typescript
/**
 * Connection timeout for downstream service calls.
 * Matches the SLA requirement of 99.9% availability.
 */
const DEFAULT_TIMEOUT_MS = 5000;

/**
 * Placeholder name for unauthenticated users.
 */
const GUEST_NAME = 'Guest';
```

Each comment explains why the constant exists.

## Checklist

- [ ] Every comment explains why, not what.
- [ ] JavaDoc is accurate and complete for all public API elements.
- [ ] TSDoc is accurate for all exported functions.
- [ ] Legal comments are in file headers, not scattered throughout.
- [ ] TODO comments have dates, owners, and tracking references.
- [ ] Commented-out code is removed; history is in version control.
- [ ] Redundant comments are deleted.
- [ ] Noise comments are replaced with meaningful explanations.
- [ ] Comments are reviewed during code review for accuracy.

## Cross-References

- [`../code-quality.md`](./code-quality.md) - General code quality principles
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) - Domain layer documentation standards
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) - Application layer documentation
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) - Infrastructure layer documentation
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) - Interface layer documentation
- [`../clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) - End-to-end documentation practices
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) - Strategic design decisions
