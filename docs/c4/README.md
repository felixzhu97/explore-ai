# C4 模型文档

本目录包含 AI-Test 平台的 C4 架构模型，使用 PlantUML 绘制。

## 文件说明


| 文件                              | 级别             | 描述                        |
| ------------------------------- | -------------- | ------------------------- |
| `context.puml`                  | C1 - Context   | 系统上下文图，展示用户、外部系统和核心服务的关系  |
| `container.puml`                | C2 - Container | 容器图，展示主要应用容器和技术栈          |
| `component-ai-agents.puml`      | C3 - Component | AI Agents 服务内部组件架构        |
| `component-frontend.puml`       | C3 - Component | Web 前端组件架构                |
| `component-vision-service.puml` | C3 - Component | Vision Service 组件架构       |
| `component-media-services.puml` | C3 - Component | TTS/Text/Media Gen 服务组件架构 |
| `component-rag-service.puml`    | C3 - Component | RAG 服务组件架构                |
| `wardley-map.puml`              | Wardley Map    | 技术演进地图                    |


## 本地库文件


| 文件                  | 描述              |
| ------------------- | --------------- |
| `C4.puml`           | C4-PlantUML 核心库 |
| `C4_Context.puml`   | Context 级别宏定义   |
| `C4_Container.puml` | Container 级别宏定义 |
| `C4_Component.puml` | Component 级别宏定义 |


## 查看方式

### 方式一：在线查看

1. 访问 [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
2. 将 `puml` 文件内容粘贴到编辑器中
3. 点击 "Submit" 渲染图表

### 方式二：本地渲染

```bash
cd docs/c4
plantuml -o png *.puml
```

生成的 PNG 文件将保存在 `png/` 目录中。

### 方式三：VS Code 插件

安装 "PlantUML" 扩展后，右键点击文件选择 "Preview"。

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户层 (People)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ ML 工程师   │  │ 运维工程师   │  │ 数据科学家   │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     前端层 (Containers)                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Web 应用 (React 18 + TypeScript)            │  │
│  │  AIHub │ AgentPanels │ TTSPanel │ VideoPanel             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     服务层 (Containers)                         │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │AI Agents     │ │ Vision       │ │ TTS          │           │
│  │Service       │ │ Service      │ │ Service      │           │
│  │(8003)        │ │ (8000)       │ │ (8004)       │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │ Text         │ │ RAG          │ │ Media Gen    │           │
│  │ Service      │ │ Service      │ │ Service      │           │
│  │(8002)        │ │ (8001)       │ │ (3456)       │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   AI Agents 内部 (Components)                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Supervisor Agent (路由协调)                   │   │
│  └──────────────────────────────────────────────────────────┘   │
│         │     │     │     │     │     │     │     │           │
│         ▼     ▼     ▼     ▼     ▼     ▼     ▼     ▼           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ K8s │ VectorDB │ RAG │ LLMOps │ AIOps │ TTS │ Video ... │   │
│  │ Agent│ Agent   │ Agent│ Agent │ Agent │Agent │ Agent     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   外部服务 (External Systems)                    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │ LLM Gateway │ │ Kubernetes   │ │ Monitoring   │           │
│  │(Ollama/GPT/ │ │              │ │(Prometheus/  │           │
│  │ Claude)     │ │              │ │ Grafana)     │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │ TTS          │ │ Video        │ │ Vector Store │           │
│  │ Providers    │ │ Providers    │ │ (Qdrant)     │           │
│  │(Azure/Google │ │(Kling/Runway │ │              │           │
│  │ ElevenLabs)  │ │ /Pika/Sora)  │ │              │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## 更新日志

### 2026-05-17

- 新增 `component-vision-service.puml` - Vision Service 组件架构
- 新增 `component-media-services.puml` - TTS/Text/Media Gen 服务组件架构
- 更新 `context.puml` - 添加 TTS、Text、Vision Service
- 更新 `container.puml` - 添加 TTS Agent、Video Agent 及相关工具
- 更新 `component-ai-agents.puml` - 添加 TTS Agent、Video Agent 及详细工具
- 更新 `component-frontend.puml` - 添加 TTS、Video 相关组件
- 添加本地 C4 库文件 (`C4.puml`, `C4_Context.puml`, `C4_Container.puml`, `C4_Component.puml`)

### 2026-05-17 (模型核对)

- 核对项目实际结构，补充缺失组件：
  - AI Agents: TTS Agent、Video Agent、TTS Tools、Video Tools
  - RAG Service: 持久化层 (Session Store, Document Cache, Metadata Store)
  - 更新各组件的工具详细功能列表
- 更新 `wardley-map.puml` - 全面更新技术演进地图：
  - Genesis: 添加 Multi-Agent Orchestration、RAG+Vector Search、Vision AI Pipeline
  - Custom Built: 添加 12 AI Agents 架构、TTS 语音合成、Video 生成服务、Feature Store
  - Product: 添加 Stable Diffusion、ElevenLabs/Coqui TTS、Ollama、Sentence Transformers
  - Commodity: 添加 Kubernetes、Redis、Prometheus/Grafana、gRPC、MLflow、Feast

