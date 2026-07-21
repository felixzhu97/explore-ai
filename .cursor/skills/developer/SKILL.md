---
name: developer
description: Feature development for this repo — XP, DDD, BDD, TDD, Domain Glossary naming, Apple HIG minimal UX, and mandatory commit/PR standards (why body + References from official docs and research). Use when implementing features, writing tests, committing, opening PRs, UI work, or DDD/TDD/BDD/XP/clean-code tasks.
---

# Developer

**XP + DDD + BDD + TDD + minimal Clean Code.** Smallest correct change. UI: Apple HIG + minimal.

**Every** commit and PR must follow §5 (project standards). **Every** Jira ticket must follow [Product Owner](../product-owner/SKILL.md). Do not invent alternate formats.

## Hard constraints

1. Layers: [architecture](../../rules/architecture.mdc) — `web → application → domain ← infrastructure`
2. No `domain/port`, `adapter/in|out`, `*Port` in new code
3. Tests: `should_expectedResult_when_condition`
4. Names: Domain Glossary [Preferred Term](../../../docs/Domain-Glossary.md) + [clean-code-naming](references/clean-code-naming.md)
5. UI: Apple HIG + [apple-minimal-ux](references/apple-minimal-ux.md)
6. **Commit / PR / Jira / branches**: always reuse §5 + [Product Owner](../product-owner/SKILL.md); branch `<type>/AI-<key>` (type matches commit); References = official docs + research
7. **XP**: follow [extreme-programming](references/extreme-programming.md) — Simple Design / YAGNI, CI green, small releases, customer / AC feedback

## Workflow

```
XP (Customer + Small steps) → BDD → TDD → DDD (+ Clean Code) → Commit/PR (+ Jira via Product Owner skill)
(+ Apple HIG when touching UI)
```

Detail: [extreme-programming](references/extreme-programming.md)

### 1. Testing — BDD then TDD

Detail: [testing](references/testing.md)

**BDD:** one scenario, business language, Given / When / Then (outcomes, not framework calls). Align terms with Domain Glossary.

**TDD:** Red → Green → Refactor; AAA; no private-method tests; no I/O in unit tests.

| | Rule |
|--|------|
| Name | `should_expectedResult_when_condition` |
| Pyramid | Unit ~70% / Integration ~20% / E2E ~10% (few critical journeys) |
| Scope | Behavior, not implementation |
| Doubles | Fake/Stub for repos; Mock only when verifying interaction |
| Avoid | Over-mocking, weak asserts, ice-cream-cone E2E, ignored tests |

### 2. DDD

| Concept | Package |
|---------|---------|
| Entity / Aggregate | `domain/model/` — private ctor + factory; rich behavior |
| Value Object | `domain/vo/` — immutable `record` |
| Repository | `domain/repository/` → impl in `infrastructure/` |
| Use Case | `application/usecase/` — orchestration only |
| Web | `web/` — no business rules |

Detail: [ddd-rich-model](references/ddd-rich-model.md)

### 3. Naming

Glossary Preferred Term first → Clean Code form. No synonyms (`Conversation` vs `ChatSession`). New concept → update glossary in the same change.

Detail: [clean-code-naming](references/clean-code-naming.md)

### 4. UI — Apple HIG + minimal

