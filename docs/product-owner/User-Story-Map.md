---
title: AI Chat & Agent Platform - 用户故事地图
---

# 用户故事地图

按交付状态组织；每条仅保留用户故事（As a / I want / So that）。

## 已交付

### AI 对话

**As a** 最终用户  
**I want** 与 AI 助手进行流式对话  
**So that** 我可以快速获得智能回答

### Provider / Model 选择

**As a** 最终用户  
**I want** 进入 Chat 页时看到可用的 Provider 与 Model 列表  
**So that** 我可以选择模型且无错误提示

### 多轮对话与自动标题

**As a** 最终用户  
**I want** 在同一会话中连续对话并由系统自动生成会话标题  
**So that** 我可以记住上下文并在侧边栏快速找到历史会话

### 按浏览器隔离聊天会话

**As a** 使用 ExploreAI 的访客  
**I want** 我的聊天会话只对本浏览器可见  
**So that** 其他电脑无法看到或操作我的对话

### 欧盟隐私告知、同意与擦除控制

**As a** 使用 ExploreAI 的访客  
**I want** 看到隐私说明、选择分析 Cookie，并能擦除本浏览器数据  
**So that** 我了解处理目的并行使基本隐私控制

### RAG 知识问答

**As a** 最终用户  
**I want** 上传文档并基于文档内容提问  
**So that** AI 回答有据可查

### 图像生成

**As a** 最终用户  
**I want** 用文字描述生成图像  
**So that** 我可以快速获得可用的视觉素材

### 语音合成 TTS

**As a** 最终用户  
**I want** 将文本合成为语音并播放或下载  
**So that** 我可以听读 AI 输出的内容

### MCP 工具调用

**As a** 最终用户  
**I want** 在对话中调用已接入的外部工具  
**So that** AI 能完成查天气、搜文档等超出纯文本的任务

### MCP Host：按 Server 浏览 Tools/Resources/Prompts 并对外暴露三原语

**As a** 最终用户 / 平台开发者  
**I want** 在 `/mcp` 按已连接 Server 查看 Tools、Resources、Prompts，且本应用对外 MCP Server 真实暴露这些能力  
**So that** 对话与外部 Host 都能按业界标准使用 MCP

### Vision 多模态 RAG

**As a** 最终用户  
**I want** 上传图片并结合文档进行问答  
**So that** 我可以分析图表、截图等视觉内容

### 图像分析（独立）

**As a** 最终用户  
**I want** 在独立页面完成图像描述、目标检测与 OCR  
**So that** 无需进入 RAG 对话即可获得结构化视觉分析结果

### 流式 ASR 语音识别

**As a** 最终用户  
**I want** 通过麦克风实时转写语音为文字  
**So that** 我可以用语音输入与 AI 交互

### Chat 质量评估

**As a** QA 工程师  
**I want** 在 Eval 页自动评估 AI 回答的质量  
**So that** 我可以量化对话效果并持续改进

### AI 指标看板

**As a** 管理员 / 运维  
**I want** 查看 AI 请求量、延迟、错误率与域健康，并能下钻到调用明细  
**So that** 我可以快速定位异常调用并继续排查

### Multi-Agent Pipeline 工作台

**As a** 业务分析师  
**I want** 在画布上编排 Agent 流水线并流式审阅阶段与结果  
**So that** 我可以按企业流程定制多步 AI 工作流

### 内置企业工作流模板

**As a** 产品经理  
**I want** 一键选用预置编排模板  
**So that** 常见商业/技术研判无需从零搭图

### 企业研判标准化输出

**As a** 业务或技术负责人  
**I want** Analyst 以 Fact / Inference / Recommendation 结构输出 brief  
**So that** 研判结果可复核、可汇报

## 进行中

### 商业化底座：配额、法务页与账号雏形

**As a** ExploreAI 访客与运营方  
**I want** 日配额保护成本、法务文档可访问、Metrics 可按需上锁，并看到匿名账号状态  
**So that** 产品具备可商业化的最小计费与合规入口

### RAG ETL 管道

**As a** 平台工程师  
**I want** 通过可插拔的 ETL 端口处理不同格式文档  
**So that** 新增文档类型时无需修改应用层代码

### 文本分析

**As a** 管理员  
**I want** 对文本进行结构化情感分析  
**So that** 我可以了解用户反馈的情绪倾向

### Tools 天气查询

**As a** 开发者  
**I want** 通过充血领域模型封装天气查询逻辑  
**So that** 工具调用遵循领域建模最佳实践

### Supervisor 自动路由

**As a** 最终用户  
**I want** 描述任务后由 Supervisor 自动分派专家 Agent  
**So that** 我不必手动设计流水线

## 未来

### AIOps 智能运维

**As a** 管理员  
**I want** 监控系统健康、用自然语言查日志并收到告警  
**So that** 运维问题可被及时发现与处理

### 知识与制度

**As a** 企业员工  
**I want** 按权限查制度、使用入职知识包，并提醒过期文档  
**So that** 口径一致且知识不过期

### 沟通与内容生产

**As a** 知识工作者  
**I want** 会议纪要生成行动项、按受众起草公文，并做术语约束本地化  
**So that** 沟通可闭环且口径统一

### 决策支持与运营轻量

**As a** 业务或一线人员  
**I want** 竞品/供应商对比、客服答复草稿、合同要点与复盘初稿  
**So that** 决策与响应更快且可人工确认

### 治理与人机协同

**As a** 合规与平台管理员  
**I want** 审批节点、入模前脱敏、审计用量，以及 IM/邮件与定时触发  
**So that** AI 工作流可安全嵌入日常企业流程

### Spring AI Workflow 原语产品化

**As a** 平台工程师  
**I want** 在 Lab 中组合 Chain / Parallel / Route / Evaluator-Optimizer  
**So that** 无 Agent 注册表时也能编排 LLM 工作流

### 会话导出

**As a** 最终用户  
**I want** 导出历史会话内容  
**So that** 我可以归档或在站外继续使用对话结果
