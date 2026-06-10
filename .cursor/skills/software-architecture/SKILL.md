---
name: software-architecture
description: 软件架构设计方法论指南。涵盖 Clean Architecture、DDD 限界上下文、充血模型设计、六边形架构、事件驱动架构、微服务设计模式。
---

# 软件架构

## 架构设计原则

### SOLID 原则

| 原则 | 说明 | 违反表现 |
|------|------|----------|
| **S**ingle Responsibility | 单一职责，类只有一个变化原因 | 一个类做太多事情 |
| **O**pen/Closed | 开闭原则，对扩展开放，对修改关闭 | 修改现有代码添加新功能 |
| **L**iskov Substitution | 里氏替换，子类可替换父类 | instanceof 检查、强制类型转换 |
| **I**nterface Segregation | 接口隔离，小而专注的接口 | 胖接口、强迫实现不需要的方法 |
| **D**ependency Inversion | 依赖倒置，依赖抽象而非具体 | 直接依赖具体类 |

### 架构腐化警示

- 循环依赖：模块 A → B → C → A
- 散弹式修改：改一个功能要改多个类
- 依恋情节：类花更多时间计算其他类数据
- 冗余重复：重复代码散布各处
- 过早抽象：YAGNI 违背，添加不必要的间接层

## Clean Architecture（整洁架构）

### 层次模型

```
┌─────────────────────────────────────────────────────────┐
│                    Frameworks & Drivers                  │
│         Web框架、ORM、UI框架、数据库、外部服务           │
├─────────────────────────────────────────────────────────┤
│                   Interface Adapters                     │
│      Controllers, Gateways, Presenters, Mappers         │
├─────────────────────────────────────────────────────────┤
│                    Application Layer                     │
│            Use Cases, Application Services               │
│              Commands, Queries, Handlers                  │
├─────────────────────────────────────────────────────────┤
│                      Domain Layer                        │
│    Entities, Value Objects, Aggregates, Domain Events    │
│           Domain Services, Repository Interfaces          │
│                    (无外部依赖)                          │
└─────────────────────────────────────────────────────────┘
         ↑ 依赖只能向内，外层依赖内层，内层不知外层
```

### 依赖规则

1. **领域层是核心**：不依赖任何外部框架、库、基础设施
2. **依赖方向**：外层可以依赖内层，内层绝不知道外层存在
3. **接口定义位置**：依赖方定义接口（被依赖方实现）
4. **数据格式**：每层使用自己的数据格式，不直接传递外层格式

### 项目结构示例（Java）

```
src/main/java/com/ai/
├── domain/                    # 领域层（核心，无外部依赖）
│   ├── model/
│   │   ├── entity/           # 实体
│   │   │   └── Order.java
│   │   ├── vo/               # 值对象
│   │   │   ├── Money.java
│   │   │   └── Email.java
│   │   ├── aggregate/        # 聚合
│   │   │   └── OrderAggregate.java
│   │   ├── event/           # 领域事件
│   │   │   └── OrderPlacedEvent.java
│   │   └── service/         # 领域服务
│   │       └── PricingService.java
│   └── repository/          # 仓储接口
│       └── OrderRepository.java
│
├── application/              # 应用层
│   ├── command/             # 命令处理
│   │   └── placeorder/
│   │       ├── PlaceOrderCommand.java
│   │       └── PlaceOrderHandler.java
│   ├── query/               # 查询处理
│   │   └── getorder/
│   │       ├── GetOrderQuery.java
│   │       └── GetOrderHandler.java
│   └── service/             # 应用服务
│       └── OrderApplicationService.java
│
├── infrastructure/           # 基础设施层
│   ├── persistence/         # 持久化实现
│   │   └── jpa/
│   │       ├── OrderRepositoryImpl.java
│   │       └── OrderJpaEntity.java
│   ├── messaging/           # 消息实现
│   │   └── OrderEventPublisher.java
│   └── external/           # 外部服务适配器
│       └── PaymentGatewayAdapter.java
│
└── interface/               # 接口适配器层
    └── api/
        ├── controller/
        │   └── OrderController.java
        └── dto/
            ├── request/
            └── response/
```

## DDD 领域驱动设计

### 战略设计

#### 限界上下文（Bounded Context）

限界上下文是语义边界的显式边界，每个上下文有自己的：

