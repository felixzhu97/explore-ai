# Explore AI — agent guide

Conversational AI platform: multi-provider chat, RAG over uploaded
documents, tool calling, MCP, agents and Golden Eval regression checks.
Java 25 + Spring Boot 4.1 + Spring AI 2.0 backend, Angular 22 frontend,
SwiftUI hello-world app.

Start here, then follow links. This file is a map; detail lives in `docs/`.

## Map

| Path | What lives there |
|------|------------------|
| `src/main/java/com/ai/<feature>/` | Backend feature modules: `chat`, `rag`, `tools`, `mcp`, `agent`, `skill`, `eval`, `automation`, `vision`, `audio`, `image`, `account`, `billing`, … |
| `src/main/java/com/ai/common/` | Shared kernel (base entities, exceptions, LLM client factory) |
| `src/main/resources/` | `application*.yml`, prompts, agent / skill / pipeline templates, DB migrations |
| `src/test/java/com/ai/architecture/` | ArchUnit rules — the mechanical form of the hard rules below |
| `src/main/web/app/<feature>/` | Angular features (standalone components, Signals) |
| `e2e/` | Playwright smoke tests (boot backend + frontend automatically) |
| `src/main/ios/` | SwiftUI app |
| `docs/Glossary.md` | Ubiquitous language — name code after Preferred Terms |
| `docs/Guideline.md` | Product guideline: privacy, RAG, tools, eval, models |
| `docs/developer/c4-model/` | C4 views; domain model in `C4-Code-Domain-Model.puml` |
| `docs/developer/QUICKSTART.md` | Environment, API keys, Ollama, troubleshooting |
| `docs/developer/golden-eval.md` | Golden Eval datasets and thresholds |
| `docs/product-owner/` | User Story Map and stories |

## Commands

| Need | Command |
|------|---------|
| Verify everything (run before every commit) | `pnpm verify` |
| Backend only: format, style, tests, ArchUnit | `pnpm verify:server` |
| Frontend only: types, lint, unit tests | `pnpm verify:web` |
| Docs only: relative Markdown links | `pnpm verify:docs` |
| Apply Java formatting | `./gradlew spotlessApply` |
| Start backend + frontend in background, wait until healthy | `scripts/dev-up.sh` (`stop`, `status`) |
| Logs from `dev-up` | `logs/backend.log`, `logs/frontend.log` |
| Start backend (→ :9000) | `./gradlew bootRun` |
| Start frontend (→ :4200) | `pnpm start` |
| Playwright smoke | `pnpm e2e` |

Copy `.env.example` to `.env` and set at least `DEEPSEEK_API_KEY` before
running the app. `verify` needs no keys, network or running services.
With `APP_OAUTH_EXPLORE_IAM_ENABLED=true`, start `../explore-iam` (:9100)
first or the backend exits and the dev proxy answers 502.

## Hard rules (enforced by `verify`)

1. Per feature: `controller → service → domain ← infra`, with `mapper`
   between controller and service. Domain depends on no other layer.
2. Repository and gateway interfaces live in `domain/repository/`;
   implementations live in `infra/`.
3. No `usecase`, `adapter`, `port` packages; no `UseCase`, `Facade`, `Impl`,
   `Adapter`, `Port` class suffixes; no `I`-prefixed interfaces.
4. Controllers return named records, never `Map` or `Object`.
5. `@Id` / `@Version` only on `common.domain.model` base classes.
6. Time comes from an injected `Clock`; no `java.util.Date`,
   `Calendar`, `System.currentTimeMillis()`.
7. Never log prompts, message content, tokens, emails or IPs
   (`log-prompt: false` stays off).

When an ArchUnit rule fails, its message ends with `Fix:` — do that.
Never add lines to `src/test/resources/archunit_store/`; it only shrinks.

## Conventions

- Tests: `should <result> when <condition>` (`@DisplayName` with spaces;
  Java method names in camelCase).
- Domain language changes: update `docs/Glossary.md` and
  `C4-Code-Domain-Model.puml` before the code.
- Branch `<slug>` (no prefix); commit subject = PR title; body why +
  References; fine-grained Draft PRs via `gh stack`.
- UI follows Apple HIG; single accent `#0066cc`.

## Where to record work

- Multi-step plan: `docs/developer/exec-plans/` (see its README)
- Known debt: `docs/developer/tech-debt.md`
- Module health: `docs/developer/quality.md`

## Agents and skills

Team agent definitions in `.cursor/agents/` are synced from the
`public` meta-repo — edit them there, not here.
