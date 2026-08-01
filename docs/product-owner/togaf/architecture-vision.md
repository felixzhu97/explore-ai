# Architecture Vision（Phase A）

> 负责人：Product Owner  
> 基线：ExploreAI as-is  
> 相关：[Architecture Principles](./architecture-principles.md) · [Stakeholders](./stakeholders.md)

## 目的

界定 ExploreAI 业务架构的范围、约束与价值主张，作为 Phase B（Capability / Value Stream）的上游 Deliverable。不替代解决方案或技术架构（见 [C4](../../developer/c4-model/)）。

## 业务驱动因素（as-is）

- 最终用户需要低摩擦的流式对话与可选工具增强
- 访客需要可理解的隐私控制与浏览器级会话隔离
- 研判类用户需要可编排、可模板化、可复核的 Multi-Agent brief
- 协作方需要 Capability / Value Stream 与 User Story Map 对齐的交付语言

## 范围

| 范围内（Business Architecture） | 范围外 |
|----------------------------------|--------|
| 对话、隐私、知识/视觉、媒体、MCP、质量观测、Multi-Agent 等 L1 Capability | Application / Data / Technology Catalog（Phase C/D → C4） |
| 三条 as-is Value Stream 及 Stage | Phase E–H（Roadmap / Contract / Compliance 全套） |
| Stakeholder concerns 与 Organization Mapping（light） | L2 Capability 抄写 User Story（见 User Story Map） |

## 约束

- Web 前端与常驻后端分离交付（拓扑见 C4）
- 访客会话按浏览器隔离；隐私告知 / 同意 / 擦除为基本控制
- 外部模型与工具以已接入能力为准，BA 层不展开供应商清单

## 价值主张（as-is）

沿 Value Stream，用户可获得：流式对话回答、有据可查的知识问答、可复核的 Multi-Agent brief；Product Owner 用 Business Capability 与 [User Story Map](../User-Story-Map.md) 对齐交付状态。

## Stakeholders（摘要）

见 [Stakeholders Catalog](./stakeholders.md) 与 [Stakeholder Map](./stakeholder-map.md)。主要 concerns：可用性、隐私信任、研判可汇报、质量可量化、运维可观测。

## Architecture Principles

见 [Architecture Principles Catalog](./architecture-principles.md)（`AP-01`…`AP-04`）。

## 追溯关系

| Vision 要素 | 下游 |
|-------------|------|
| Capabilities | [business-capabilities.md](./business-capabilities.md) |
| Value delivery | [value-streams.md](./value-streams.md) |
| Organization | [stakeholders.md](./stakeholders.md) · [capability-stakeholder.md](./capability-stakeholder.md) |
| Delivery stories | [User Story Map](../User-Story-Map.md) |
| SBB / Tech | [C4 model](../../developer/c4-model/) |
