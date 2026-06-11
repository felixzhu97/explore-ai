---
name: java-clean-architecture
description: Java 24+ Clean Architecture 重构指南。包含领域建模、用例设计、依赖倒置、架构验证。用于将现有 Java 代码重构为 Clean Architecture 架构。
---

> **前置规则**: 本 skill 基于 `.cursor/rules/java-clean-architecture.mdc` 中定义的规则执行重构

---

# Java Clean Architecture 重构指南

## 重构流程

```
1. 识别领域概念 → 提取 Domain 层
2. 识别业务规则 → 移动到实体方法
3. 识别用例流程 → 创建 Application 层
4. 识别持久化逻辑 → 创建 Infrastructure 层
5. 识别请求处理 → 创建 Interface 层
6. 验证架构边界 → 确保依赖方向正确
```

---

## 重构阶段

### 阶段 1: 提取 Domain 层

**目标**: 创建核心业务模型，无任何外部依赖

#### 步骤 1.1: 创建目录结构

```
src/main/java/com/ai/domain/
├── model/           # 聚合根、实体
├── value/          # 值对象
├── service/        # 领域服务
├── repository/     # 仓储接口
├── event/          # 领域事件
└── exception/      # 领域异常
```

#### 步骤 1.2: 提取实体

从现有 `entity`、`model` 包中提取：

1. 移除所有 JPA/Spring 注解
2. 将 `javax.persistence.*` 改为普通字段
3. 将业务逻辑从 Service 移到实体方法
4. 移除 public setters，改为业务方法
5. 创建工厂方法替代构造函数

**示例转换**:

```java
// ❌ 重构前: 贫血模型 + 业务逻辑在 Service
@Entity
public class Order {
    @Id
    private UUID id;
    private UUID customerId;
    private String status;
    private List<OrderLine> lines;

    // Getter/Setter 为主
}

// Service 中
public class OrderService {
    public void placeOrder(Order order) {
        if (order.getLines().isEmpty()) {
            throw new EmptyOrderException();
        }
        order.setStatus("PLACED");
    }
}

// ✅ 重构后: 充血模型
public class Order extends AggregateRoot {
    private final UUID id;
    private OrderStatus status;
    private final List<OrderLine> lines;

    private Order(UUID id) {
        this.id = id;
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }

    public static Order create() {
        return new Order(UUID.randomUUID());
    }

    public void place() {
        if (lines.isEmpty()) {
            throw OrderException.empty();
        }
        this.status = OrderStatus.PLACED;
        addDomainEvent(new OrderPlacedEvent(this.id));
    }
}
```

#### 步骤 1.3: 提取值对象

将可变的属性类改为不可变值对象：

```java
// ✅ 值对象使用 Java Record
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

#### 步骤 1.4: 定义仓储接口

在 `domain/repository/` 中定义接口：

```java
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
    void delete(OrderId id);
}
```

---

### 阶段 2: 创建 Application 层

**目标**: 用例编排，不包含业务逻辑

#### 步骤 2.1: 创建目录结构

```
src/main/java/com/ai/application/
├── usecase/        # 用例
├── dto/            # 命令和结果
└── port/           # 端口接口
```

#### 步骤 2.2: 创建用例

将 Service 中的业务方法重构成用例：

```java
public class PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    public PlaceOrderUseCase(OrderRepository orderRepository, EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public OrderResult execute(PlaceOrderCommand command) {
        // 1. 获取订单
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // 2.执行业务逻辑 (在 Domain 中)
        order.place();

        // 3. 保存
        orderRepository.save(order);

        // 4. 发布事件
        eventPublisher.publish(order.pullDomainEvents());

        // 5. 返回结果
        return OrderResult.from(order);
    }
}
```

---

### 阶段 3: 创建 Infrastructure 层

**目标**: 实现 Application 层定义的接口

#### 步骤 3.1: 创建目录结构

```
src/main/java/com/ai/infrastructure/
├── persistence/
│   ├── jpa/           # JPA 实体
│   └── repository/    # 仓储实现
├── external/          # 外部服务适配器
└── config/
```

#### 步骤 3.2: 创建 JPA 实体

```java
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private UUID id;
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    private String status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLineEntity> lines;

    // Domain 转换方法
    public Order toDomain() {
        // 转换逻辑
    }

    public static OrderEntity fromDomain(Order order) {
        // 转换逻辑
    }
}
```

#### 步骤 3.3: 实现仓储

```java
@Repository
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springRepository;
    private final OrderEntityMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public void save(Order order) {
        springRepository.save(mapper.toEntity(order));
    }
}
```

---

### 阶段 4: 创建 Interface 层

**目标**: 处理 HTTP 请求，DTO 转换

#### 步骤 4.1: 创建目录结构

```
src/main/java/com/ai/interface/
├── controller/        # REST 控制器
├── dto/
│   ├── request/       # 请求 DTO
│   └── response/      # 响应 DTO
├── mapper/            # 转换器
└── advice/            # 异常处理
```

#### 步骤 4.2: 创建 Controller

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;

    @PostMapping("/{id}/place")
    public ResponseEntity<OrderResponse> place(@PathVariable UUID id) {
        var command = new PlaceOrderCommand(OrderId.of(id));
        var result = placeOrderUseCase.execute(command);
        return ResponseEntity.ok(OrderResponse.from(result));
    }
}
```

