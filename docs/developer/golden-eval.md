# Golden Eval | Live quality regression

Runs OpenAI Evals–style JSONL cases against **real** Chat / RAG generation, then scores with Spring AI official [RelevancyEvaluator / FactCheckingEvaluator](https://docs.spring.io/spring-ai/reference/api/testing.html).

Default `./gradlew test` does **not** call paid LLM APIs for Golden Eval. Set `GOLDEN_EVAL_IT=true` only when you intend a live run.

## Artifacts

| Item | Path |
|------|------|
| Suites | `src/main/resources/eval/golden/chat.jsonl`, `rag.jsonl` |
| RAG fixtures | `src/main/resources/eval/golden/fixtures/` |
| IT | `src/test/java/com/ai/eval/GoldenEvalIT.java` |
| Orchestration | `GoldenEvalUseCase` (no public REST; test-only) |

## Run

```bash
set -a && source .env && set +a

# Separate H2 file so bootRun does not lock the default DB
export H2_URL='jdbc:h2:file:./data/explore-ai-golden;AUTO_SERVER=TRUE'
export GOLDEN_EVAL_IT=true

./gradlew test --tests 'com.ai.eval.GoldenEvalIT' --rerun-tasks
```

## Where to read the report

Stdout summary (also embedded in Gradle reports):

```text
Golden RAG: total=3 passed=3 failed=0 passRate=1.00
Golden CHAT: total=4 passed=1 failed=3 passRate=0.25
```

| Artifact | Location |
|----------|----------|
| JUnit XML | `build/test-results/test/TEST-com.ai.eval.GoldenEvalIT.xml` |
| HTML | `build/reports/tests/test/index.html` → class `GoldenEvalIT` |

## How to interpret

- Pass rate = share of cases where relevancy passes, and factuality passes when context is present (quality gate — not “intent accuracy”).
- RAG usually tracks retrieval grounding; Chat cases with `metadata.contexts` fact-check against that context **without** injecting it into generation — low Chat pass rate can be an expected product-knowledge signal.
- The IT asserts a valid report shape; it does **not** require `passRate == 1.0`.

Extend suites by appending JSONL lines (`id`, `input`, `ideal`, `metadata`). See [OpenAI Evals](https://github.com/openai/evals) format and Spring AI [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html).
