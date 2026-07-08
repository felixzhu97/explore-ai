# C4 模型文档

使用 PlantUML 绘制的 C4 架构模型，描述 AI Chat & Agent Platform 的完整架构。

## 文件

| 文件                           | 层级  | 说明                                            |
| ---------------------------- | --- | --------------------------------------------- |
| `C1-Context.puml`            | C1  | 系统上下文图                                        |
| `C2-Container.puml`          | C2  | 容器图（7 个子域：Chat / RAG / Tool Calling / Image / TTS / ASR / MCP Server / MCP Client） |
| `C3-Component-Backend.puml`  | C3  | 后端组件图（Interface / Domain / Infrastructure 三层） |
| `C3-Component-Frontend.puml` | C3  | 前端组件图（App / Pages / Agents / Core / Shared）   |
| `C4-Deployment.puml`         | C4  | 本地开发环境部署图（端口 9000）                         |

---

## C1 - 系统上下文图

![C1-Context](./png/C1-Context.png)

---

## C2 - 容器图

![C2-Container](./png/C2-Container.png)

---

## C3 - 后端组件图

![C3-Component-Backend](./png/C3-Component-Backend.png)

---

## C3 - 前端组件图

![C3-Component-Frontend](./png/C3-Component-Frontend.png)

---

## C4 - 部署图

![C4-Deployment](./png/C4-Deployment.png)

---

## 技术栈

### 后端 (AI Platform Backend)

- **运行时**: Spring Boot 4.0 / Java 25 / Spring AI 2.0
- **端口**: **9000** (统一)
- **子域**: Chat Domain / RAG Domain / Tool Calling Domain / Image Generation Domain / Audio/TTS Domain / ASR Domain / MCP Server Domain / MCP Client Domain
- **向量库**: H2 嵌入式向量 (内置) / PostgreSQL + pgvector (可选)
- **外部服务**: DeepSeek API (LLM) / Ollama (本地 Embedding + Vision, 端口 11434) / whisper.cpp (本地 ASR, 端口 8178) / DALL-E API (图像生成) / TTS Service (语音合成)

### 前端 (Web Frontend)

- **框架**: Angular 22 + TypeScript
- **路由**: AI Infra / RAG / Vision / AI Hubs
- **端口**: 4200 (dev server, proxy `/api` → `:9000`)

## 部署端口汇总

| 服务                    | 端口       |
| --------------------- | -------- |
| Spring Boot Backend   | **9000** |
| PostgreSQL + pgvector | 5432     |
| Ollama (Embedding/Vision/ASR) | 11434    |
| Angular Dev Server    | 4200     |
| DeepSeek API          | HTTPS    |
| DALL-E API            | HTTPS    |
| TTS Service           | HTTPS    |

## 查看

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- `plantuml -o png *.puml`
