---
title: AI-Test Platform Wardley Map
---

## Wardley Map

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
    'primaryColor': '#4A90D9',
    'primaryTextColor': '#fff',
    'primaryBorderColor': '#2E5090',
    'clusterBkg': '#f0f4f8',
    'clusterBorder': '#4A90D9'
}}}%%
flowchart LR
    %% ============ 用户锚点 (右上角) ============
    subgraph ANCHOR["🔵 用户锚点"]
        USER["用户"]
        EXTERNAL["外部 API 消费者"]
    end

    %% ============ 用户可见能力 (顶部) ============
    subgraph VISIBLE["📊 用户可见层"]
        direction LR
        AGENT["Agent 对话"] --> RAG["RAG 知识问答"]
        AGENT --> VISION["视觉分析"]
        AGENT --> IMG["图像生成"]
        AGENT --> TTS["语音合成"]
        AGENT --> AIOPS["AIOps 智能分析"]
    end

    %% ============ 业务能力层 ============
    subgraph BUSINESS["🏢 业务能力层"]
        direction LR
        MULTI["Multi-Agent 编排"]
        SESSION["会话管理"]
        DOC_PROC["文档处理"]
        VSEARCH["向量检索"]
        EMBED["Embedding 生成"]
        MCP["MCP 协议"]
    end

    %% ============ 数据处理层 ============
    subgraph DATA["💾 数据处理层"]
        direction LR
        LLM["LLM 推理引擎"]
        VDB["向量数据库"]
        DOC_S["文档存储"]
        CACHE["缓存服务"]
    end

    %% ============ 外部服务 ============
    subgraph EXTERNAL_SVC["🌐 外部 API 服务"]
        direction LR
        DS["DeepSeek API"]
        DALL["DALL-E API"]
        YTTS["Edge TTS"]
        YOLO["YOLO 检测"]
        OCR["PaddleOCR"]
    end

    %% ============ 框架 ============
    subgraph FRAMEWORK["🔧 开发框架"]
        direction LR
        NG["Angular 22"]
        SB["Spring Boot 3.5"]
        SA["Spring AI 2.0"]
    end

    %% ============ 基础设施 ============
    subgraph INFRA["🏭 基础设施 (标准化)"]
        direction LR
        K8S["Kubernetes"]
        DOCKER["Docker"]
        PG["PostgreSQL 17"]
        REDIS["Redis"]
        PROM["Prometheus/Grafana"]
        GHA["GitHub Actions"]
        JAVA["Java 25"]
        HTTP["HTTP/WebSocket"]
    end

    %% ============ 依赖关系 ============
    USER --> VISIBLE
    VISIBLE --> BUSINESS
    BUSINESS --> DATA
    BUSINESS --> EXTERNAL_SVC
    MULTI --> MCP
    SESSION --> CACHE
    VSEARCH --> VDB
    EMBED --> LLM
    DOC_PROC --> DOC_S
    RAG --> VSEARCH
    VISION --> YOLO
    VISION --> OCR
    LLM --> DS
    IMG --> DALL
    TTS --> YTTS
    SA --> SB
    NG --> HTTP
    SB --> JAVA
    VDB --> PG
    CACHE --> REDIS
    PROM --> K8S
    K8S --> DOCKER
    PG --> DOCKER
    REDIS --> DOCKER

    %% ============ 演化箭头 (红色虚线) ============
    MULTI -.->|"演化"| MCP
    VSEARCH -.->|"演化"| LLM
    EMBED -.->|"演化"| DS
    VISION -.->|"演化"| YOLO
    AIOPS -.->|"演化"| PROM
```

## Wardley Map 坐标位置对照

| 组件 | Visibility | Evolution | 说明 |
|------|-------------|-----------|------|
| **用户/外部消费者** | 0.90+ | 0.85+ | 右上角锚点 |
| Agent 对话、RAG | 0.75-0.85 | 0.20-0.35 | 顶部，Genesis-Custom |
| Multi-Agent、MCP | 0.55-0.65 | 0.15-0.25 | Genesis，需要创新 |
| LLM 推理、向量检索 | 0.40-0.50 | 0.30-0.40 | Custom，快速发展 |
| DeepSeek、DALL-E API | 0.30-0.40 | 0.50-0.60 | Product，供应商选择 |
| Spring Boot、Angular | 0.25-0.35 | 0.70-0.80 | Product，框架成熟 |
| Kubernetes、Docker | 0.10-0.20 | 0.85-0.95 | Commodity，标准化 |

## 战略决策矩阵

| 阶段 | X 范围 | 特征 | 战略选择 |
|------|--------|------|----------|
| **Genesis** | 0-25% | 全新、未知 | 构建差异化能力 |
| **Custom Built** | 25-50% | 定制、内部 | 内部建设，积累 |
| **Product** | 50-75% | 商业化 | 多供应商策略 |
| **Commodity** | 75-100% | 标准化 | 成本优化、自动化 |

## 关键战略洞察

1. **AIOps 智能分析** — 处于 Genesis 早期，需持续研发投入
2. **Multi-Agent 编排** — Genesis 向 Custom 演进中，核心竞争力
3. **向量检索 + LLM** — Custom 阶段，快速发展，需优化成本
4. **外部 API (DeepSeek/DALL-E)** — Product 阶段，评估 SLA，考虑多 Provider
5. **基础设施** — Commodity 阶段，标准化采购，持续自动化
