# 指南

`explore-ai` 中 Chat、RAG、Tools、MCP、Agents、Eval、Metrics 与本地模型的构建与集成准则。

## 协议与平台 API

**以 Spring AI 作为对话、工具、RAG、MCP 与评估器的集成面。**

在其能力范围内基于官方参考实现，不自行对接各厂商 HTTP 客户端。详见 [Spring AI reference](https://docs.spring.io/spring-ai/reference/)。

**跨进程边界时，通过 MCP 暴露与消费工具。**

服务端与客户端工具契约遵循 Model Context Protocol。详见 [MCP](https://modelcontextprotocol.io/) 与 [规范](https://spec.modelcontextprotocol.io/)。

**助手输出使用 Server-Sent Events 流式返回。**

Chat 与 RAG 回复采用 SSE，让用户无需等待整段完成。事件形状保持稳定，并在 [API](developer/api.md) 中文档化。规范见 [WHATWG SSE](https://html.spec.whatwg.org/multipage/server-sent-events.html)。

**通过已文档化的 API 配置模型提供方。**

默认对话与评估使用 [DeepSeek](https://api-docs.deepseek.com/)。启用 OpenAI 兼容或 Anthropic 路径时遵循其官方文档。本地运行时使用 [Ollama](https://github.com/ollama/ollama)。

## Chat 与会话

**每个 Chat Session 归属明确的 Owner Key。**

会话与消息按浏览器或账号边界隔离；跨 Owner 访问返回未找到，界面回到新建对话。术语见 [Glossary](Glossary.md)。

**空会话不进入历史列表。**

仅有用户或助手实质内容后才持久化到侧栏 Recents；空消息体不展示为气泡。

**流式与记忆职责分离。**

流式传输负责呈现；会话记忆由 ChatMemory / 仓储边界持久化，不在 UI 层拼装权威历史。

## RAG

**在 RAG 中保持检索与生成分离。**

先检索文档分块，再基于上下文生成。经典框架：[Retrieval-Augmented Generation](https://arxiv.org/abs/2005.11401)。

**上传链路完成分块与嵌入后再标记就绪。**

Document 状态机（上传中 → 处理中 → 就绪 / 失败）对用户可见；失败可重试，不假装已可问答。

**不要混用嵌入空间。**

更换嵌入模型后须重新入库文档。向量维度与模型标识必须与存储 schema 一致。默认嵌入见 [Qwen3 Embedding](https://qwenlm.github.io/blog/qwen3-embedding/)。

**问答回答须可追溯到 Source Document。**

展示检索命中或等价引用线索，避免无出处的“知识库”空话。

## 工具、MCP 与 Agent

**工具型 Agent 围绕明确的推理—行动循环设计。**

工具契约清晰、步骤可观测。基础读物：[ReAct](https://arxiv.org/abs/2210.03629)、[Toolformer](https://arxiv.org/abs/2302.04761)。

**内置工具与 MCP 工具使用同一套可调用约定。**

天气、检索、日期时间等与 MCP 注册工具在命名、描述与错误返回上保持一致，便于 Chat 与 Pipeline 复用。

**Pipeline 画布表达的是有向执行图，而非自由闲聊。**

节点为可编辑的 Agent 副本（提示词与工具）；边表达依赖顺序。模版与运行结果分开展示。

**Skill 是可复用指令包，在 Chat 中显式应用。**

Skill 变更不隐式改写历史会话；应用范围对用户可见。

## Eval 与 Metrics

**用评估器衡量质量，而不是凭轶事判断。**

Golden Eval 使用 Spring AI 评估器与约定用例格式。评判可靠性：[Judging LLM-as-a-Judge](https://arxiv.org/abs/2306.05685)；实现参考：[Spring AI evaluation testing](https://docs.spring.io/spring-ai/reference/api/testing.html)、[OpenAI Evals](https://github.com/openai/evals)。

**用例以可版本化的输入与期望为主。**

扩展套件时追加结构化用例（如 JSONL：`id`、`input`、`ideal`、`metadata`），避免只靠手工截图验收。

**Metrics 记录可聚合的调用事实。**

按 domain、outcome、latency、token 等维度观测；看板与下钻对齐 Glossary 中的指标用语。

## 语音与图像

**语音与图像链路对齐各自领域的成熟方法。**

ASR 代表性路径：[Whisper](https://arxiv.org/abs/2212.04356)。图像生成代表性路径：[Latent Diffusion Models](https://arxiv.org/abs/2112.10752)。不混用不兼容的模型契约。

**TTS、ASR、图像生成与图像分析分路由呈现。**

Generation 壳层组织图像与 TTS；Vision / ASR 保持独立入口，避免把多模态能力塞进单一「AI 杂项」页。

## 模型与本地运行时

**将基于 Transformer 的解码视为对话与本地模型的基础。**

共享词汇（token、采样、上下文长度）见 [Attention Is All You Need](https://arxiv.org/abs/1706.03762) 与 Glossary Appendix D。

**优先选用有文档的开源模型与稳定打包格式。**

对话默认跟随 DeepSeek（[DeepSeek-V3](https://arxiv.org/abs/2412.19437)、[deepseek-ai](https://github.com/deepseek-ai)）。嵌入与多模态跟随 [Qwen](https://qwen.ai/)。发现与比较模型：[Hugging Face](https://huggingface.co/)；跟踪新工作：[arXiv cs.AI](https://arxiv.org/list/cs.AI/recent)。

**按用途使用 GGUF 与 ONNX。**

本地 LLM 权重：[GGUF](https://huggingface.co/docs/hub/en/gguf)。本地视觉引擎：[ONNX Runtime](https://onnxruntime.ai/)。

**冷启动与上下文长度对用户可感知。**

首次加载本地模型可能较慢；请求不得超过运行时声明的 context length。

## 文案约定

**产品语言使用 Chat、RAG、Tool Calling、MCP、Eval、Metrics。**

界面中若已有 Preferred Term，避免使用厂商品牌名（见 Glossary *Terms to Avoid*）。

**Spring AI、Ollama 与提供方名称仅出现在面向开发者的文档中。**

除非用户正在选择提供方，否则不把框架或厂商名放进终端界面。

**链接到活文档，而不是复制内容。**

上手：[Quick Start](developer/QUICKSTART.md)。契约：[API](developer/api.md)。回归：[Golden Eval](developer/golden-eval.md)。旅程：[User Story Map](product-owner/User-Story-Map.md)。术语：[Glossary](Glossary.md)。架构：[C4 模型](developer/c4-model/)。

## 相关资源

| 资源 | URL |
| ---- | --- |
| Spring AI 参考 | https://docs.spring.io/spring-ai/reference/ |
| Spring AI 评估测试 | https://docs.spring.io/spring-ai/reference/api/testing.html |
| MCP | https://modelcontextprotocol.io/ |
| MCP 规范 | https://spec.modelcontextprotocol.io/ |
| OpenAI 平台文档 | https://platform.openai.com/docs |
| OpenAI Evals | https://github.com/openai/evals |
| DeepSeek API | https://api-docs.deepseek.com/ |
| Anthropic 文档 | https://docs.anthropic.com/ |
| Ollama | https://github.com/ollama/ollama |
| SSE（WHATWG） | https://html.spec.whatwg.org/multipage/server-sent-events.html |
| Attention Is All You Need | https://arxiv.org/abs/1706.03762 |
| Retrieval-Augmented Generation | https://arxiv.org/abs/2005.11401 |
| ReAct | https://arxiv.org/abs/2210.03629 |
| Toolformer | https://arxiv.org/abs/2302.04761 |
| Judging LLM-as-a-Judge | https://arxiv.org/abs/2306.05685 |
| Whisper | https://arxiv.org/abs/2212.04356 |
| Latent Diffusion Models | https://arxiv.org/abs/2112.10752 |
| DeepSeek-V3 | https://arxiv.org/abs/2412.19437 |
| Qwen | https://qwen.ai/ |
| Qwen3 Embedding | https://qwenlm.github.io/blog/qwen3-embedding/ |
| Hugging Face | https://huggingface.co/ |
| GGUF | https://huggingface.co/docs/hub/en/gguf |
| ONNX Runtime | https://onnxruntime.ai/ |
| arXiv cs.AI | https://arxiv.org/list/cs.AI/recent |
