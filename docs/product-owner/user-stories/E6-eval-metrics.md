# E6 评估与指标

← [用户故事地图](../User-Story-Map.md)

## 背景

QA 可在 Eval 页自动评估 AI 回答质量；管理员/运维可查看请求量、延迟、错误率与域健康，并下钻到调用明细。

---

## US-13 Chat 质量评估

**As a** QA 工程师  
**I want** 在 Eval 页自动评估 AI 回答的质量  
**So that** 我可以量化对话效果并持续改进

### 验收标准

1. **Scenario** 在 Eval 页发起自动评估
   **GIVEN** QA 工程师打开 Eval 页  
   **WHEN** 触发对 AI 回答的质量评估  
   **THEN** 系统自动完成评估并展示结果  
   **AND** 结果可用于量化对话效果

2. **Scenario** 评估结果可用于对比改进
   **GIVEN** 至少一次评估已完成  
   **WHEN** QA 查看评估输出  
   **THEN** 可看到可比较或可记录的质量信息  
   **AND** 可用于后续改进判断

### 状态

已实现

---

## US-17 Chat / Document QA 黄金集回归（测试）

**As a** QA 工程师  
**I want** 用固定黄金集对 Chat 与 Document QA 的真实回答做回归评估  
**So that** 提示词或模型变更导致的质量回退能在发版前被发现  

### 验收标准

1. **Scenario** 运行 Chat 黄金集
   **GIVEN** Chat 黄金 JSONL 已就绪  
   **WHEN** 执行黄金评估集成测试  
   **THEN** 每条用例对真实生成结果给出通过/失败与评估反馈  
   **AND** 汇总通过率可用

2. **Scenario** 运行 Document QA 黄金集
   **GIVEN** RAG 黄金 JSONL 与 fixture 文档已就绪  
   **WHEN** 执行黄金评估集成测试  
   **THEN** 每条用例基于检索或参考上下文打分  
   **AND** 汇总通过率可用

### 状态

已实现

---

## US-14 AI 指标看板

**As a** 管理员 / 运维  
**I want** 查看 AI 请求量、延迟、错误率与域健康，并能下钻到调用明细  
**So that** 我可以快速定位异常调用并继续排查

### 验收标准

1. **Scenario** 查看聚合指标与域健康
   **GIVEN** 管理员 / 运维打开指标看板  
   **WHEN** 看板数据加载完成  
   **THEN** 可查看 AI 请求量、延迟、错误率与域健康相关信息

2. **Scenario** 下钻到调用明细
   **GIVEN** 看板已展示聚合指标  
   **WHEN** 用户下钻某一异常或关注项  
   **THEN** 可查看相关调用明细  
   **AND** 便于继续排查

### 状态

已实现

---

## US-36 Lab 与隐私页跟随界面语言

**As a** 访客  
**I want** Metrics、Eval、ASR、MCP 与隐私页的界面文案跟随已选语言  
**So that** 切换语言后 Lab 与隐私相关页面不再残留英文硬编码

### 验收标准

1. **Scenario** Lab 页文案随语言切换
   **GIVEN** 访客已选择界面语言（en / zh / ja / fr / es）  
   **WHEN** 打开 Metrics、Eval、ASR 或 MCP 页  
   **THEN** 标题、表单、状态、错误与表格列头等 UI 文案使用该语言  
   **AND** 品牌名 ExploreAI 保持不翻译

2. **Scenario** 隐私页完整本地化
   **GIVEN** 访客选择 ja / fr / es  
   **WHEN** 打开隐私页  
   **THEN** 告知、子处理方用途与控制文案为对应语言（非英文占位）  
   **AND** 子处理方品牌名（DeepSeek、OpenAI 等）保持原样

3. **Scenario** 共享 chrome 无障碍文案随语言
   **GIVEN** 访客已选择界面语言  
   **WHEN** 使用侧栏、顶栏菜单、语言选择器、RAG 删除文档或 TTS 播放控制  
   **THEN** 相关 aria-label 使用当前语言

### 状态

已实现（Jira: [AI-341](https://felixzhu.atlassian.net/browse/AI-341)）