- **通用语言**：团队共享的术语和含义
- **领域模型**：只属于该上下文的概念
- **边界**：明确什么在里面，什么在外面

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   订单上下文     │    │   库存上下文     │    │   支付上下文     │
│                 │    │                 │    │                 │
│  - Order        │◄──►│  - Inventory    │◄──►│  - Payment      │
│  - OrderItem    │    │  - Stock        │    │  - Transaction  │
│  - Pricing      │    │  - Warehouse    │    │  - Gateway     │
│                 │    │                 │    │                 │
│  团队: 订单团队  │    │  团队: 仓储团队  │    │  团队: 支付团队  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### 核心域 / 支撑域 / 通用域

| 类型 | 说明 | 投入 |
|------|------|------|
| **Core Domain** | 核心竞争力，唯一价值所在 | 最大投入，精雕细琢 |
| **Supporting Domain** | 支撑核心域，需要定制开发 | 适度投入 |
| **Generic Domain** | 通用解决方案，可直接购买 | 尽量少投入 |

#### 上下文映射

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Customer   │      │    Order    │      │   Fulfill   │
│   Context   │      │   Context   │      │   Context   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │ conformist          │ upstream/downstream│ conformist
       └────────────────────┴────────────────────┘
```

- **Shared Kernel**：共享领域模型的子集
- **Customer/Supplier**：上下游关系
- **Conformist**：下游完全服从上游模型
- **Anticorruption Layer**：转换层隔离不同模型

### 战术设计

#### 实体（Entity）

有唯一标识，生命周期可延续的对象。

```java
// 充血模型：行为在实体内部
public class Order extends AggregateRoot {
    private OrderId id;           // 唯一标识
    private CustomerId customerId;
    private List<OrderLine> lines;
    private OrderStatus status;
    private Money totalAmount;

    // 工厂方法创建订单
    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        Order order = new Order();
        order.id = OrderId.generate();
        order.customerId = customerId;
        order.lines = new ArrayList<>(lines);
        order.status = OrderStatus.DRAFT;
        order.totalAmount = calculateTotal(lines);
        return order;
    }

    // 业务行为：放置订单
    public void place() {
        if (status != OrderStatus.DRAFT) {
            throw new OrderInvalidStateException("Only draft order can be placed");
        }
        if (lines.isEmpty()) {
            throw new OrderEmptyException("Order must have at least one line");
        }
        status = OrderStatus.PLACED;
        // 发布领域事件
        addDomainEvent(new OrderPlacedEvent(this));
    }

    // 业务行为：取消订单
    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new OrderCannotBeCancelledException();
        }
        status = OrderStatus.CANCELLED;
        addDomainEvent(new OrderCancelledEvent(this, reason));
    }

    // 受保护构造函数（强制使用工厂方法）
    protected Order() {}
}
```

#### 值对象（Value Object）

无唯一标识，不可变，通过属性值判断相等。

```java
// 不可变值对象
public record Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        this.amount = amount.stripTrailingZeros();
        this.currency = Objects.requireNonNull(currency, "Currency is required");
    }

    // 值对象的运算返回新实例
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    // equals/hashCode 基于值
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    // 无 setter，所有字段 final
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }
}
```

#### 聚合（Aggregate）

一致性边界，通过根实体对外访问。

```java
// 聚合根：Order 是 OrderLine 的访问入口
public class Order extends AggregateRoot {
    private OrderId id;
    private List<OrderLine> lines;  // 内部管理，不直接暴露

    // 外部只能通过聚合根添加行
    public void addLine(Product product, int quantity) {
        // 聚合根内验证一致性规则
        validateLine(product, quantity);
        
        OrderLine line = new OrderLine(product.getId(), product.getPrice(), quantity);
        lines.add(line);
        recalculateTotal();
    }

    // 禁止直接访问内部行
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    // 只能通过聚合根修改
    public void removeLine(OrderLineId lineId) {
        lines.removeIf(line -> line.getId().equals(lineId));
        recalculateTotal();
    }
}

// 错误：暴露内部实现
// public class BadOrder {
//     public List<OrderLine> lines;  // 直接暴露，可被外部修改
// }
```

#### 仓储（Repository）

聚合的集合抽象，只增删改查聚合根。

```java
// 领域层：定义仓储接口（无实现依赖）
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Optional<Order> findByIdWithLines(OrderId id);  // 聚合加载策略
    Page<Order> findByCustomer(CustomerId customerId, Pageable pageable);
    void save(Order order);
    void delete(Order order);
}

