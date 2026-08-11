# E5 MCP

← [用户故事地图](../User-Story-Map.md)

## 背景

用户可在对话中调用已接入的外部工具，使 AI 完成查天气、搜文档等超出纯文本的任务。

---

## US-12 MCP 工具调用

**As a** 最终用户  
**I want** 在对话中调用已接入的外部工具  
**So that** AI 能完成查天气、搜文档等超出纯文本的任务

### 验收标准

1. **Scenario** 对话中调用已接入工具
   **GIVEN** 系统已接入可用的外部工具  
   **WHEN** 用户在对话中提出需要工具完成的请求（如查天气、搜文档）  
   **THEN** AI 能调用相应已接入工具  
   **AND** 用户获得超出纯文本生成的任务结果

2. **Scenario** 未接入所需工具时不能伪造成功调用
   **GIVEN** 用户请求依赖尚未接入的工具能力  
   **WHEN** 用户在对话中发出该请求  
   **THEN** 系统不表现为已成功调用该未接入工具  
   **AND** 用户仍可看到对话层面的说明或普通回复

### 状态

已实现

---

## US-37 Plugin 目录与安装

**As a** 最终用户  
**I want** 在 Work 下的 Plugin 目录浏览精选与自定义连接、安装并启用/禁用  
**So that** 我能把内置与第三方工具按需接入对话，而无需理解底层协议细节

### 验收标准

1. **Scenario** 浏览目录与已安装
   **GIVEN** `module-mcp` 已开启  
   **WHEN** 用户打开 `/plugins`  
   **THEN** 看到 Installed、Featured 与分类列表  
   **AND** 内置 Explore AI Plugin 默认已安装

2. **Scenario** 安装 Featured / Custom
   **GIVEN** 用户提供 Streamable HTTP 端点与可选 API Key（内置除外）  
   **WHEN** 用户从目录安装  
   **THEN** 安装按 Owner 隔离保存  
   **AND** 可启用、禁用或移除（内置仅可禁用）

3. **Scenario** 启用后对话可用工具
   **GIVEN** Owner 已启用至少一个 Plugin  
   **WHEN** 用户在 Chat 中发消息  
   **THEN** 系统按 Owner 合并该 Plugin 的 Tools 供模型调用  
   **AND** 本地 `@Tool` 与 Plugin 工具重名时优先本地工具

### 状态

已实现
