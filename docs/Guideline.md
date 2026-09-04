# Guideline

Generative AI in `explore-ai` helps people chat, retrieve knowledge, call tools, run agents, evaluate quality, and work with local models. Use it to offer features that save time, improve communication, and unlock creativity—when those outcomes are clear and specific.

## Introduction

This guideline describes how to design and integrate Chat, RAG, Tools, MCP, Agents, Eval, Metrics, and local models in `explore-ai`. Prefer official documentation and primary research when changing AI behavior. Product vocabulary lives in the [Glossary](Glossary.md); architecture boundaries live in the [C4 model](developer/c4-model/).

## Best practices

**Design your experience responsibly.**

Responsible AI considers direct and indirect impacts on people, systems, and society. It is often easy to prototype an exciting AI feature, yet harder to create a robust experience that works in real situations. Small changes to inputs—or the same input given more than once—can produce very different outcomes. Orient design around experiences that are inclusive, crafted with care, and protective of privacy.

**Keep people in control.**

Respect people’s agency. Honor in-scope requests when the expected output is clear, handle sensitive content carefully, and let people dismiss, retry, or revert results they do not want. Clearly identify when and where the product uses AI. In Chat and Pipeline, prefer visible actions—send, stop, retry, apply Skill—over silent automation.

**Offer generative features only where they provide clear value.**

Generative AI is powerful, but it is not the right solution for every situation. Prefer Chat, RAG, tools, and agents when they deliver time savings, grounded answers, or creative leverage. Avoid adding AI because it is new or expected.

**Ensure a useful experience when generative features are unavailable.**

People may lack API keys, opt out of a provider, or run without a local model. Keep non-AI paths usable where reasonable—document browsing, empty states, and clear setup guidance—so the product does not feel broken when generation cannot run.

**Score quality with evaluators, not anecdotes.**

