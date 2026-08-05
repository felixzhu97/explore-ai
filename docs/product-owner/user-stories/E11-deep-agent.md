# E11 Deep Agent（已退役）

← [用户故事地图](../User-Story-Map.md)

> **Status: retired.** Standalone Agent module (`/agents`, Deep sessions, saved agent library, `module-agents`) was removed.
>
> Product direction: **Pipeline-only workers**. Builtin multilingual definitions seed the `/pipelines` palette; double-click a canvas node to edit the **per-graph copy** (name, systemPrompt, toolKeys). Workflow templates pick agents visually (no comma-separated `agentTypes`); task input lives next to **Run** on the canvas.
>
> Successor stories: [E7 Pipeline（工作流）](./E7-multi-agent.md).

## 历史范围（归档）

| ID | 原能力 | 现状 |
| --- | --- | --- |
| US-32 | 带记忆的多轮 Agent 会话 | 已移除；不再提供独立 Agent 工作区 |
| US-33 | Plan / tool / eval 过程可见 | 已移除 Deep SSE；Pipeline 结果轨展示 handoff / 工具步骤 |
| US-34 | 多语言 Agent 模版库 | 内置目录迁入 Pipeline `agent-templates/{lang}.json`；无客户端 Agent 库 |