---

## 重构模式

### 模式 1: 贫血模型 → 充血模型

**识别特征**:
- Service 类包含大量业务逻辑
- Entity 只有 getter/setter
- 多个 Service 相互调用

**重构步骤**:
1. 识别核心实体
2. 提取业务规则到实体方法
3. 用业务方法替换 Service 中的逻辑调用
4. 删除冗余 Service

### 模式 2: 事务脚本 → 领域模型

**识别特征**:
- 一个方法处理完整业务流程
- 大量参数传递
- 难以测试

**重构步骤**:
1. 识别业务概念和规则
2. 创建领域模型
3. 将步骤分解到模型方法
4. 用例负责协调

### 模式 3: 领域模型拆分聚合

**识别特征**:
- 单个类包含多个概念
- 对象间引用复杂
- 修改影响范围大

**重构步骤**:
1. 识别聚合边界
2. 提取值对象
3. 创建聚合根
4. 用 ID 替代直接引用

---

## 架构验证

### 依赖检查

```bash
# Domain 不依赖任何外部
grep -r "org.springframework" src/domain/ || echo "OK"
grep -r "jakarta.persistence" src/domain/ || echo "OK"
grep -r "javax.persistence" src/domain/ || echo "OK"

# 依赖方向正确
grep -r "domain.*import.*application" src/ || echo "OK"
grep -r "domain.*import.*infrastructure" src/ || echo "OK"
```

### 测试策略

```java
// 1. Domain 测试 (单元测试，无 mock)
class OrderTest {
    @Test
    void shouldThrowWhenPlacingEmptyOrder() {
        Order order = Order.create();
        assertThatThrownBy(order::place)
            .isInstanceOf(OrderException.class)
            .hasMessageContaining("empty");
    }
}

// 2. UseCase 测试 (单元测试，mock 依赖)
class PlaceOrderUseCaseTest {
    @Mock OrderRepository orderRepository;
    @Mock EventPublisher eventPublisher;

    @Test
    void shouldPublishEventAfterPlacing() {
        // Given
        Order order = Order.create();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        // When
        useCase.execute(new PlaceOrderCommand(order.getId()));

        // Then
        verify(eventPublisher).publish(any());
    }
}
```

---

## 常见问题

### Q: 现有 JPA 实体如何处理？

A: 保留 JPA 实体在 Infrastructure 层，创建独立的 Domain 实体，通过 Mapper 转换。

### Q: 如何处理遗留数据库表？

A: JPA 实体映射表结构，Domain 实体表达业务概念，通过 Mapper 隔离。

### Q: 现有 Service 如何迁移？

A:
- 业务逻辑 → 移到 Domain 实体
- 编排逻辑 → 移到 UseCase
- 基础设施调用 → 移到 Infrastructure

### Q: 测试怎么写？

A:
- Domain: 纯单元测试
- UseCase: Mock 依赖的 Repository/Port
- Controller: MockMvc 集成测试

---

## 重构清单

### 启动前

- [ ] 备份现有代码
- [ ] 确保有测试覆盖
- [ ] 定义重构范围

### 过程中

- [ ] 每次重构一小步
- [ ] 重构后运行测试
- [ ] 验证架构边界

### 完成后

- [ ] 所有测试通过
- [ ] 架构检查通过
- [ ] 文档已更新
