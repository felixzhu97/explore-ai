# C4 模型文档

使用 PlantUML 绘制的 C4 架构模型，描述 AI Chat & Agent Platform 的完整架构。

## 文件

| 文件                              | 层级 | 说明                                         |
| --------------------------------- | ---- | -------------------------------------------- |
| `C1-Context.puml`                 | C1   | 系统上下文图                                 |
| `C2-Container.puml`               | C2   | 容器图（4 个子域：Chat / RAG / Tooling & Agent / Cross-Cutting）|
| `C3-Component-Backend.puml`       | C3   | 后端组件图（Interface / Application / Domain / Infrastructure 四层）|
| `C3-Component-Frontend.puml`      | C3   | 前端组件图（App / Pages / Agents / Core / Shared）|
| `C4-Deployment.puml`              | C4   | 本地开发环境部署图（端口 9000）              |
| `../cicd/cicd-workflow.puml`      | CI/CD | 统一 CI/CD 流水线图                         |

## 技术栈

### 后端 (AI Platform Backend)
- **运行时**: Spring Boot 4.1 / Java 25 / Spring AI 2.0
- **端口**: **9000** (统一)
- **子域**: Chat Domain / RAG Domain / Tooling & Agent Domain / Cross-Cutting
- **向量库**: PostgreSQL + pgvector (端口 5432)
- **外部服务**: DeepSeek API (LLM + Embedding) / Ollama (本地 Embedding, 端口 11434) / DuckDuckGo Search

### 前端 (Web Frontend)
- **框架**: Angular 22 + TypeScript (AnalogJS Vite plugin)
- **路由**: `/ai-infra` (默认) / `/rag` / `/vision` / `/aihubs`
- **端口**: 4200 (dev server, proxy `/api` → `:9000`)

## 部署端口汇总

| 服务                    | 端口  |
| ----------------------- | ----- |
| Spring Boot Backend     | **9000** |
| PostgreSQL + pgvector   | 5432  |
| Ollama (Embedding)      | 11434 |
| Angular Dev Server      | 4200  |
| DeepSeek API            | HTTPS |
| DuckDuckGo Search       | HTTPS |

## 查看

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- `plantuml -o png *.puml`
