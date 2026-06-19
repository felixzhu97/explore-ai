---
name: hexagonal-architecture
description: Hexagonal Architecture 重构指南。将现有代码重构为端口-适配器架构，同时保留 DDD 充血模型。
version: "1.0"
lastUpdated: "2026-06-20"
---

> **前置规则**: 本 skill 基于 `.cursor/rules/hexagonal-architecture.mdc` 中定义的规则执行重构

---

# Hexagonal Architecture 重构指南

## 重构流程

```
1. 识别领域概念 → 提取 Domain 层 (核心，无依赖)
2. 识别 Driving 适配器 → 创建 adapter/in (主端口)
3. 识别 Driven 适配器 → 创建 adapter/out (从端口)
4. 整合配置 → 创建 Config 层
```

---

## 重构阶段

### 阶段 1: 提取 Domain 层

**目标**: 创建核心业务模型，无任何外部依赖

```java
// ✅ 充血模型
public class Order extends AggregateRoot {
    private final UUID id;
    private OrderStatus status;
    private final List<OrderLine> lines;

    private Order(UUID id) { /* ... */ }

    public static Order create() {
        return new Order(UUID.randomUUID());
    }

    public void place() {
        validateCanPlace();
        this.status = OrderStatus.PLACED;
    }
}
```

### 阶段 2: 创建 adapter/in (Driving)

**目标**: 主适配器接收外部输入

```java
// 控制器
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final PlaceOrderUseCase useCase;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody Request req) {
        return ResponseEntity.ok(useCase.execute(req.toCommand()));
    }
}
```

### 阶段 3: 创建 adapter/out (Driven)

**目标**: 从适配器连接外部系统

```java
// 仓储实现
@Repository
public class JpaOrderRepository implements OrderRepository {
    @Override
    public Optional<Order> findById(UUID id) { /* ... */ }
}
```

### 阶段 4: Config 层整合

**目标**: Spring 配置和依赖注入

```java
@SpringBootApplication
@ComponentScan(basePackages = "com.ai")
public class AiApplication { }
```

---

## 常见问题

### Q: 如何区分 adapter/in 和 adapter/out？

- **adapter/in**: 驱动系统的输入点 (Controllers, REST APIs)
- **adapter/out**: 被系统调用的外部依赖 (Repositories, External Services)

### Q: Domain 层可以依赖哪些？

- ✅ 纯 Java 类
- ✅ 值对象 (Value Objects)
- ✅ 端口接口 (Port Interfaces)
- ❌ Spring/Jakarta 注解
- ❌ 数据库驱动

### Q: 用例 (UseCase) 放哪里？

- 小型项目: 放在 `adapter/in/controller` 作为私有类
- 中型项目: 放在 `domain/service/` 作为领域服务
- 大型项目: 单独创建 `application/usecase/` 包

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
