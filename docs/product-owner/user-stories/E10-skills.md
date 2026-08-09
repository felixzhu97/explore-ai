# E10 Skills

← [用户故事地图](../User-Story-Map.md)

## 背景

用户需要可复用的指令包（Skill）来统一语气、结构与领域规则；并在聊天时按需选用，避免每轮从零约定。

Jira: [AI-325](https://felixzhu.atlassian.net/browse/AI-325)、[AI-326](https://felixzhu.atlassian.net/browse/AI-326)

---

## US-30 管理可复用 Skills

**As a** 重度用户  
**I want** 创建、编辑、启停与删除可复用 Skills  
**So that** 我能把一致的指引保存下来供后续对话使用

### 验收标准

1. **Scenario** 创建并列出 Skills  
   **GIVEN** Skills 模块可用  
   **WHEN** 用户创建带名称与指令的 Skill  
   **THEN** 该 Skill 出现在本人列表中  
   **AND** 仅本人可见

2. **Scenario** 编辑、启停与删除  
   **GIVEN** 用户拥有已有 Skill  
   **WHEN** 编辑、禁用或删除  
   **THEN** 列表与详情反映变更  
   **AND** 已禁用的 Skill 不能被选入新的聊天回合

3. **Scenario** 从模板新建  
   **GIVEN** 提供内置 Skill 模板  
   **WHEN** 用户一键添加模板，或自定义后保存  
   **THEN** 库中出现对应 Skill 副本  
   **AND** 同名冲突时自动后缀（如 `Brief Style (2)`）

### 状态

已实现

---

## US-31 在聊天中应用 Skills

**As a** 聊天用户  
**I want** 发送消息时选择一个或多个已启用的 Skills  
**So that** 回复遵循我选定的指令包

### 验收标准

1. **Scenario** 聊天中应用 Skills  
   **GIVEN** 至少有一个已启用的 Skill  
   **WHEN** 在聊天中选中并发送消息  
   **THEN** 助手回复体现该 Skill 的指令

2. **Scenario** 未选择 Skills  
   **GIVEN** 存在 Skills 但未选中  
   **WHEN** 发送聊天消息  
   **THEN** 回复使用默认指引，不附加这些 Skill 指令包

### 状态

已实现

---

## US-35 工作台页布局与 Metrics 一致

**As a** 访客  
**I want** Skills、Agents、自动化与工作流画廊的页壳与指标页一致  
**So that** 标题、主操作与内容宽度在各模块间可预期

### 验收标准

1. **Scenario** Skills / Agents / 自动化页眉对齐指标  
   **GIVEN** 访客打开 Skills、Agents 或自动化  
   **WHEN** 查看首屏  
   **THEN** 可见 ExploreAI 眉题、左对齐大标题，以及同排主 CTA  
   **AND** 无 sticky 顶部分割栏

2. **Scenario** 工作流画廊页眉对齐指标  
   **GIVEN** 访客打开工作流画廊  
   **WHEN** 查看画廊  
   **THEN** 无居中的双层空状态标题  
   **AND** Metrics 式页眉展示工作流标题，同排「添加」

3. **Scenario** 控件保持安静权重  
   **GIVEN** 访客查看 Agents 或工作流操作  
   **WHEN** 查看主/次按钮  
   **THEN** 仍为边框 / ghost 风格（非高饱和主色块）

### 状态

已实现（Jira: [AI-340](https://felixzhu.atlassian.net/browse/AI-340)）
