# AI-Explore

基于 SpringAI + Angular 的 AI 应用平台。

## 技术栈

### 前端
- Angular 20+ + TypeScript
- SCSS 样式管理
- 苹果风格设计系统

### 后端
- Java 24
- Spring Boot 3.5
- Spring AI 1.0

## 快速启动

### 前置条件
- Node.js 20+
- Java 24
- pnpm

### 安装依赖
```bash
pnpm install
```

### 启动服务

```bash
# 启动后端 (端口 8080)
npm run springai:dev

# 启动前端 (端口 4200)
npm run angular:dev
```

### 环境变量

创建 `apps/server-springai/.env` 文件：

```env
OPENAI_API_KEY=your-api-key
OPENAI_BASE_URL=https://api.openai.com
```

## API 接口

### 聊天
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

### 健康检查
```bash
curl http://localhost:8080/api/health
```

## 项目结构

```
ai-explore/
├── apps/
│   ├── web/         # Angular 前端
│   └── server/      # SpringAI 后端
├── .cursor/         # Cursor 配置
└── docs/           # 文档
```