Official: [HIG](https://developer.apple.com/design/human-interface-guidelines/). Clarity, deference, one primary action; no decorative noise.

Detail: [apple-minimal-ux](references/apple-minimal-ux.md)

### 5. Branches / Commit / PR (mandatory every time)

#### Branch naming

**Prefix = change type** (same set as commit types). Do **not** default every branch to `feat`.

| Type | Pattern | Example |
|------|---------|---------|
| feat | `feat/AI-<key>` | `feat/AI-113` |
| fix | `fix/AI-<key>` | `fix/AI-99` |
| refactor | `refactor/AI-<key>` | `refactor/AI-120` |
| docs | `docs/AI-<key>` | `docs/AI-159` |
| test | `test/AI-<key>` | `test/AI-88` |
| chore | `chore/AI-<key>` | `chore/AI-50` |
| perf | `perf/AI-<key>` | `perf/AI-77` |
| ci | `ci/AI-<key>` | `ci/AI-42` |
| (no ticket) | `<type>/<topic>` | `chore/update-deps` |

Allowed types: `feat` | `fix` | `refactor` | `docs` | `test` | `chore` | `perf` | `ci`

Rules:

- Branch prefix **must** match the primary change type
- With a Jira ticket: `<type>/AI-<key>` only — no extra slug suffix
- Without a ticket: `<type>/<topic>` (kebab-case)
- Do **not** use `feature/` for new branches (legacy only; CI still accepts it)
- Long-lived integration lines: `main`, `java-angular` (do not push work directly to these except via PR)

#### Branch / PR flow (Chain PRs)

```
main
 └── feat/AI-100          # PR #1 → base: main
      └── fix/AI-101      # PR #2 → base: feat/AI-100
```

1. First branch in a chain: create from `main` (or current integration line); PR **base** = `main`
2. Follow-up work in the same chain: create from the **previous branch**; PR **base** = that branch (not `main`)
3. Standalone work with no dependency: `<type>/…` from `main`, PR base = `main`; use the type that matches the change

#### Commit message

**Always** use this format. No alternate layouts.

1. One complete change per commit  
2. Subject ≤ 50 chars, imperative, no trailing period  
3. After the subject, add a **short why** (1–3 sentences)  
4. Always add **References** (see priority below)  
5. Never: `Co-authored-by`, `Made with`, emoji in subject  

#### References priority (required)

Prefer **specific** pages, not homepages. Search the web in real time when needed.

| Priority | Source | Where to look |
|----------|--------|----------------|
| 1 | Project dependency official docs | [dependency-docs](references/dependency-docs.md) |
| 2 | Vendor / lab **research** + open-source | [business-tech-analysis sources](../business-tech-analysis/references/sources.md) (research hubs + GitHub) |
| 3 | **arXiv** papers (abs page) | [arXiv](https://arxiv.org/) — when the change cites a method/paper |
| 4 | Standards / HIG | e.g. [Apple HIG](https://developer.apple.com/design/human-interface-guidelines/) |

Avoid: random blogs, undated tweets, marketing landing pages (unless no primary source exists — then note why).

#### AI / model reference set (required when relevant)

For model, benchmark, ASR / TTS / LLM, RAG, agent, or algorithm-related changes, the reference set must be more specific than a generic docs link.

When these source types exist, include all of them in both the commit and the PR:

1. One **academic** source, preferably the arXiv abs page or official paper page
2. One **Hugging Face** model, collection, or paper page
3. One official **vendor blog**, release note, or announcement page
4. The upstream **GitHub repository** or official implementation docs when they are the implementation source

For framework or dependency-only changes, keep using official docs first. For AI / model changes, prefer the full reference set above over a single docs link.

```
<type>: <short description>

<why: brief motivation for this change>

References:
- [Title](URL)
```

Types: `feat` | `fix` | `refactor` | `docs` | `test` | `chore` | `perf` | `ci`

Example:

```
 docs: add Qwen3-ASR reference guidance to PR skill

Contributors need a consistent citation set for model-related changes so commits and PRs point to the paper, release notes, distribution page, and upstream implementation.

References:
- [Qwen3-ASR Technical Report](https://arxiv.org/abs/2601.21337)
- [Qwen3-ASR - a Qwen Collection](https://huggingface.co/collections/Qwen/qwen3-asr)
- [Qwen3-ASR & Qwen3-ForcedAligner is Now Open Sourced](https://qwen.ai/blog?id=qwen3asr)
- [QwenLM/Qwen3-ASR](https://github.com/QwenLM/Qwen3-ASR)
```

PR body (no markdown headings — plain sections only):

```
This change updates the commit and PR skill so model-related work cites a full reference set instead of a single generic docs link. It makes AI-facing changes easier to review and trace back to the paper, release notes, distribution page, and source implementation.

References:
- [Qwen3-ASR Technical Report](https://arxiv.org/abs/2601.21337)
- [Qwen3-ASR - a Qwen Collection](https://huggingface.co/collections/Qwen/qwen3-asr)
- [Qwen3-ASR & Qwen3-ForcedAligner is Now Open Sourced](https://qwen.ai/blog?id=qwen3asr)
- [QwenLM/Qwen3-ASR](https://github.com/QwenLM/Qwen3-ASR)

Jira:
- https://felixzhu.atlassian.net/browse/AI-XXX
```

PR **References** must match the commit References (same links). Use the same official/research priority.

## Checklist

- [ ] Customer / AC outcome clear (XP Planning Game + On-site Customer)
- [ ] BDD scenario / AC covered
- [ ] TDD; test name `should_…_when_…`; Refactor while green
- [ ] YAGNI / Simple Design — no speculative extras
- [ ] Domain holds rules; use case orchestrates
- [ ] Glossary Preferred Terms; glossary updated if new concept
- [ ] UI (if any): HIG + minimal
- [ ] Branch: `<type>/AI-<key>` (type matches commit); Chain PR base correct
- [ ] Commit: subject + why + References (official/research)
- [ ] PR: plain body + same References + Jira link; chain base; CI green
- [ ] Jira (if any): [Product Owner](../product-owner/SKILL.md) template followed

## Related

| Need | Where |
|------|-------|
| Extreme Programming | [extreme-programming](references/extreme-programming.md) |
| Architecture | [architecture rule](../../rules/architecture.mdc) |
| Glossary | [Domain-Glossary](../../../docs/Domain-Glossary.md) |
| Testing core | [testing](references/testing.md) |
| Angular depth | [angular-developer](../angular-developer/SKILL.md) |
| Spring AI | [spring-ai](../spring-ai/SKILL.md) |
| Business / tech strategy | [business-tech-analysis](../business-tech-analysis/SKILL.md) |
| Research / OSS watchlist | [sources.md](../business-tech-analysis/references/sources.md) |
| Product Owner | [Product Owner](../product-owner/SKILL.md) |
