# Skills 索引

本目录包含项目的 Skills 定义，每个 Skill 都有详细的开发指南和参考文档。

## 按用途分类

### 架构设计

| Skill | 版本 | 描述 |
|-------|------|------|
| [software-architecture](./software-architecture/) | v1.0 | 软件架构设计方法论，涵盖 Clean Architecture、DDD、C4 模型 |
| [hexagonal-architecture](./hexagonal-architecture/) | v1.0 | 六边形架构重构指南，Ports & Adapters 模式 |
| [clean-architecture](./clean-architecture/) | - | 苹果风格整洁架构开发规范 |

### 开发实践

| Skill | 版本 | 描述 |
|-------|------|------|
| [software-development](./software-development/) | v1.0 | 整洁代码开发实践，命名规范、函数设计 |
| [angular-developer](./angular-developer/) | v1.0 | Angular 20+ 最佳实践，信号、响应式、表单 |
| [angular-new-app](./angular-new-app/) | v1.0 | Angular 新项目创建指南 |

### AI 集成

| Skill | 版本 | 描述 |
|-------|------|------|
| [spring-ai](./spring-ai/) | v2.0 | Spring AI 2.0 开发指南，ChatClient、RAG、Tool Calling |

### 测试质量

| Skill | 版本 | 描述 |
|-------|------|------|
| [software-testing](./software-testing/) | v1.0 | 测试方法论，TDD/BDD/测试金字塔 |

---

## 如何使用

在 Cursor Agent 中，当任务涉及相关内容时，会自动读取对应的 Skill：

- 设计架构 → `software-architecture`
- 重构代码 → `hexagonal-architecture`
- 编写测试 → `software-testing`
- Angular 开发 → `angular-developer`
- Spring AI 集成 → `spring-ai`

---

## 更新日志

| 日期 | 更新内容 |
|------|----------|
| 2026-06-20 | 统一添加版本信息和 lastUpdated 字段 |
