# E13 Agent façade + A2A

← [用户故事地图](../User-Story-Map.md)

## 背景

Sibling 产品与外部 Agent 需要调用 Explore AI 的 baseline Agent，而不绑定 Pipeline 画布。Explore AI 提供 Published Language（`/api/agents`）与 Linux Foundation A2A Protocol v1.0 最小服务端（Agent Card + message:send / stream）。MCP 仍用于工具，不是跨 Agent 协议。

---

## US-40 调用 baseline Agent

**As a** 兄弟系统开发者  
**I want** 通过 `/api/agents` 列出并调用 catalog Agent  
**So that** 我的产品 BFF 无需嵌入 Pipeline 画布即可获得 reason → tools → reply

### 验收标准

1. **Scenario** 列出 Agent
   **GIVEN** `module-pipelines` 开启且调用方已认证  
   **WHEN** `GET /api/agents`  
   **THEN** 返回 type / name / description / tools（不含 system prompt）

2. **Scenario** 同步调用
   **GIVEN** catalog 中存在 `research`  
   **WHEN** `POST /api/agents/research/invoke` 带 `message`  
   **THEN** 返回该 Agent 的 reply

### 状态

已实现

---

## US-41 A2A Agent Card 与 Task

**As a** 外部 A2A Client  
**I want** 发现 Agent Card 并用 message:send / stream 运行 Task  
**So that** 我可以按行业标准与 Explore AI Agents 互操作

### 验收标准

1. **Scenario** 发现
   **GIVEN** Explore AI 已部署  
   **WHEN** `GET /.well-known/agent-card.json`  
   **THEN** 返回 skills（builtin workers）与 HTTP+JSON 接口，不含 PII / system prompt

2. **Scenario** 发送消息
   **GIVEN** 有效 IAM Bearer（`write:ai_agent`）  
   **WHEN** `POST /a2a/v1/message:send` 带 text parts  
   **THEN** 返回 completed Task（默认 skill `research`）或 failed Task

### 状态

已实现

## 参考

- https://a2a-protocol.org/latest/specification/
- [Glossary §10.1 A2A](../../Glossary.md)
