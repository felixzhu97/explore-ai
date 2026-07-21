# Spring AI 2.0 Reference Coverage Matrix

Baseline after `feat/AI-150` Draft PR. Official index: [Spring AI Reference](https://docs.spring.io/spring-ai/reference/).

| Guide area | Doc | Status in ExploreAI | AI-150 | Follow-up |
|------------|-----|---------------------|--------|-----------|
| ChatClient API | [chatclient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | Factory profiles + fluent call/stream | Done | — |
| Advisors | [advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html) | Memory, logger, AnswerAfterTools, RAG advisor | Done (used) | Advanced custom advisors |
| Structured Output | [structured-output](https://docs.spring.io/spring-ai/reference/api/structured-output.html) | `.entity` + native + validateSchema on routes/workflows | Done | Broader adoption audit |
| Native Structured Output | [native](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html) | Used on supervisor + workflows | Done | — |
| Effective Agents | [effective-agents](https://docs.spring.io/spring-ai/reference/api/effective-agents.html) | Five workflows + `/api/workflows` | Done | Pattern composition product UX |
| Chat Memory | Memory advisors / JDBC | JDBC window + configurable maxMessages + shared conversationId for RAG | Incremental | Bridge fidelity / multi-tenant memory |
| Tools | Tool calling | `@Tool` + MCP merge into TOOLS profile | Incremental | Tool observation advisors |
| MCP | MCP client/server | Starters present; workers get MCP via TOOLS profile | Incremental | Connection lifecycle / multi-server |
| RAG / Retrieval | RAG advisors | H2 `DocumentRetriever` + `RetrievalAugmentationAdvisor` | Incremental | Full VectorStore + ETL |
| Embeddings | Embeddings | Ollama embedding for RAG | Existing | [AI-179](https://felixzhu.atlassian.net/browse/AI-179) |
| VectorStore + ETL | Vector stores | Custom H2 only | N/A (kept) | [AI-180](https://felixzhu.atlassian.net/browse/AI-180) |
| Multimodality / Vision | Media on ChatClient | Vision module exists | Partial | [AI-181](https://felixzhu.atlassian.net/browse/AI-181) |
| Image generation | Image models | Generate/image module | Partial | [AI-181](https://felixzhu.atlassian.net/browse/AI-181) |
| Audio / TTS / ASR | Audio | Separate modules | Partial | [AI-182](https://felixzhu.atlassian.net/browse/AI-182) |
| Observability | Metrics/tracing | Micrometer present | Partial | [AI-183](https://felixzhu.atlassian.net/browse/AI-183) |
| Evaluation | Evaluators | Relevancy / FactChecking present | Partial | [AI-184](https://felixzhu.atlassian.net/browse/AI-184) |
| PromptTemplate | Prompting | Custom `PromptTemplates` | Partial | [AI-185](https://felixzhu.atlassian.net/browse/AI-185) |
| Testing | spring-ai-test | Dep present | Partial | [AI-186](https://felixzhu.atlassian.net/browse/AI-186) |
| Model starters detail | OpenAI/Ollama/Anthropic chapters | Multi-provider resolve | Partial | [AI-187](https://felixzhu.atlassian.net/browse/AI-187) |
| Guardrails / Safety | Safety extensions | Custom eval safety path | Partial | [AI-188](https://felixzhu.atlassian.net/browse/AI-188) |

## Follow-up Epic

- [AI-178](https://felixzhu.atlassian.net/browse/AI-178) — 对齐 Spring AI Reference 剩余 Guide（ExploreAI）

## Notes

- AI-150 does **not** claim full Reference coverage.
- Follow-up Epic [AI-178](https://felixzhu.atlassian.net/browse/AI-178) tracks remaining rows.