// 基础设施层：实现仓储
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository delegate;

    @Override
    public Optional<Order> findByIdWithLines(OrderId id) {
        return delegate.findWithLinesById(id.value())
            .map(jpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Order order) {
        JpaOrder entity = jpaMapper.toEntity(order);
        delegate.save(entity);
    }
}
```

#### 领域服务（Domain Service）

无法归入单个实体的业务逻辑。

```java
// 跨实体的业务规则
public class PricingService {
    
    // 计算订单总价，考虑折扣规则
    public Money calculateOrderPrice(List<OrderLine> lines, Customer customer, Promotion promotion) {
        Money subtotal = lines.stream()
            .map(line -> line.getPrice().multiply(line.getQuantity()))
            .reduce(new Money(BigDecimal.ZERO, Currency.USD), Money::add);

        Money discount = calculateDiscount(subtotal, customer, promotion);
        return subtotal.add(discount.negate());
    }

    private Money calculateDiscount(Money subtotal, Customer customer, Promotion promotion) {
        Money discount = Money.ZERO;
        
        // VIP 客户折扣
        if (customer.isVip()) {
            discount = discount.add(subtotal.multiply(0.1));
        }
        
        // 促销活动折扣
        if (promotion != null && promotion.isActive()) {
            discount = discount.add(promotion.applyTo(subtotal));
        }
        
        // 不超过订单金额
        return discount.isGreaterThan(subtotal) ? subtotal : discount;
    }
}
```

#### 领域事件（Domain Event）

领域内发生的重要事件，用于解耦。

```java
// 领域事件定义
public record OrderPlacedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    Instant occurredAt
) {}

// 聚合根发布事件
public class Order extends AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public void place() {
        // ... 放置逻辑
        addDomainEvent(new OrderPlacedEvent(
            this.id,
            this.customerId,
            this.totalAmount,
            Instant.now()
        ));
    }

    @Override
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}

// 事件处理器
@EventHandler
public class OrderEventHandler {
    public void handle(OrderPlacedEvent event) {
        // 发邮件、通知库存、更新报表等
    }
}
```

### 贫血模型 vs 充血模型

| 特征 | 贫血模型（Anti-Pattern） | 充血模型（推荐） |
|------|-------------------------|----------------|
| 实体内容 | 只有字段 + getter/setter | 字段 + 业务行为 |
| 业务逻辑位置 | Service 层 | 领域对象内部 |
| 状态变更 | Service 直接修改字段 | 领域对象方法封装 |
| 验证逻辑 | Service 或工具类 | 领域对象自验证 |
| 可测试性 | 测试 Service | 测试领域对象 |

```java
// 贫血模型（错误）
public class AnemicOrder {
    private UUID id;
    private List<OrderLine> lines;
    private OrderStatus status;

    // 只有 getter/setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ...
}

// Service 承担所有业务逻辑（违反单一职责）
public class AnemicOrderService {
    public void placeOrder(Order order) {
        if (order.getLines().isEmpty()) {
            throw new IllegalStateException();
        }
        order.setStatus(OrderStatus.PLACED);
        repository.save(order);
        // 发邮件、扣库存...
    }
}

// 充血模型（正确）
public class Order {
    public void place() {
        if (this.lines.isEmpty()) {
            throw new OrderEmptyException();
        }
        this.status = OrderStatus.PLACED;
    }
}
```

## 六边形架构（Hexagonal Architecture）

```
                    ┌─────────────────────┐
                    │    Primary Adapters  │
                    │  (Driving Actors)     │
                    │                       │
                    │  ┌─────────────────┐  │
                    │  │   Controllers   │  │
                    │  │   REST, GraphQL │  │
                    │  │   CLI, Events   │  │
                    │  └────────┬────────┘  │
                    └───────────┼───────────┘
                                │
                                ▼
                    ┌─────────────────────┐
                    │                     │
                    │      PORTS           │
                    │  (Inbound Interfaces)│
                    │                     │
                    │  ┌─────────────────┐  │
                    │  │  Use Cases /    │  │
                    │  │  Commands       │  │
                    │  │                 │  │
                    │  └────────┬────────┘  │
                    │           │            │
                    │           ▼            │
                    │  ┌─────────────────┐  │
                    │  │     DOMAIN       │  │
                    │  │   (Core Logic)   │  │
                    │  │                  │  │
                    │  │  Entities       │  │
                    │  │  Value Objects  │  │
                    │  │  Domain Services │  │
                    │  └────────┬────────┘  │
                    │           │            │
                    │      PORTS           │
                    │  (Outbound Interfaces)│
                    │           │            │
                    │  ┌────────┴────────┐  │
                    │  │    Secondary    │  │
                    │  │    Adapters     │  │
                    │  │                 │  │
                    │  │  Repositories   │  │
                    │  │  External APIs  │  │
                    │  │  Message Queues │  │
                    └──┴─────────────────┴──┘
