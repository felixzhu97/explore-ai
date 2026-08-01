# Catalog — Value Streams

> 负责人：Product Owner · ADM Phase B  
> as-is 主旅程；进行中 / 规划中的 Capability 不另开 Value Stream

## VS-01 Obtain Conversational Answer

| Stage ID | Stage name | 退出条件（简述） |
|----------|------------|------------------|
| VS01-S1 | Open Chat & Select Model | Provider / Model 可用或已选 |
| VS01-S2 | Ask & Stream Reply | 流式回复完成 |
| VS01-S3 | Continue Multi-turn | 追问保留上下文；侧栏可定位 |

Enabling L1：`BC-L1-01`（可选 `BC-L1-04` ASR、`BC-L1-05` 工具）

## VS-02 Grounded Knowledge Q&A

| Stage ID | Stage name | 退出条件（简述） |
|----------|------------|------------------|
| VS02-S1 | Provide Knowledge Source | 文档 / 图片就绪 |
| VS02-S2 | Ask Grounded Question | 已提问 |
| VS02-S3 | Receive Grounded Answer | 回答有据可查 |

Enabling L1：`BC-L1-03`（会话 `BC-L1-01`）

## VS-03 Multi-Agent Brief

| Stage ID | Stage name | 退出条件（简述） |
|----------|------------|------------------|
| VS03-S1 | Compose or Select Template | Pipeline 或模板就绪 |
| VS03-S2 | Run & Review Stages | 阶段可审阅 |
| VS03-S3 | Consume Standardized Brief | Fact / Inference / Recommendation |

Enabling L1：`BC-L1-07`
