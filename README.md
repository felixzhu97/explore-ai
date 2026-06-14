# AI-Explore

基于 Spring AI + Angular 的 AI 聊天应用，支持 RAG 文档问答。

## 技术栈

| 组件   | 技术                         |
| ------ | ---------------------------- |
| 后端   | Java 25 + Spring Boot 3.5.11 |
| AI     | Spring AI 1.1.7 (DeepSeek)   |
| 数据库 | PostgreSQL 17 + pgvector     |
| 前端   | Angular 19+ + TypeScript     |
| 容器化 | Docker + Docker Compose      |

## 架构

Clean Architecture + DDD，详见 `docs/c4/`

## 快速启动

### 1. 配置

创建 `apps/server/.env`:

```env
DEEPSEEK_API_KEY=your-deepseek-api-key
```

### 2. 启动 (Docker)

```bash
cd apps/server && docker-compose up -d
```

- 应用: http://localhost:8080
- 健康检查: http://localhost:8080/actuator/health

### 3. 本地开发

```bash
cd apps/server
docker-compose up postgres -d
./gradlew bootRun
```

## API

### 聊天

```bash
# 发送消息
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 列出会话
curl http://localhost:8080/api/sessions

# 获取历史
curl http://localhost:8080/api/sessions/{id}/messages
```

### RAG

```bash
# 上传文档
POST /api/rag/documents/upload
{"title": "产品手册", "content": "文档内容..."}

# 流式问答
POST /api/rag/chat/stream
Accept: text/event-stream
{"question": "产品功能是什么？"}
```

## 环境变量

| 变量               | 默认值                                  | 说明   |
| ------------------ | --------------------------------------- | ------ |
| `DEEPSEEK_API_KEY` | -                                       | 必填   |
| `POSTGRES_URL`     | jdbc:postgresql://localhost:5432/ai_rag | 数据库 |

详见 `docs/c4/` 架构图和 `docs/wardley-map.puml` 技术演进图。

## CI/CD

### Workflows overview

| 文件 | 用途 |
| ---- | ---- |
| `backend-ci.yml` | Java/Gradle build + tests + JaCoCo coverage |
| `codeql.yml` | Security scanning for Java/Kotlin and TypeScript |
| `review-dog.yml` | Frontend quality (ESLint, TypeScript, Prettier, Vitest) with reviewdog PR comments |

### Branch triggers

> The trigger branch list `["main", "java-angular", "feature/**"]` is intentionally **duplicated** across all 3 workflow files. GitHub Actions does not support a shared trigger definition.

**If you add or remove a protected branch, update all 3 files in the same PR.**

- `.github/workflows/backend-ci.yml`
- `.github/workflows/codeql.yml`
- `.github/workflows/review-dog.yml`
