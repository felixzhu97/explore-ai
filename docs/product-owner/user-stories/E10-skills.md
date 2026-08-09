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

## US-32 工作台控件与 Skills 一致

**As a** 访客  
**I want** Agents 与工作流的主要操作控件与 Skills 同一套安静样式  
**So that** 切换模块时不必重新辨认哪些按钮是主操作

### 验收标准

1. **Scenario** Agents 控件对齐 Skills  
   **GIVEN** 访客打开 Agents  
   **WHEN** 查看新建、模板与库操作  
   **THEN** 按钮为边框 / ghost 风格（非高饱和主色块）  
   **AND** 我的 Agents 为堆叠列表

2. **Scenario** 工作流画廊控件对齐 Skills  
   **GIVEN** 访客打开工作流画廊  
   **WHEN** 查看新建、使用与编辑  
   **THEN** 操作按钮权重与 Skills 一致

### 状态

已实现（Jira: [AI-340](https://felixzhu.atlassian.net/browse/AI-340)）
