# AI-Explore

基于 Spring AI + Angular 的 AI 聊天应用。

A Clean Architecture AI chat application powered by Spring AI and DeepSeek.

## 技术栈 | Technology Stack

### 前端 | Frontend
- Angular 20+ + TypeScript
- SCSS 样式管理 | SCSS styling

### 后端 | Backend
- Java 25
- Spring Boot 3.5
- Spring AI 1.1.7 (DeepSeek)

## 功能特点 | Features

- **DeepSeek AI 对话** | DeepSeek AI Chat - 基于 DeepSeek 的智能对话能力
- **多会话管理** | Multi-Session Management - 支持创建、切换、删除聊天会话
- **消息历史** | Message History - 完整的对话历史记录
- **Clean Architecture** - 遵循领域驱动设计（DDD）原则
- **Hexagonal Architecture** - 端口与适配器架构模式

## 快速启动 | Quick Start

### 前置条件 | Prerequisites
- Node.js 20+
- Java 25
- pnpm

### 安装依赖 | Install Dependencies
```bash
pnpm install
```

### 启动服务 | Start Services

```bash
# 启动后端 (端口 8080) | Start backend (port 8080)
npm run springai:dev

# 启动前端 (端口 4200) | Start frontend (port 4200)
npm run angular:dev
```

### 环境变量 | Environment Variables

创建 `apps/server/.env` 文件：

```env
SPRING_AI_DEEPSEEK_API_KEY=your-api-key
SPRING_AI_DEEPSEEK_BASE_URL=https://api.deepseek.com
```

## API 接口 | API Reference

### 发送消息 | Send Message
```bash
# 带会话ID | With session ID
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "xxx", "message": "你好"}'

# 新会话 | New session
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

### 会话管理 | Session Management

```bash
# 创建会话 | Create session
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "My Chat"}'

# 获取所有会话 | List sessions
curl http://localhost:8080/api/sessions

# 获取会话消息历史 | Get message history
curl http://localhost:8080/api/sessions/{sessionId}/messages

# 删除会话 | Delete session
curl -X DELETE http://localhost:8080/api/sessions/{sessionId}
```

### 健康检查 | Health Check
```bash
curl http://localhost:8080/api/health
```

## 项目结构 | Project Structure

```
ai-explore/
├── apps/
│   ├── web/                    # Angular 前端 | Angular frontend
│   │   └── src/
│   │       └── app/
│   └── server/                 # Spring Boot 后端 | Spring Boot backend
│       └── src/main/java/com/ai/
│           ├── domain/                    # 领域层 | Domain Layer
│           │   ├── model/                 # 实体 & 聚合根 | Entities & Aggregates
│           │   ├── vo/                    # 值对象 | Value Objects
│           │   ├── event/                 # 领域事件 | Domain Events
│           │   └── service/               # 领域服务 | Domain Services
│           ├── application/               # 应用层 | Application Layer
│           │   ├── port/                  # 端口定义 | Port Definitions
│           │   ├── service/               # 应用服务 | Application Services
│           │   └── usecase/               # 用例 | Use Cases
│           ├── infrastructure/             # 基础设施层 | Infrastructure Layer
│           │   ├── adapter/               # 适配器 | Adapters
│           │   │   ├── ai/                 # AI 适配器 | AI Adapters
│           │   │   └── persistence/        # 持久化适配器 | Persistence Adapters
│           │   └── config/                 # 配置 | Configuration
│           └── interfaces/                 # 接口层 | Interface Layer
│               ├── controller/             # REST 控制器 | REST Controllers
│               └── dto/                    # 数据传输对象 | Data Transfer Objects
├── docs/                              # 文档 | Documentation
└── .cursor/                           # Cursor 配置 | Cursor Configuration
```

## Clean Architecture 概览 | Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Interface Layer                         │
│              (Controllers, DTOs, REST API)                   │
├─────────────────────────────────────────────────────────────┤
│                     Application Layer                       │
│           (Services, Use Cases, Port Interfaces)             │
├─────────────────────────────────────────────────────────────┤
│                       Domain Layer                          │
│         (Entities, Value Objects, Domain Events)             │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│              (AI Adapters, Persistence, Config)              │
└─────────────────────────────────────────────────────────────┘

依赖方向: Interface → Application → Domain ← Infrastructure
Dependency Direction: Interface → Application → Domain ← Infrastructure
```

## 架构原则 | Architecture Principles

| 原则 | Principle | 说明 | Description |
|------|-----------|------|-------------|
| 依赖倒置 | Dependency Inversion | 外层依赖内层，内层不依赖外层 | Outer depends on inner, inner knows nothing about outer |
| 领域核心 | Domain Core | 领域层无外部框架依赖 | Domain has no framework dependencies |
| 端口隔离 | Port Isolation | 端口在应用层定义，适配器在基础设施层实现 | Ports defined in application, implemented in infrastructure |