Golden Eval uses Spring AI evaluators and versioned cases so quality stays measurable as prompts and models change. See [Judging LLM-as-a-Judge](https://arxiv.org/abs/2306.05685) and [Spring AI evaluation testing](https://docs.spring.io/spring-ai/reference/api/testing.html).

## Transparency

**Communicate where the product uses AI.**

Letting people know when AI is involved sets expectations and lets them choose knowingly. Never present model output as if it were authored by a human without disclosure.

**Set clear expectations about what a feature can and cannot do.**

Clarify capabilities and limits so people build an accurate mental model—for example, RAG answers depend on uploaded documents; tools only run when registered and permitted.

**Surface sources for grounded answers.**

When RAG retrieves context, show Source Document hits or equivalent citations. Avoid ungrounded “knowledge base” claims.

## Privacy

**Isolate every Chat Session with an Owner Key.**

Sessions and messages stay within browser or account boundaries. Cross-owner access returns not found; the UI returns to a new chat. Terms: [Glossary](Glossary.md).

**Ask only for the data a feature needs.**

Provider keys, documents, and optional account linking stay purpose-bound. Offer clear ways to revoke access or erase data where the product supports privacy controls.

**Disclose how models and providers use shared information.**

People are more comfortable when they understand where prompts and documents go—local Ollama versus a remote provider—before they commit sensitive content.

## Protocols and platform APIs

**Use Spring AI as the integration surface for chat, tools, RAG, MCP, and evaluators.**

Stay within documented APIs instead of custom provider HTTP clients. See the [Spring AI reference](https://docs.spring.io/spring-ai/reference/).

**Follow Model Context Protocol for cross-process tools.**

Server and client tool contracts follow MCP. See [MCP](https://modelcontextprotocol.io/) and the [specification](https://spec.modelcontextprotocol.io/).

**Stream assistant output with Server-Sent Events.**

Chat and RAG replies use SSE so people see progress without waiting for a full completion. Keep event shapes stable and documented in the [API](developer/api.md). See [WHATWG SSE](https://html.spec.whatwg.org/multipage/server-sent-events.html).

**Configure providers through documented APIs.**

Default chat and eval use [DeepSeek](https://api-docs.deepseek.com/). OpenAI-compatible and Anthropic paths follow their official docs when enabled. Local runtimes use [Ollama](https://github.com/ollama/ollama).

## Chat and sessions

**Persist history for non-empty sessions only.**

Add sessions to Recents only after substantive user or assistant content. Empty message bodies do not render as bubbles.

**Separate streaming from memory.**

Streaming is presentation. Session memory persists through ChatMemory and repository boundaries—not as authoritative history assembled in the UI.

**Make it easy to refine or retry generated replies.**

Prefer stop, regenerate, and edit flows near assistant content so people stay in charge of the conversation.

## Retrieval-augmented generation

**Keep retrieval and generation separate.**

Retrieve document chunks, then generate with grounded context. Classic framing: [Retrieval-Augmented Generation](https://arxiv.org/abs/2005.11401).

**Mark documents ready only after chunking and embedding.**

The Document state machine (uploading → processing → ready / failed) stays visible. Failures are retryable; do not pretend Q&A is available early.

**Do not mix embedding spaces.**

After changing an embedding model, re-ingest documents. Vector dimensions and model identity must match the store schema. Default embeddings: [Qwen3 Embedding](https://qwenlm.github.io/blog/qwen3-embedding/).

## Tools, MCP, and agents

**Design tool-using agents around clear reason–act loops.**

Tool contracts stay explicit and steps observable. Readings: [ReAct](https://arxiv.org/abs/2210.03629), [Toolformer](https://arxiv.org/abs/2302.04761).

**Share callable conventions across built-in and MCP tools.**

Weather, search, datetime, and MCP-registered tools share naming, description, and error shape so Chat and Pipeline can reuse them.

**Treat the Pipeline canvas as a directed execution graph.**

Nodes are editable Agent copies (prompt and tools); edges express dependency order. Templates and run results stay separate.

**Apply Skills as reusable instruction packs.**

Skills are explicit in Chat. Edits do not silently rewrite history; scope stays visible to people.

**Confirm before irreversible or high-impact tool actions.**

When a tool can send mail, delete data, or trigger automation, prefer clear confirmation over silent side effects.

## Evaluation and metrics

**Prefer versioned inputs and expected outputs.**

Extend suites with structured cases (for example JSONL: `id`, `input`, `ideal`, `metadata`). Do not rely on screenshots alone. Case format: [OpenAI Evals](https://github.com/openai/evals).

**Record aggregatable invocation facts in Metrics.**

Observe domain, outcome, latency, and tokens. Dashboards and drill-down use Glossary metric language.

**Let people and developers improve quality over time.**

Eval regressions and Metrics trends guide prompt, model, and tool changes—treat them as continuous improvement signals, not one-off demos.

## Speech and imagery

**Align each modality with mature methods.**

ASR lineage: [Whisper](https://arxiv.org/abs/2212.04356). Image generation lineage: [Latent Diffusion Models](https://arxiv.org/abs/2112.10752). Do not mix incompatible model contracts.

**Keep separate routes for TTS, ASR, image generation, and image analysis.**

The Generation shell organizes image and TTS. Vision and ASR keep their own entries—avoid a single catch-all multimodal page.

**Factor processing time into the experience.**

Generative work can take longer than ordinary UI actions. Prefer specific status (for example summarizing or embedding) over a vague “Processing…” label, and allow cancel where streaming supports it.

## Models and local runtimes

**Treat Transformer decoding as the foundation for chat and local models.**

Shared vocabulary (tokens, sampling, context length): [Attention Is All You Need](https://arxiv.org/abs/1706.03762) and Glossary Appendix D.

**Prefer documented open models and stable packaging.**

Chat defaults follow DeepSeek ([DeepSeek-V3](https://arxiv.org/abs/2412.19437), [deepseek-ai](https://github.com/deepseek-ai)). Embeddings and multimodal families follow [Qwen](https://qwen.ai/). Discover models on [Hugging Face](https://huggingface.co/); scan new work on [arXiv cs.AI](https://arxiv.org/list/cs.AI/recent).

**Use GGUF for local LLM weights and ONNX for local vision.**

See [GGUF](https://huggingface.co/docs/hub/en/gguf) and [ONNX Runtime](https://onnxruntime.ai/).

**Make cold start and context length visible.**

First load of a local model may be slow. Requests must not exceed the runtime’s declared context length.

## Product language

**Use Preferred Terms in product UI: Chat, RAG, Tool Calling, MCP, Eval, Metrics.**

Avoid vendor brand names in the UI when a Preferred Term exists (Glossary *Terms to Avoid*).

**Keep framework and provider names in developer documentation.**

Keep Spring AI, Ollama, and provider labels out of end-user chrome unless the person is choosing a provider.

## Related

[Apple HIG — Generative AI](https://developer.apple.com/design/human-interface-guidelines/generative-ai)

[Attention Is All You Need](https://arxiv.org/abs/1706.03762)

[Retrieval-Augmented Generation](https://arxiv.org/abs/2005.11401)

[ReAct](https://arxiv.org/abs/2210.03629)

[Toolformer](https://arxiv.org/abs/2302.04761)

[Judging LLM-as-a-Judge](https://arxiv.org/abs/2306.05685)

[Whisper](https://arxiv.org/abs/2212.04356)

[Latent Diffusion Models](https://arxiv.org/abs/2112.10752)

[DeepSeek-V3](https://arxiv.org/abs/2412.19437)

[Model Context Protocol](https://modelcontextprotocol.io/)

[MCP specification](https://spec.modelcontextprotocol.io/)

[Hugging Face](https://huggingface.co/)

[arXiv cs.AI](https://arxiv.org/list/cs.AI/recent)

[Qwen3 Embedding](https://qwenlm.github.io/blog/qwen3-embedding/)

[OpenAI Evals](https://github.com/openai/evals)

## Developer documentation

[Spring AI reference](https://docs.spring.io/spring-ai/reference/)

[Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)

[Spring AI Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)

[Spring AI Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)

[Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)

[Spring AI Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)

[Spring AI MCP](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html)

[Spring AI MCP Client](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)

[Spring AI evaluation testing](https://docs.spring.io/spring-ai/reference/api/testing.html)

[DeepSeek API](https://api-docs.deepseek.com/)

[Ollama](https://github.com/ollama/ollama)

[OpenAI platform docs](https://platform.openai.com/docs)

[Anthropic docs](https://docs.anthropic.com/)

[Glossary](Glossary.md)

[Quick Start](developer/QUICKSTART.md)

[API](developer/api.md)

[Golden Eval](developer/golden-eval.md)
