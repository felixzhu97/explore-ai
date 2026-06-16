# E2E Testing

End-to-end tests that verify complete user flows through the entire application stack, from UI through backend to database.

## When to Use

Use E2E tests sparingly for the most critical user journeys — login, checkout, core business workflows. E2E tests are slow, expensive, and prone to flakiness, so they should cover only paths where the value of testing the full stack outweighs the cost. Prefer unit and integration tests for everything else, reserving E2E for validating that all components wire together correctly.

## Core Idea

### Playwright Example

```typescript
// e2e/orders.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Order Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('login-btn').click();
    await page.getByLabel('Email').fill('customer@test.com');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
  });

  test('should complete order with free shipping', async ({ page }) => {
    // Given
    await page.getByTestId('product-1').click();
    await page.getByTestId('add-to-cart').click();
    await page.getByTestId('cart-total').waitFor();

    // Verify cart shows correct total
    await expect(page.getByTestId('cart-total')).toHaveText('$5,200');

    // When
    await page.getByTestId('checkout-btn').click();
    await page.getByTestId('shipping-select').selectOption('standard');
    await page.getByTestId('place-order-btn').click();

    // Then
    await expect(page.getByTestId('order-success')).toBeVisible();
    await expect(page.getByTestId('order-total')).toHaveText('$5,200'); // Free shipping
    await expect(page.getByTestId('shipping-fee')).toHaveText('$0.00');
  });

  test('should show shipping fee for orders under $100', async ({ page }) => {
    // Given - Add cheap item
    await page.getByTestId('product-pencil').click();
    await page.getByTestId('add-to-cart').click();

    // When
    await page.getByTestId('checkout-btn').click();

    // Then
    await expect(page.getByTestId('shipping-fee')).toHaveText('$10.00');
    await expect(page.getByTestId('shipping-message')).toContainText('Order amount below 100 yuan');
  });

  test('should handle payment failure gracefully', async ({ page }) => {
    // Given
    await addExpensiveItemToCart(page);
    await page.getByTestId('checkout-btn').click();

    // Enter invalid card
    await page.getByTestId('card-number').fill('4000000000000002'); // Stripe test failure card
    await page.getByTestId('expiry').fill('12/30');
    await page.getByTestId('cvc').fill('123');

    // When
    await page.getByTestId('place-order-btn').click();

    // Then
    await expect(page.getByTestId('payment-error')).toBeVisible();
    await expect(page.getByTestId('payment-error')).toContainText('Card declined');
  });
});
```

## Bad/Good Examples

### Good: Critical Path Only

```typescript
// Only test the most important user journeys
test.describe('E2E Critical Paths', () => {
  test('should allow customer to place order end-to-end');
  test('should allow admin to approve order');
  test('should send email notification on order placement');
});
```

- Few tests, high confidence
- Clear business value per test

### Bad: Over-Abundant E2E Tests

```typescript
// ❌ Testing every UI state with E2E
test('should show empty cart message when cart is empty');
test('should show correct product name');
test('should enable button when form is valid');
// ... 200 more similar tests
```

- Hundreds of tests slow down CI significantly
- Many of these can be covered by unit/integration tests
- Flaky selectors cause constant failures

## Real Implementation Reference

- `apps/admin-ui/e2e/`
- `apps/web/e2e/`
- `apps/portal/e2e/`

## Related References

- [Testing Strategy Overview](./testing-strategy-overview.md)
- [Unit Testing](./unit-testing.md)
- [Integration Testing](./integration-testing.md)
- [BDD Behavior-Driven Development](./bdd-behavior-driven-development.md)
- [Software Testing](../SKILL.md)
