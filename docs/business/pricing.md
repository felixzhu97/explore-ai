# ExploreAI 定价草案

> Phase-0 · [AI-227](https://felixzhu.atlassian.net/browse/AI-227) · 数字为草案，上线前按实测 COGS 调整

## 套餐

| | Free | Pro | Enterprise |
|--|------|-----|------------|
| **价格** | $0 | $29 / seat / mo（草案） | 定制 |
| **日调用配额** | 50 | 2 000 / seat | 定制 |
| **Workspace** | 1 人 | 多成员 | 多 Workspace / SSO |
| **RAG 存储** | 有限 | 提高限额 | 定制 / 私有化 |
| **Metrics 看板** | 无（需管理员密钥） | Workspace 内可见 | + 审计导出 |
| **SSO / DPA** | — | — | 有 |
| **支持** | 社区/文档 | 邮件 | 合同 SLA |

## 计费事件（计量）

计入配额的操作（草案）：

- Chat / SSE 文本生成
- RAG chat
- Agent / Tools 调用
- Image / TTS（按次，可设更高权重）

不计入：健康检查、静态前端、隐私擦除、只读会话列表（可选）。

## 超限行为

1. API 返回 `429`，错误码 `QUOTA_EXCEEDED`
2. UI 提示升级路径（Landing / Billing Portal — Stripe 后续 [AI-230](https://felixzhu.atlassian.net/browse/AI-230)）
3. 配额按 UTC 日窗口重置（可配置）

## 工程配置键（首期）

```yaml
app:
  billing:
    plan: free   # free | pro
    free-daily-requests: 50
    pro-daily-requests: 2000
  metrics:
    admin-api-key: ${METRICS_ADMIN_API_KEY:}
```

## References

- [Stripe usage-based billing](https://docs.stripe.com/billing/subscriptions/usage-based)
- [AI-221](https://felixzhu.atlassian.net/browse/AI-221) 用量计量与套餐计费
