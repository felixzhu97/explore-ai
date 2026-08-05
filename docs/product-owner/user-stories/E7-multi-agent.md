# E7 Pipeline（工作流）

← [用户故事地图](../User-Story-Map.md)

## 背景

业务分析师可在画布编排工作流并流式审阅阶段与结果；产品经理可一键选用预置模板；研判输出以 Fact / Inference / Recommendation 结构呈现。

---

## US-15 工作流工作台

**As a** 业务分析师  
**I want** 在画布上编排工作流并流式审阅阶段与结果  
**So that** 我可以按企业流程定制多步 AI 工作流

### 验收标准

1. **Scenario** 画布编排工作流
   **GIVEN** 业务分析师打开工作流工作台  
   **WHEN** 在画布上编排工作流  
   **THEN** 工作流结构可见并可按多步流程定制

2. **Scenario** 流式审阅阶段与结果
   **GIVEN** 已编排并可运行的工作流  
   **WHEN** 运行工作流  
   **THEN** 可流式审阅各阶段进展与结果  
   **AND** 最终结果可在工作台中查看

### 状态

已实现

---

## US-16 企业工作流模版（内置 + 可自定义）

**As a** 产品经理  
**I want** 从多语言内置模版一键选用，并可添加到我的模版库后自定义  
**So that** 常见企业研判无需从零搭图，且可按团队流程沉淀模版

### 验收标准

1. **Scenario** 一键选用多语言内置模版
   **GIVEN** 工作台从后端加载当前语言的预置编排模版  
   **WHEN** 产品经理一键选用某一模版  
   **THEN** 画布加载该模版对应的编排  
   **AND** 无需从零搭图即可开始

2. **Scenario** 添加并自定义我的模版
   **GIVEN** 产品经理打开工作流工作台  
   **WHEN** 将内置模版添加到库、自定义字段并保存  
   **THEN** 我的模版列表可见该条目  
   **AND** 可再次选用、编辑、启停或删除

3. **Scenario** 选用后可运行研判类流程
   **GIVEN** 已选用企业研判相关模版  
   **WHEN** 按模版运行  
   **THEN** 可得到基于该模版的流程输出

### 状态

已实现

---

## US-17 企业研判标准化输出

**As a** 业务或技术负责人  
**I want** Analyst 以 Fact / Inference / Recommendation 结构输出 brief  
**So that** 研判结果可复核、可汇报

### 验收标准

1. **Scenario** brief 含 Fact / Inference / Recommendation
   **GIVEN** 用户运行会产生 Analyst brief 的研判流程  
   **WHEN** 流程完成并展示 brief  
   **THEN** 输出包含 Fact、Inference、Recommendation 结构  
   **AND** 内容可用于复核与汇报

2. **Scenario** 结构完整便于汇报
   **GIVEN** brief 已生成  
   **WHEN** 负责人阅读输出  
   **THEN** 可区分事实、推断与建议  
   **AND** 不必自行重组结构即可汇报

### 状态

已实现
