# BDD Behavior-Driven Development

A collaboration technique that bridges the gap between business language and technical implementation through structured, human-readable scenarios.

## When to Use

Use BDD when defining requirements collaboratively with product owners, business analysts, or domain experts. BDD is most effective at the start of a feature or user story, where it serves as executable specification and shared understanding. It is also valuable when writing acceptance tests that verify end-to-end behavior from the user's perspective.

## Core Idea

### Gherkin Syntax

```gherkin
Feature: Order Free Shipping Calculation
  As a customer
  I want free shipping on orders over 100 yuan
  So that I can reduce shopping costs

  Scenario: Order amount exceeds 100 yuan, enjoy free shipping
    Given my shopping cart has items
      | Item Name | Unit Price | Quantity |
      | Laptop    | 5000       | 1        |
      | Mouse     | 200        | 2        |
    When I submit the order
    Then shipping fee should be 0 yuan
    And I should see message "Order amount exceeds 100 yuan, free shipping"

  Scenario: Order amount below 100 yuan, shipping fee charged
    Given my shopping cart has items
      | Item Name | Unit Price | Quantity |
      | Pencil    | 5          | 3        |
    When I submit the order
    Then shipping fee should be 10 yuan
    And I should see message "Order amount below 100 yuan, shipping fee charged"

  Scenario: Order exactly 100 yuan, enjoy free shipping
    Given my shopping cart has items
      | Item Name | Unit Price | Quantity |
      | Book      | 100        | 1        |
    When I submit the order
    Then shipping fee should be 0 yuan

  Scenario Outline: Shipping fee calculation for different amounts
    Given my shopping cart total is <total>
    When I submit the order
    Then shipping fee should be <shipping_fee>

    Examples:
      | total | shipping_fee |
      | 50    | 10          |
      | 100   | 0           |
      | 150   | 0           |
```

### BDD Implementation Mapping

```java
// Step Definitions
public class OrderShippingSteps {

    private Order order;
    private Money shippingFee;
    private List<OrderLine> cartItems;

    @Given("my shopping cart has items")
    public void given_my_cart_has_items(DataTable dataTable) {
        cartItems = dataTable.asList(OrderLine.class);
        order = new Order(cartItems);
    }

    @When("I submit the order")
    public void when_i_submit_order() {
        shippingFee = order.calculateShippingFee();
    }

    @Then("shipping fee should be {int} yuan")
    public void then_shipping_fee_should_be(int expected) {
        assertThat(shippingFee).isEqualTo(Money.of(expected));
    }

    @And("I should see message {string}")
    public void and_i_should_see_message(String message) {
        assertThat(order.getLastMessage()).isEqualTo(message);
    }
}
```

### Relationship Between BDD and TDD

```
BDD (Acceptance Tests)
    │
    │ Guides
    ▼
TDD (Unit Tests)
    │
    │ Implements
    ▼
Code
```

- **BDD** defines system behavior from an external perspective (What)
- **TDD** implements functionality from an internal perspective (How)
- BDD scenarios are the source of requirements for TDD tests

## Bad/Good Examples

### Good: BDD Scenario as Executable Specification

```gherkin
Feature: VIP Member Discount

  Scenario: VIP member enjoys 10% discount
    Given customer Zhang Wei is a VIP member
    And shopping cart total is 1000 yuan
    When checkout is completed
    Then actual payment amount should be 900 yuan
```

```java
// Step definitions implement the BDD contract
@Then("actual payment amount should be {int} yuan")
public void then_actual_payment_should_be(int expected) {
    assertThat(order.getTotalPaid()).isEqualTo(Money.of(expected));
}
```

### Bad: Implementation Leakage in Scenarios

```gherkin
# ❌ Exposes implementation details
Scenario: Call discount service to calculate 10% off
  Given customer is VIP
  When discountService.calculate() is called
  Then system returns 0.9 multiplier
```

- Scenarios should describe behavior, not implementation
- Business stakeholders cannot understand "discountService.calculate()"

## Real Implementation Reference

- BDD feature files: `apps/server/src/test/resources/features/`
- Step definitions: `apps/server/src/test/java/com/ai/steps/`

## Related References

- [Testing Strategy Overview](./testing-strategy-overview.md)
- [TDD Test-Driven Development](./tdd-test-driven-development.md)
- [E2E Testing](./e2e-testing.md)
- [Unit Testing](./unit-testing.md)
- [Software Testing](../SKILL.md)
