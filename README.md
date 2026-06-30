# AI-Explore

基于 Spring AI + Angular 的 AI 聊天应用，支持 RAG 文档问答和 Tool Calling。

## 技术栈

| 组件   | 技术                          |
| ------ | ----------------------------- |
| 后端   | Java 25 + Spring Boot 4.0     |
| AI     | Spring AI 2.0 (DeepSeek)     |
| 数据库 | H2 嵌入式数据库 + Liquibase   |
| 前端   | Angular 22 + TypeScript      |

## 快速启动

```bash
# 配置
echo "DEEPSEEK_API_KEY=your-key" > .env

# 本地开发（无需 Docker，H2 随 Spring Boot 进程启动，Liquibase 自动建表）
./gradlew bootRun
```

- 应用: http://localhost:9000
- 健康检查: http://localhost:9000/actuator/health
- 数据库文件: `./data/ai-explore.*`（首次启动自动创建）

## API

### Chat

```bash
curl -X POST http://localhost:9000/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

### RAG

```bash
# 上传文档
curl -X POST http://localhost:9000/api/rag/documents/upload \
  -H "Content-Type: application/json" \
  -d '{"title": "手册", "content": "文档内容..."}'

# 流式问答
curl -X POST http://localhost:9000/api/rag/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"question": "产品功能是什么？"}'
```

## 架构

Clean Architecture + DDD，详见 `docs/c4/`
