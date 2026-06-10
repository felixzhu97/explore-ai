---
name: software-testing
description: 软件测试方法论指南。涵盖 TDD 测试驱动开发、BDD 行为驱动开发、测试金字塔、单元测试、集成测试、E2E 测试策略与实践。
---

# 软件测试

## 测试策略概览

### 测试金字塔

```
                    ▲
                   /E\
                  /2E \      E2E Tests (Few, Slow, Expensive)
                 /-----\        - 跨系统完整流程
                / Inte \       - 关键用户路径
               /gration\         - 信任唯一来源
              /--------\
             /  Unit   \       Integration Tests
            /  Tests   \         - 组件协作
           /------------\         - 数据库、消息队列
          /              \       - 服务间交互
         ▼◄───────────────▼◄
              Trust Level
         ──────────────────────
         ▲                      ▲
        High                   Low
```

### 测试分层原则

| 层级 | 数量占比 | 速度 | 范围 | 信任度 |
|------|---------|------|------|--------|
| 单元测试 | 70% | < 1ms | 单个类/方法 | 高 |
| 集成测试 | 20% | < 100ms | 组件协作 | 中 |
| E2E 测试 | 10% | < 10s | 完整系统 | 低（假阳性高） |

## TDD 测试驱动开发

### 红-绿-重构循环

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│    ┌─────────┐      ┌─────────┐      ┌─────────┐               │
│    │   RED   │ ───► │  GREEN  │ ───► │ REFACTOR │               │
│    │         │      │         │      │         │               │
│    │ 写失败   │      │ 最小实现 │      │ 清理代码 │               │
│    │  测试    │      │  通过    │      │  保测试  │               │
│    └─────────┘      └─────────┘      └─────────┘               │
│         ▲                                      │               │
│         └──────────────────────────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### TDD 步骤详解

#### Step 1: RED（红）- 写失败测试

```java
// 业务需求：订单满100元免运费
class OrderShippingTest {

    @Test
    void shouldBeEligibleForFreeShippingWhenOrderExceeds100Dollars() {
        // Given: 一个金额为 120 元的订单
        Order order = new Order(List.of(new OrderLine(Product.of("Item", Money.of(120)), 1)));

        // When: 计算运费
        Money shippingFee = order.calculateShippingFee();

        // Then: 应该免运费
        assertThat(shippingFee).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldChargeShippingFeeWhenOrderBelow100Dollars() {
        // Given
        Order order = new Order(List.of(new OrderLine(Product.of("Item", Money.of(50)), 1)));

        // When
        Money shippingFee = order.calculateShippingFee();

        // Then
        assertThat(shippingFee).isEqualTo(Money.of(10));
    }
}
```

#### Step 2: GREEN（绿）- 最小实现

```java
// 最小实现，让测试通过
public class Order {
    public Money calculateShippingFee() {
        if (totalAmount().isGreaterThanOrEqual(Money.of(100))) {
            return Money.ZERO;
        }
        return Money.of(10);
    }
}
```

#### Step 3: REFACTOR（重构）- 清理代码

```java
// 重构后
public class Order {
    private static final Money FREE_SHIPPING_THRESHOLD = Money.of(100);
    private static final Money STANDARD_SHIPPING_FEE = Money.of(10);

    public Money calculateShippingFee() {
        return totalAmount().isGreaterThanOrEqual(FREE_SHIPPING_THRESHOLD)
            ? Money.ZERO
            : STANDARD_SHIPPING_FEE;
    }
}
```

### TDD 测试命名规范

```
should_预期行为_when_触发条件

Java:     shouldBeEligibleForFreeShippingWhenOrderExceeds100Dollars
Python:   should_be_eligible_for_free_shipping_when_order_exceeds_100_dollars
TypeScript: should_be_eligible_for_free_shipping_when_order_exceeds_100_dollars
```

### TDD 实践要点

- **测试顺序**：按业务价值排序，先测核心路径
- **最小实现**：只写让测试通过的代码，不多不少
- **快速反馈**：测试运行时间 < 1 秒
- **独立测试**：测试间无依赖，可并行运行
- **单一断言原则**：每个测试验证一个行为（可选）