```

## 事件驱动架构

### 事件溯源（Event Sourcing）

```java
// 事件存储替代状态存储
public class BankAccount {
    private AccountId id;
    private List<DomainEvent> events = new ArrayList();

    // 从事件重放构建状态
    public void replay(Iterable<DomainEvent> events) {
        events.forEach(this::mutate);
    }

    private void mutate(DomainEvent event) {
        switch (event) {
            case DepositedEvent e -> apply(e);
            case WithdrawnEvent e -> apply(e);
        }
    }

    private void apply(DepositedEvent e) {
        this.balance = this.balance.add(e.amount());
    }

    // 命令产生事件
    public void deposit(Money amount) {
        if (amount.isNegative()) throw new InvalidAmountException();
        events.add(new DepositedEvent(id, amount, Instant.now()));
        mutate(events.get(events.size() - 1));
    }
}
```

### CQRS（命令查询职责分离）

```
┌─────────────────┐         ┌─────────────────┐
│   Commands      │         │     Queries     │
│  (Write Model)  │         │  (Read Model)   │
│                 │         │                 │
│  CreateOrder    │────────►│  OrderSummary   │
│  UpdateOrder    │  同步    │  OrderDetails   │
│  CancelOrder    │  异步    │  OrderHistory   │
│                 │         │                 │
└────────┬────────┘         └────────▲────────┘
         │                            │
         │        ┌──────────────────┘
         │        │
         ▼        ▼
    ┌─────────────────────────────────┐
    │       Event Store / Bus         │
    │   (Kafka, EventStore, etc.)     │
    └─────────────────────────────────┘
```

## 微服务设计模式

### 聚合式 vs 事件驱动

| 模式 | 特点 | 适用场景 |
|------|------|----------|
| **聚合式** | 单体架构，按限界上下文划分 | 团队小、业务复杂度适中 |
| **事件驱动** | 通过事件异步协作 | 独立部署、高并发、跨系统 |
| **Saga** | 分布式事务管理 | 需要跨服务一致性 |
| **CQRS** | 读写分离 | 读写比例差异大 |

### 服务间通信

```
同步通信：                        异步通信：
┌─────┐    REST/gRPC    ┌─────┐   ┌─────┐    Event    ┌─────┐
│ A   │ ───────────────► │ B   │   │ A   │ ─────────► │ B   │
└─────┘                  └─────┘   └─────┘            └─────┘
      响应                          发布/订阅          消费处理
```

## 架构决策记录（ADR）

每个重要架构决策需要记录：

```markdown
# ADR-001: 使用充血模型设计订单聚合

## 状态
已接受

## 背景
订单业务逻辑分散在 OrderService 和各处，缺乏统一封装。

## 决策
采用充血模型，将订单状态变更、业务规则封装在 Order 实体内部。

## 结果
- 订单状态机完整封装
- 业务规则内聚在领域对象
- 易于单元测试

## 后果
- 需要团队学习 DDD 充血模型
- 聚合设计需要仔细评审
```

## 架构评审清单

### 代码级评审

- [ ] 无循环依赖（模块/包级别）
- [ ] 领域层无基础设施依赖
- [ ] 实体包含业务行为（不只是字段）
- [ ] 值对象不可变
- [ ] 聚合边界清晰
- [ ] 仓储只操作聚合根

### 设计级评审

- [ ] 限界上下文划分合理
- [ ] 上下文映射关系明确
- [ ] 核心域得到足够投入
- [ ] 架构层次遵守依赖规则

### 变更影响分析

- [ ] 变更会导致哪些模块/层需要修改
- [ ] 新增功能应该放在哪个限界上下文
- [ ] 是否需要创建新的聚合或服务
