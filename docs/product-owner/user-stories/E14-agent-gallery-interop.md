# E14 Agent Gallery 与产品对标互操作

← [用户故事地图](../User-Story-Map.md)

## 背景

对标 Gemini Enterprise / Copilot Studio（Registry）/ Agentforce / Foundry：Explore AI 提供 Gallery（目录）、Studio（Pipeline）、Runtime API（`/api/agents` + A2A Card）。兄弟系统经 BFF 消费；IAM 用 `write:ai_agent` 做客户端可调用治理。不各自接入巨头 Agent SDK。

---

## US-42 Gallery 对话试用

**As a** 访客或登录用户  
**I want** 在 Agents Gallery 选中一位 Agent 并用对话方式提问  
**So that** 我能像使用产品目录一样试用 Worker，而无需打开 Pipeline 或调试面板

### 验收标准

1. **Scenario** 选中 research 并提问  
   **GIVEN** Agents 页已加载精选目录  
   **WHEN** 选中研究 Agent，输入短问题并发送  
   **THEN** 经 `POST /api/agents/{type}/invoke` 返回 reply，并以对话气泡展示在右侧面板

### 状态

已实现

---

## US-43 互操作静默就绪

**As a** 访客或登录用户  
**I want** 看到这些 Agent 可供已连接应用使用  
**So that** 我理解 Gallery 也是对外运行时目录，而无需面对 Card JSON / Task 调试控件

### 验收标准

1. **Scenario** Card 静默发现  
   **GIVEN** 后端 A2A Agent Card 可用  
   **WHEN** 打开 Agents 页  
   **THEN** 页脚展示「可供已连接的应用使用」及 skill 友好名称；不展示 JSON / message:send 调试区

### 状态

已实现

---

## US-44 Chat 消息内 Service Agent

**As a** Chat 用户  
**I want** 在消息页通过 Chat BFF 向 Gallery Agent（含 support）提问  
**So that** 我获得类似 Agentforce Service Agent 的客服/助手体验，且浏览器不直连 Explore AI

### 验收标准

1. **Scenario** 经 BFF 列表与调用  
   **GIVEN** Chat 已配置 `EXPLORE_AI_BASE_URL` 与 `EXPLORE_AI_SERVICE_KEY`  
   **WHEN** 客户端调用 `GET /api/v1/ai/explore/agents` 与 `POST .../agents/{type}/invoke`  
   **THEN** Chat 以 `X-Service-Key` + `X-Client-Id` 转发 Explore AI，返回摘要或 reply

### 状态

已实现

---

## US-45 IAM Agent Registry（write:ai_agent）

**As a** IAM 管理员  
**I want** 为 OAuth 客户端授予或撤销 `write:ai_agent`  
**So that** 只有获准的应用能调用 A2A / 受保护的 Agent 面（Copilot Registry + Entra 形态）

### 验收标准

1. **Scenario** 有 scope  
   **GIVEN** 客户端 scopes 含 `write:ai_agent` 且持有有效 Bearer  
   **WHEN** 调用 `POST /a2a/v1/message:send`  
   **THEN** 请求不被 scope 拒绝（可完成或业务失败）

2. **Scenario** 无 scope  
   **GIVEN** 客户端无 `write:ai_agent`  
   **WHEN** 调用同一 A2A 端点  
   **THEN** 返回 403（或等价未授权）

### 状态

已实现

---

## US-46 Commerce 领域 Agent（薄 BFF）

**As a** Commerce API 调用方  
**I want** 经 Commerce 代理列出并调用 Explore AI Agents（可选订单摘要 grounding）  
**So that** 客服场景贴近 Agentforce 领域 Agent，而不嵌入 Salesforce SDK

### 验收标准

1. **Scenario** 代理 invoke  
   **GIVEN** Commerce API 已配置 Explore AI 上游与 service key  
   **WHEN** `POST /api/ai/agents/:type/invoke`  
   **THEN** 转发至 Explore AI `/api/agents/{type}/invoke` 并返回 reply

### 状态

已实现

---

## 兄弟系统

| 系统 | 故事方向 | 对标 | 状态 |
| --- | --- | --- | --- |
| Explore Chat | 消息内 Service Agent（BFF → Gallery） | Agentforce Service Agent / Gemini 宿主 | US-44 |
| Explore IAM | 客户端 `write:ai_agent` 授予 | Copilot Registry + Entra | US-45 |
| Explore Commerce | 客服领域 Agent 薄 BFF + grounding | Agentforce 领域 Agent | US-46 |
| Explore Portfolio | 研判 / 风险 Agent 经 AI BFF | Joule 领域接地 | 索引 |

## 参考

- [Glossary §10.2](../../Glossary.md)
- https://docs.cloud.google.com/gemini/enterprise/docs