## BDD 行为驱动开发

### Gherkin 语法

```gherkin
Feature: 订单免运费计算
  作为客户
  我希望订单满100元免运费
  以便降低购物成本

  Scenario: 订单金额超过100元，享受免运费
    Given 我的购物车有商品
      | 商品名称 | 单价 | 数量 |
      | 笔记本   | 5000 | 1   |
      | 鼠标     | 200  | 2   |
    When 我提交订单
    Then 运费应该为 0 元
    And 我应该看到提示 "订单金额超过100元，免运费"

  Scenario: 订单金额不足100元，收取运费
    Given 我的购物车有商品
      | 商品名称 | 单价 | 数量 |
      | 铅笔     | 5    | 3   |
    When 我提交订单
    Then 运费应该为 10 元
    And 我应该看到提示 "订单金额不足100元，收取运费"

  Scenario: 订单刚好100元，享受免运费
    Given 我的购物车有商品
      | 商品名称 | 单价 | 数量 |
      | 书       | 100  | 1   |
    When 我提交订单
    Then 运费应该为 0 元

  Scenario Outline: 不同金额段的运费计算
    Given 我的购物车商品总价为 <total>
    When 我提交订单
    Then 运费应该为 <shipping_fee>
    
    Examples:
      | total | shipping_fee |
      | 50    | 10          |
      | 100   | 0           |
      | 150   | 0           |
```

### BDD 实现映射

```java
// Step Definitions
public class OrderShippingSteps {

    private Order order;
    private Money shippingFee;
    private List<OrderLine> cartItems;

    @Given("我的购物车有商品")
    public void given_my_cart_has_items(DataTable dataTable) {
        cartItems = dataTable.asList(OrderLine.class);
        order = new Order(cartItems);
    }

    @When("我提交订单")
    public void when_i_submit_order() {
        shippingFee = order.calculateShippingFee();
    }

    @Then("运费应该为 {int} 元")
    public void then_shipping_fee_should_be(int expected) {
        assertThat(shippingFee).isEqualTo(Money.of(expected));
    }

    @And("我应该看到提示 {string}")
    public void and_i_should_see_message(String message) {
        assertThat(order.getLastMessage()).isEqualTo(message);
    }
}
```

### BDD 与 TDD 的关系

```
BDD (验收测试)
    │
    │ 指导
    ▼
TDD (单元测试)
    │
    │ 实现
    ▼
代码
```

- **BDD** 从外部视角定义系统行为（What）
- **TDD** 从内部视角实现功能（How）
- BDD 场景是 TDD 测试的需求来源

## 单元测试

### AAA 模式

```java
class OrderPricingTest {

    @Test
    @DisplayName("订单总金额应为所有行项之和")
    void shouldCalculateTotalAsSumOfLines() {
        // Arrange: 准备测试数据
        List<OrderLine> lines = List.of(
            createLine(Money.of(100), 2),  // 200
            createLine(Money.of(50), 3)    // 150
        );
        Order order = new Order(lines);

        // Act: 执行被测操作
        Money total = order.totalAmount();

        // Assert: 验证结果
        assertThat(total).isEqualTo(Money.of(350));
    }
}
```

### 测试替身（Test Doubles）

| 类型 | 用途 | 示例 |
|------|------|------|
| **Dummy** | 填充参数，不使用 | `new Order(null, null, dummy)` |
| **Fake** | 简化实现，不适合生产 | `InMemoryUserRepository` |
| **Stub** | 预设响应 | `when(repo.findById(1)).thenReturn(user)` |
| **Spy** | 记录调用，保留真实行为 | `Mockito.spy(list).add(1)` |
| **Mock** | 验证交互，验证行为 | `verify(repo).save(user)` |

