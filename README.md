# AI-Explore

基于 Spring AI + Angular 的 AI 聊天应用。

## 技术栈

| 前端 | 后端 |
|------|------|
| Angular 19+ | Java 25 |
| TypeScript | Spring Boot 3.5 |
| SCSS | Spring AI 1.1.7 (DeepSeek) |

## 快速启动

```bash
pnpm install
npm run springai:dev  # 后端 (端口 8080)
npm run angular:dev   # 前端 (端口 4200)
```

环境变量 `apps/server/.env`:
```env
SPRING_AI_DEEPSEEK_API_KEY=your-api-key
```

## API

```bash
# 发送消息
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 会话管理
curl http://localhost:8080/api/sessions          # 列表
curl http://localhost:8080/api/sessions/{id}/messages  # 历史
curl -X DELETE http://localhost:8080/api/sessions/{id}  # 删除
```

## 架构

```
Interface → Application → Domain ← Infrastructure
```

详细 C4 模型见 `docs/c4/`
