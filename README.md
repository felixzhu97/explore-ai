# explore-ai

`explore-ai` is a full-stack platform for conversational AI. You can chat with multiple LLM providers, upload documents for RAG retrieval, call tools (weather, web search, datetime), and run Golden Eval regression checks against chat and RAG quality.

The backend uses Java 25, Spring Boot 4.1, and Spring AI 2.0; the frontend uses Angular 22. A native SwiftUI hello-world app (**AI**) lives under `src/main/ios`. Architecture follows `web → application → domain ← infrastructure`. See the [C4 model](docs/developer/c4-model/) and [Glossary](docs/Glossary.md) for boundaries and ubiquitous language.

Optional modules—agents, MCP, vision, audio, image generation, metrics, and more—are tracked in the [User Story Map](docs/product-owner/User-Story-Map.md).

**Live:** [https://www.felixzhu.chat](https://www.felixzhu.chat)

## Get started

### Requirements

You need JDK 25+, Node.js 20+, pnpm 8+, and Git. For local RAG embeddings, install [Ollama](https://ollama.com/) and run `ollama pull qwen3-embedding:0.6b` (re-ingest documents after changing embedding models).

### Initial install

Clone the repository, create your environment file, and load variables:

```bash
git clone https://github.com/felixzhu97/explore-ai.git
cd explore-ai
cp .env.example .env
# Edit .env — at minimum set DEEPSEEK_API_KEY
set -a && source .env && set +a
```

For a full walkthrough (API keys, Ollama, troubleshooting), see [Quick Start](docs/developer/QUICKSTART.md).

### Backend

Start the Spring Boot API:

```bash
./gradlew bootRun
# → http://localhost:9000
curl -s http://localhost:9000/actuator/health
```

### Frontend

In a second terminal:

```bash
cd src/main/web
pnpm install
pnpm start
# → http://localhost:4200
```

### Run your first chat

With the backend running:

```bash
curl -X POST http://localhost:9000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello"}'
```

Open [http://localhost:4200](http://localhost:4200) for the full UI. See [Quick Start](docs/developer/QUICKSTART.md) for session flows, RAG upload, and tool calling.

### Stop local services

Press `Ctrl+C` in each terminal running `bootRun` and `pnpm start`. To reset local H2 data, remove the directory configured by `H2_URL` (default under `./data/explore-ai`).

## Configuration

Environment variables override defaults in `src/main/resources/application.yml`. Do not commit real secrets.

- **`DEEPSEEK_API_KEY`** — required for the default chat and eval path
- **`OPENAI_API_KEY`** — optional OpenAI-compatible features
- **`SERPER_API_KEY`** — optional web search tool
- **`H2_URL`** — optional database URL override
- **`GOLDEN_EVAL_IT`** — set `true` to enable live Golden Eval integration tests ([guide](docs/developer/golden-eval.md))

See [`.env.example`](.env.example) for the full list.

## Next steps

- Take a guided setup in [Quick Start](docs/developer/QUICKSTART.md)
- Read the [API reference](docs/developer/api.md)
- Run [Golden Eval](docs/developer/golden-eval.md) (LLM-as-a-Judge regression)
- Browse the [C4 model](docs/developer/c4-model/) and [Glossary](docs/Glossary.md)
- Browse the AI [Guideline](docs/Guideline.md)
- Open the native iOS skeleton: [`src/main/ios`](src/main/ios) (scheme **AI**)
- Explore capabilities on the [User Story Map](docs/product-owner/User-Story-Map.md)
- Deploy: backend on [Render](https://render.com/docs/compute-plans) via [`render.yaml`](render.yaml); frontend on [Vercel](https://vercel.com) via [`vercel.json`](vercel.json) (proxies `/api/*` to Render)

Run unit tests with `./gradlew test`.

## Contributing

Contributions are welcome. Open an issue or pull request on [GitHub](https://github.com/felixzhu97/explore-ai). Keep domain and API names aligned with the [Glossary](docs/Glossary.md) when changing behavior or docs.

## Project status

`explore-ai` is under active development. Modules, APIs, and configuration may change before a 1.0 release.

## License

[MIT](LICENSE) © 2026 Felix