```java
// Stub: 预设返回值
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void shouldCalculateOrderWithStubbedProducts() {
        // Given: Stub 预设产品不存在
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        // When
        Order order = orderService.createOrder(List.of(new OrderLineId("p1", 1)));

        // Then
        assertThat(order).isNull();
    }
}

// Mock: 验证交互
@Test
void shouldSaveOrderWhenPlaced() {
    // Given
    Order order = createValidOrder();
    when(orderRepository.save(any())).thenReturn(order);

    // When
    orderService.placeOrder(order);

    // Then: 验证 save 被调用
    verify(orderRepository).save(order);
    verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
}

// Spy: 记录调用但保留真实行为
@Test
void shouldLogWhenOrderPlaced() {
    // Given
    Order order = createValidOrder();
    Logger logger = spy(Logger.class);
    order.setLogger(logger);

    // When
    order.place();

    // Then
    verify(logger).info(contains("Order placed"));
}
```

### 边界条件测试

```java
class OrderEdgeCaseTest {

    @Test
    void shouldThrowWhenOrderHasNoLines() {
        Order order = new Order(List.of());
        assertThrows(OrderEmptyException.class, order::place);
    }

    @Test
    void shouldHandleLargeQuantity() {
        OrderLine line = new OrderLine(product(), 999_999_999);
        Order order = new Order(List.of(line));
        
        assertThatCode(order::place).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleZeroPriceProduct() {
        OrderLine line = new OrderLine(Product.free(), 1);
        Order order = new Order(List.of(line));
        
        assertThat(order.totalAmount()).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldHandleNegativeQuantity() {
        assertThrows(IllegalQuantityException.class, 
            () -> new OrderLine(product(), -1));
    }
}
```

### 测试数据构建器

```java
// Builder 模式简化测试数据构建
class OrderTestBuilder {
    private List<OrderLine> lines = new ArrayList<>();
    private Customer customer = CustomerBuilder.builder().build();
    private OrderStatus status = OrderStatus.DRAFT;

    public static OrderTestBuilder builder() {
        return new OrderTestBuilder();
    }

    public OrderTestBuilder withLine(Money price, int qty) {
        lines.add(new OrderLine(productWithPrice(price), qty));
        return this;
    }

    public OrderTestBuilder withCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public OrderTestBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public Order build() {
        Order order = new Order(customer, lines);
        if (status != OrderStatus.DRAFT) {
            order.setStatus(status);
        }
        return order;
    }

    private Product productWithPrice(Money price) {
        return new Product(ProductId.generate(), "Test", price);
    }
}

// 使用
@Test
void shouldCalculateDiscountForVipCustomers() {
    Customer vipCustomer = CustomerBuilder.builder()
        .withVipStatus(true)
        .build();

    Order order = OrderTestBuilder.builder()
        .withCustomer(vipCustomer)
        .withLine(Money.of(1000), 1)
        .build();

    Money discount = discountService.calculate(order);
    assertThat(discount).isEqualTo(Money.of(100)); // 10% VIP discount
}
```

## 集成测试

### 数据库集成测试

```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback(false)  // 查看测试数据时可设为 false
    void shouldPersistAndRetrieveOrder() {
        // Given
        Order order = OrderTestBuilder.builder()
            .withLine(Money.of(100), 2)
            .build();
        order.place();

        // When
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> found = orderRepository.findById(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().totalAmount()).isEqualTo(Money.of(200));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void shouldFindOrdersByCustomer() {
        // Given
        Customer customer = createCustomer("test@example.com");
        Order order1 = createOrder(customer);
        Order order2 = createOrder(customer);
        orderRepository.saveAll(List.of(order1, order2));

        // When
        Page<Order> orders = orderRepository.findByCustomer(customer.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(orders.getTotalElements()).isEqualTo(2);
    }
}
```

