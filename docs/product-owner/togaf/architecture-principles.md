# Catalog — Architecture Principles

> 负责人：Product Owner · ADM Preliminary / Phase A

| ID | Name | Statement | Rationale | Implications |
|----|------|-----------|-----------|--------------|
| AP-01 | Capability-Led Planning | 规划与验收以 Business Capability 与可观察结果表述 | 能力比组织与实现细节更稳定（Business Capabilities Guide） | Catalog 与 User Story Map 同步状态；不以类名或工具名为故事主语 |
| AP-02 | Value-Stream Traceability | 用户价值经 Value Stream Stage 追溯到启用的 Capability | Value Streams Guide 要求 Capability–Stage 交叉映射 | 维护 [capability-value-stream](./capability-value-stream.md) |
| AP-03 | Trust for Visitors | 访客数据按浏览器隔离，并提供隐私说明与擦除控制 | 现状已交付隐私与隔离能力，构成可信使用前提 | `BC-L1-02` 保持已实现，直至有变更故事 |
| AP-04 | Separate BA from SBB | Business Architecture 不描述部署与组件实现；SBB 见 C4 | TOGAF 区分 Architecture Building Block 与 Solution Building Block | Phase C/D 内容不写入本目录；实现变更更新 C4 |
