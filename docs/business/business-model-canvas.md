# ExploreAI Business Model Canvas（草案）

> Phase-0 · Jira [AI-227](https://felixzhu.atlassian.net/browse/AI-227) · 非法律/财务承诺，供产品与工程对齐

## 定位

面向小团队知识工作者的 **B2B AI 工作台**（Chat + RAG + Agent 模板），以 Workspace 协作与用量计费变现。

## 画布

| 模块 | 内容 |
|------|------|
| **客户细分** | 业务分析师、产品经理、5–50 人知识团队；后续 Enterprise（法务/合规采购） |
| **价值主张** | 浏览器即可用的多能力 AI 工作台；会话隔离与隐私控制；可扩展 Agent/RAG |
| **渠道** | 产品站 / Landing → 自助注册；内容与演示；后续合作伙伴 |
| **客户关系** | 自助 Onboarding；邮件支持；Pro+ 优先响应 |
| **收入** | Free（硬配额）/ Pro（席位+更高用量）/ Enterprise（SSO、DPA、审计） |
| **核心资源** | Spring AI 后端、Angular 前端、模型 API 预算、品牌与模板库 |
| **关键业务** | 模型调用、RAG、Agent 编排、计量计费、信任与合规 |
| **重要伙伴** | DeepSeek / OpenAI / Serper；Vercel / Railway；LaunchDarkly；Datadog |
| **成本结构** | LLM/搜索 API（主 COGS）、托管、可观测、支持 |

## 单位经济假设（待实测校准）

| 项 | 假设 |
|----|------|
| Free 日配额 | 50 次计费 API 调用 / Client 或 Workspace |
| Pro | $29/席位/月 或 $49/Workspace 含 5 席 + 更高日配额 |
| 目标毛利率 | ≥ 60%（模型成本可控前提下） |
| 超量 | 硬拒绝（429/402）+ 升级引导，避免透支 |

## 非目标（本期）

- 纯 C 端广告变现
- 自建大模型训练
- 多区域多活（先单区域 + 备份）

## References

- [Stripe Billing](https://docs.stripe.com/billing)
- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/)
- Epic: [AI-220](https://felixzhu.atlassian.net/browse/AI-220) 账号与 Workspace
