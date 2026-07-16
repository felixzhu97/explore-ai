---
name: developer
description: Feature development for this repo — DDD, BDD, TDD, Domain Glossary naming, Apple HIG minimal UX, and mandatory commit/PR standards (why body + References from official docs and research). Use when implementing features, writing tests, committing, opening PRs, UI work, or DDD/TDD/BDD/clean-code tasks.
---

# Developer

**DDD + BDD + TDD + minimal Clean Code.** Smallest correct change. UI: Apple HIG + minimal.

**Every** commit and PR must follow §5 (project standards). **Every** Jira ticket must follow [jira](../jira/SKILL.md). Do not invent alternate formats.

## Hard constraints

1. Layers: [architecture](../../rules/architecture.mdc) — `web → application → domain ← infrastructure`
2. No `domain/port`, `adapter/in|out`, `*Port` in new code
3. Tests: `should_expectedResult_when_condition`
4. Names: Domain Glossary [Preferred Term](../../../docs/Domain-Glossary.md) + [clean-code-naming](references/clean-code-naming.md)
5. UI: Apple HIG + [apple-minimal-ux](references/apple-minimal-ux.md)
6. **Commit / PR / Jira / branches**: always reuse §5 + [jira](../jira/SKILL.md); branch `feat/AI-<key>-slug`; References = official docs + research

## Workflow

```
BDD → TDD → DDD (+ Clean Code) → Commit/PR (+ Jira via jira skill)
(+ Apple HIG when touching UI)
```

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

| Kind | Pattern | Example |
|------|---------|---------|
| Feature (default) | `feat/AI-<key>-<short-slug>` | `feat/AI-113-session-archive` |
| Feature (no ticket yet) | `feat/<short-slug>` | `feat/multi-agent-orchestration` |
| Fix | `fix/AI-<key>-<short-slug>` | `fix/AI-99-cors-origins` |
| Chore / docs / test | `chore/…` `docs/…` `test/…` | `chore/ci-feat-glob` |

Rules:

- Prefer **kebab-case** slugs; no spaces or underscores in the slug
- Use Jira key when the work has a ticket (`AI-113`)
- Do **not** use `feature/` for new branches (legacy only; CI still accepts it)
- Long-lived integration lines: `main`, `java-angular` (do not push feature work directly to these except via PR)

#### Branch / PR flow (Chain PRs)

```
main
 └── feat/AI-100-base          # PR #1 → base: main
      └── feat/AI-101-extend   # PR #2 → base: feat/AI-100-base
```

1. First branch in a chain: create from `main` (or current integration line); PR **base** = `main`
2. Follow-up work in the same chain: create from the **previous feat branch**; PR **base** = that branch (not `main`)
3. Standalone small fix with no dependency: `feat/…` or `fix/…` from `main`, PR base = `main`

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

```
<type>: <short description>

<why: brief motivation for this change>

References:
- [Title](URL)
```

Types: `feat` | `fix` | `refactor` | `docs` | `test` | `chore` | `perf` | `ci`

Example:

```
feat: add session archive for inactive chats

Users need to archive stale sessions so the list stays focused on active work.

References:
- [Spring Data JPA - Reference Documentation](https://docs.spring.io/spring-data/jpa/reference/)
```

PR body (no markdown headings — plain sections only):

```
<1–3 sentences: what changed and why>

References:
- [Title](URL)

Jira:
- https://felixzhu.atlassian.net/browse/AI-XXX
```

PR **References** must match the commit References (same links). Use the same official/research priority.

## Checklist

- [ ] BDD scenario / AC covered
- [ ] TDD; test name `should_…_when_…`
- [ ] Domain holds rules; use case orchestrates
- [ ] Glossary Preferred Terms; glossary updated if new concept
- [ ] UI (if any): HIG + minimal
- [ ] Branch: `feat/AI-<key>-slug` (or fix/chore/…); Chain PR base correct
- [ ] Commit: subject + why + References (official/research)
- [ ] PR: plain body + same References + Jira link; chain base
- [ ] Jira (if any): [jira](../jira/SKILL.md) template followed

## Related

| Need | Where |
|------|-------|
| Architecture | [architecture rule](../../rules/architecture.mdc) |
| Glossary | [Domain-Glossary](../../../docs/Domain-Glossary.md) |
| Testing core | [testing](references/testing.md) |
| Angular depth | [angular-developer](../angular-developer/SKILL.md) |
| Spring AI | [spring-ai](../spring-ai/SKILL.md) |
| Business / tech strategy | [business-tech-analysis](../business-tech-analysis/SKILL.md) |
| Research / OSS watchlist | [sources.md](../business-tech-analysis/references/sources.md) |
| Jira | [jira](../jira/SKILL.md) |