### API 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderAndReturn201() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            List.of(new OrderLineRequest("prod-1", 2))
        );

        // When/Then
        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.totalAmount").value(200.00));
    }

    @Test
    void shouldReturn400WhenOrderIsEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(List.of());

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("empty")));
    }
}
```

### 消息队列集成测试

```java
@Testcontainers
class OrderEventIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaMessageListenerContainer<String, OrderPlacedEvent> listener;

    @Test
    void shouldReceiveOrderPlacedEvent() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        OrderPlacedEvent receivedEvent = null;

        listener.setupMessageListener(event -> {
            receivedEvent = event;
            latch.countDown();
        });

        // When
        Order order = createPlacedOrder();
        kafkaTemplate.send("order-events", order.getId().toString(), 
            new OrderPlacedEvent(order.getId(), order.getTotalAmount()));

        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedEvent.orderId()).isEqualTo(order.getId());
    }
}
```

## E2E 测试

### Playwright 示例

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
    await expect(page.getByTestId('shipping-message')).toContainText('订单金额不足100元');
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

## 测试覆盖率

### 覆盖率目标

| 层级 | 覆盖类型 | 目标 |
|------|---------|------|
| 领域层 | 行覆盖率 (Line) | > 90% |
| 应用层 | 路径覆盖率 | > 80% |
| 基础设施 | 集成覆盖 | 关键路径 |
| 接口层 | Happy path | 100% |
| 接口层 | Error path | > 70% |

### 覆盖率陷阱

- **高覆盖率 ≠ 高质量**：虚假测试可以通过覆盖率检查
- **关注测试意图**：测试是否验证正确的行为
- **边界条件**：覆盖率报告应重点关注未覆盖的分支

## 测试数据管理

### 测试数据构建策略

```java
// 策略 1: Builder 模式
Order order = OrderBuilder.builder()
    .withCustomer(vipCustomer)
    .withLine(Money.of(100), 2)
    .withStatus(OrderStatus.PLACED)
    .build();

// 策略 2: Fixture 工厂
class TestFixtures {
    public static Customer vipCustomer() { ... }
    public static Customer regularCustomer() { ... }
    public static Order placedOrder(Customer customer) { ... }
}

// 策略 3: Test Data Generator
class OrderGenerator {
    public static Order randomOrder() {
        return OrderBuilder.builder()
            .withLine(randomPrice(), randomQuantity(1, 10))
            .build();
    }
}
```

### 测试数据库隔离

```java
// 每个测试方法使用独立事务
@SpringBootTest
@Transactional
class RepositoryTest {
    @Test
    void test1() {
        repository.save(entity);  // 仅在 test1 中可见
    }

    @Test
    void test2() {
        // 看不到 test1 的数据
        assertThat(repository.findAll()).isEmpty();
    }
}

// 或使用测试容器
@Testcontainers
class ContainerizedTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

## 测试评审清单

### 测试可读性

- [ ] 测试名称清晰表达意图
- [ ] Given/When/Then 结构清晰
- [ ] 无魔法数字（使用常量或变量）
- [ ] 断言消息有帮助

### 测试可靠性

- [ ] 测试是确定性的（无随机失败）
- [ ] 测试间无依赖
- [ ] 测试在本地和 CI 表现一致
- [ ] 测试清理了自己的状态

### 测试可维护性

- [ ] 使用 Test Builder 简化数据构建
- [ ] 测试数据集中管理（Fixtures）
- [ ] 避免重复的测试设置代码
- [ ] 测试代码与生产代码同等重视

### 测试覆盖

- [ ] 核心业务逻辑 100% 覆盖
- [ ] 边界条件有测试
- [ ] 异常路径有测试
- [ ] Happy path 和 Sad path 都有

## 测试反模式

| 反模式 | 表现 | 正确做法 |
|--------|------|----------|
| **测试实现而非行为** | `assertEquals(1, service.getCount())` | 测试业务价值 |
| **断言不足** | `assertTrue(result)` | 精确断言 |
| **过度模拟** | Mock 一切 | 用 Fake 或真实对象 |
| **测试私有方法** | `testPrivateMethod()` | 测试公共行为 |
| **慢测试** | 单元测试访问 DB | Mock 依赖 |
| **脆弱测试** | 测试细节易变 | 测试意图 |
| **注释掉的测试** | 被跳过的测试 | 删除或修复 |
