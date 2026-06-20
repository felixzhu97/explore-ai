---
title: AI-Test Platform 战略演进地图
---

## Wardley Map

```mermaid
wardley-beta
title AI-Test Platform 战略演进地图
size [1200, 800]

%% ============ ANCHORS ============
anchor User [0.95, 0.90]
anchor External_API_Consumer [0.92, 0.85]

%% ============ 用户可见能力 (顶部) ============
component Agent_Chat [0.85, 0.25] : 多轮对话
component RAG_Chat [0.82, 0.35] : 知识库问答
component Vision_Analysis [0.78, 0.20] : 图像分析
component Image_Generation [0.75, 0.28] : AI 图像生成
component Speech_Synthesis [0.72, 0.30] : 语音合成
component AI_Ops_Dashboard [0.70, 0.40] : 智能运维

%% ============ 业务能力 (中部) ============
component Multi_Agent [0.60, 0.18] : Multi-Agent 编排
component MCP_Protocol [0.58, 0.22] : MCP 协议
component Session_Management [0.55, 0.45] : 会话管理
component Vector_Search [0.52, 0.38] : 向量检索
component Document_Processor [0.50, 0.42] : 文档处理
component Embedding_Service [0.48, 0.32] : Embedding 生成

%% ============ 基础设施 (中下部) ============
component LLM_Inference [0.40, 0.30] : LLM 推理
component Vector_DB [0.38, 0.55] : 向量数据库
component Doc_Storage [0.35, 0.60] : 文档存储
component Cache [0.32, 0.65] : 缓存服务

%% ============ 第三方服务 (Product - 右中) ============
component DeepSeek_API [0.30, 0.55] : DeepSeek API
component DALL_E_API [0.28, 0.50] : DALL-E API
component Edge_TTS_API [0.26, 0.52] : Edge TTS
component YOLO_Model [0.25, 0.48] : YOLO 检测
component PaddleOCR [0.24, 0.45] : PaddleOCR

%% ============ 框架 (Product - 右中) ============
component Angular_22 [0.35, 0.72] : Angular 22
component Spring_Boot [0.32, 0.75] : Spring Boot 3.5
component Spring_AI [0.30, 0.70] : Spring AI 2.0

%% ============ 基础设施技术 (Commodity - 最右) ============
component Kubernetes [0.20, 0.82] : Kubernetes
component Docker [0.18, 0.88] : Docker
component PostgreSQL [0.22, 0.78] : PostgreSQL 17
component Redis [0.20, 0.80] : Redis
component HTTP_Protocol [0.25, 0.90] : HTTP/WebSocket
component Prometheus [0.15, 0.85] : Prometheus/Grafana
component GitHub_Actions [0.12, 0.78] : GitHub Actions
component Java_25 [0.28, 0.92] : Java 25
component Node_js [0.30, 0.95] : Node.js

%% ============ 战略标注 ============
note "差异化竞争区\n投入研发资源" [0.45, 0.15]
note "优化选择区\n评估供应商" [0.35, 0.50]
note "标准化采购区\n成本优先" [0.20, 0.85]

%% ============ 依赖关系 ============
User -> Agent_Chat
User -> RAG_Chat
User -> Vision_Analysis
User -> Image_Generation
User -> Speech_Synthesis
User -> AI_Ops_Dashboard

Agent_Chat -> Multi_Agent
Agent_Chat -> Session_Management
Agent_Chat -> LLM_Inference

RAG_Chat -> Vector_Search
RAG_Chat -> Embedding_Service
RAG_Chat -> Document_Processor

Vision_Analysis -> LLM_Inference
Image_Generation -> LLM_Inference
Speech_Synthesis -> LLM_Inference
AI_Ops_Dashboard -> Multi_Agent

Multi_Agent -> MCP_Protocol
Vector_Search -> Vector_DB
Embedding_Service -> LLM_Inference
Document_Processor -> Doc_Storage
Session_Management -> Cache

LLM_Inference -> DeepSeek_API
Image_Generation -> DALL_E_API
Speech_Synthesis -> Edge_TTS_API
Vision_Analysis -> YOLO_Model
Vision_Analysis -> PaddleOCR

Angular_22 -> Node_js
Spring_Boot -> Java_25
Spring_AI -> Spring_Boot

Kubernetes -> Docker
PostgreSQL -> Docker
Redis -> Docker
Prometheus -> Kubernetes
GitHub_Actions -> Kubernetes

%% ============ 演化箭头 ============
evolve Multi_Agent 0.40
evolve MCP_Protocol 0.45
evolve Vector_Search 0.55
evolve LLM_Inference 0.50
```

## 战略决策指引

| 演化阶段 | 组件示例 | 战略决策 |
|---------|---------|----------|
| **Genesis (0-25%)** | Multi-Agent, MCP-Protocol | 差异化竞争，探索创新 |
| **Custom Built (25-50%)** | RAG-Chat, Vision-Analysis, Embedding | 内部构建，积累能力 |
| **Product (50-75%)** | DeepSeek-API, Spring-AI, Angular | 优化选择，评估供应商 |
| **Commodity (75-100%)** | Kubernetes, Docker, PostgreSQL | 标准化采购，成本优先 |

## 关键洞察

- **AIOps 面板**处于 Genesis 阶段，需要持续投入探索
- **向量检索**正在从 Custom Built 向 Product 演进
- **LLM 推理**高度依赖外部 API，需要考虑多 Provider 策略
- **基础设施**已高度标准化，无需重复造轮子
