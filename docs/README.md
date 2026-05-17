# AI-Test Platform - 文档

企业级 AI 基础设施管理平台文档，提供多智能体协作、视觉 AI、RAG 文档问答等能力。

## 文档目录


| 文档                        | 说明                |
| ------------------------- | ----------------- |
| [快速开始](./QUICKSTART.md)   | 5 分钟快速上手          |
| [架构设计](./ARCHITECTURE.md) | 系统设计与组件概览         |
| [API 参考](./API.md)        | AI 服务 REST API 端点 |
| [开发指南](./DEVELOPMENT.md)  | 本地开发环境配置和工作流程     |
| [C4 模型](./c4/README.md)   | 系统架构 C4 模型图       |


## 项目概览

AI-Test Platform 是一个 TypeScript/Python monorepo 项目，整合了以下组件：

Wardley Map

### 8 个服务


| 服务                 | 技术栈                 | 功能                                 |
| ------------------ | ------------------- | ---------------------------------- |
| **AI Agents**      | FastAPI + LangGraph | 10 个专业智能体，多智能体编排                   |
| **RAG Service**    | FastAPI + Qdrant    | 文档检索增强生成                           |
| **Vision Service** | FastAPI + PyTorch   | YOLO、BLIP、PaddleOCR、SD、视频生成        |
| **Text Service**   | FastAPI             | GPT/Claude/Ollama 文本生成             |
| **TTS Service**    | FastAPI             | Azure/Google/ElevenLabs/Coqui 语音合成 |
| **Media Gen**      | FastAPI + Diffusers | 本地 Stable Diffusion 文生图            |
| **Web Frontend**   | React + Vite        | 用户界面，AIInfraPanel 统一面板             |
| **Server Backend** | Express.js          | 后端工具接口                             |


### 功能特性

#### AI Agents 多智能体系统


| 智能体           | 功能              | 使用场景                      |
| ------------- | --------------- | ------------------------- |
| Supervisor    | 多智能体协调器         | 任务路由和编排                   |
| K8s           | Kubernetes 集群管理 | Pod、Service、Deployment 管理 |
| Monitoring    | 监控系统            | 指标查询、告警配置                 |
| Model         | ML 模型管理         | 版本控制、部署、推理                |
| LLMOps        | LLM 运维          | 训练、微调、评估、模型部署             |
| AIOps         | 智能运维            | 事件分析、根因定位、日志分析            |
| VectorDB      | 向量数据库           | 嵌入管理、相似度搜索                |
| RAG           | 检索增强生成          | 文档问答、知识库                  |
| Pipeline      | 工作流编排           | 自动化流水线、任务调度               |
| Feature Store | 特征存储            | 特征工程管理、在线/离线特征            |


#### Vision AI


| 功能       | 模型                     | 使用场景        |
| -------- | ---------------------- | ----------- |
| 目标检测     | YOLO11n                | 识别并定位图像中的物体 |
| 图像描述生成   | BLIP                   | 生成自然语言图像描述  |
| OCR 文字识别 | PaddleOCR              | 从图像中提取文字    |
| 图像生成     | Stable Diffusion       | 文生图、图像变体    |
| 视频生成     | Sora/Pika/Runway/Kling | 文本/图像转视频    |


#### RAG 文档问答


| 功能    | 说明            |
| ----- | ------------- |
| 文档问答  | 基于上传的文档回答问题   |
| 语义搜索  | 在文档集合中查找相关内容  |
| 流式响应  | 实时令牌流输出       |
| 对话历史  | 基于会话的聊天历史记录   |
| 持久化存储 | 文档和向量持久化存储    |
| 缓存机制  | 高效的查询缓存       |
| 文档元数据 | 支持自定义文档标签和元数据 |


#### Text-to-Text 服务


| 功能   | Provider         | 说明                         |
| ---- | ---------------- | -------------------------- |
| 文本补全 | OpenAI GPT       | GPT-4o, GPT-3.5-turbo      |
| 对话生成 | Anthropic Claude | Claude Sonnet, Claude Opus |
| 本地模型 | Ollama           | qwen2.5, llama3.2          |


#### Text-to-Speech 服务


| Provider                 | 说明              |
| ------------------------ | --------------- |
| Azure Cognitive Services | 高质量神经网络语音，多语言支持 |
| Google Cloud TTS         | 自然语音，WaveNet 语音 |
| ElevenLabs               | 超逼真 AI 语音，情感控制  |
| Coqui TTS                | 开源本地 TTS，隐私保护   |


## 技术栈

### 前端

- React 18
- Vite
- TypeScript
- Emotion CSS-in-JS

### AI Agents

- Python / FastAPI
- LangChain / LangGraph
- Ollama (本地 LLM)

### RAG

- Qdrant 向量数据库
- LangChain
- Sentence Transformers

### Vision

- [Ultralytics YOLO](https://github.com/ultralytics/ultralytics)
- [HuggingFace BLIP](https://huggingface.co/Salesforce/blip-image-captioning-large)
- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)
- [Stable Diffusion](https://github.com/runwayml/stable-diffusion)

## 快速链接

- [根目录 README](../README.md) - 项目总览
- [API 文档](./API.md)
- [开发指南](./DEVELOPMENT.md)
- [C4 架构图](./c4/README.md)
- [Wardley 地图](./wardley-map.md) - 技术演进可视化
- [RAG 服务文档](../services/rag/README.md)
- [AI Agents 服务文档](../services/ai_agents/README.md)