# Code Quality References

Curated, language-agnostic references for engineering code quality into architecture decisions.
This document complements `books.md`, `patterns.md`, `antipatterns.md`, and `c4-model.md` by
focusing on **how to measure, enforce, and continuously improve** the quality attributes
that architecture ultimately delivers (maintainability, testability, evolvability,
reliability, observability).

> Use this reference when you need to:
> - Pick measurable quality attributes and define SLI/SLO-style targets.
> - Adopt a coding standard or design convention backed by a recognised authority.
> - Set up automated quality gates (lint, static analysis, mutation, coverage, SBOM).
> - Audit an existing codebase with a structured checklist.

---

## 1. Foundational Books (Authoritative Sources)

| Book | Authors | Why It Matters for Code Quality |
| --- | --- | --- |
| *Clean Code* | Robert C. Martin | Naming, functions, formatting, error handling, unit tests as design feedback. |
| *The Clean Coder* | Robert C. Martin | Professionalism, estimation, pressure handling, collaboration discipline. |
| *Clean Architecture* | Robert C. Martin | SOLID, component principles, separation of concerns at the architectural level. |
| *Refactoring (2nd ed.)* | Martin Fowler | Catalog of 200+ refactorings with mechanical steps and safety guarantees. |
| *Working Effectively with Legacy Code* | Michael Feathers | Techniques (sprout, wrap, seam) for bringing untested code under test. |
| *Code Complete (2nd ed.)* | Steve McConnell | Construction practices: variable naming, control flow, defensive programming. |
| *The Pragmatic Programmer (20th Anniversary ed.)* | Hunt & Thomas | DRY, orthogonality, tracer bullets, broken windows theory. |
| *Domain-Driven Design* | Eric Evans | Bounded context, ubiquitous language, modelling the heart of the business. |
| *Implementing Domain-Driven Design* | Vaughn Vernon | Concrete mapping of DDD tactical patterns to modern codebases. |
| *Software Architecture: The Hard Parts* | Ford, Richards, Sadalage, Dehghani | Trade-off analysis for distributed decisions (transactions, contracts, data). |
| *Building Evolutionary Architectures* | Ford, Parsons, Kua | Fitness functions: automated checks for architectural characteristics. |
| *Designing Data-Intensive Applications* | Martin Kleppmann | Reliability, scalability, maintainability of data systems. |
| *A Philosophy of Software Design* | John Ousterhout | Complexity definition (changes, cognitive load), "deep" vs "shallow" modules. |
| *Tidy First?* | Kent Beck | Coupling refactoring to behaviour change; calibration of when to tidy. |

> Full bibliographic entries live in [`./books.md`](./books.md).

---

## 2. Official Standards & Specifications

| Standard | Body | Scope |
| --- | --- | --- |
| [ISO/IEC 25010](https://iso25000.com/index.php/en/iso-25000-standards/iso-25010) | ISO/IEC | Product quality model: functional suitability, performance, compatibility, usability, reliability, security, maintainability, portability. |
| [ISO/IEC 5055](https://iso25000.com/index.php/en/iso-25000-standards/iso-2505) | ISO/IEC | Automated source-code quality measures (reliability, security, performance efficiency, maintainability). |
| [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) | OWASP | Verifiable security controls for web applications; a baseline for security quality. |
| [OWASP Top 10](https://owasp.org/www-project-top-ten/) | OWASP | Awareness document for the most critical web security risks. |
| [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/) | CIS | Hardening guides for OS, containers, cloud, language runtimes. |
| [SLSA](https://slsa.dev/) | OpenSSF | Supply-chain integrity levels for build and release artefacts. |
| [NIST SSDF (SP 800-218)](https://csrc.nist.gov/Projects/ssdf) | NIST | Secure Software Development Framework: practices and tasks. |
| [Semantic Versioning 2.0.0](https://semver.org/) | semver.org | Public API compatibility as a quality contract. |
| [Conventional Commits 1.0.0](https://www.conventionalcommits.org/) | conventionalcommits.org | Commit message grammar for automated changelog and SemVer. |
| [Keep a Changelog 1.1.0](https://keepachangelog.com/) | Olivier Lacan | Human-readable changelog conventions. |
| [12-Factor App](https://12factor.net/) | Heroku | Operational quality: config, disposability, dev/prod parity, logs. |
| [CNCF Cloud Native Definition v1.0](https://github.com/cncf/toc/blob/main/DEFINITION.md) | CNCF | Baseline capabilities expected of cloud-native systems. |

---

## 3. Reference Architectures & Open-Source Exemplars

Use these projects as **read-only reference points** for how mature teams structure
high-quality codebases. Clone locally only when you need to inspect; never vendor them.

### Java / JVM

| Project | Why It Is a Quality Reference |
| --- | --- |
| [Spring Boot Reference](https://github.com/spring-projects/spring-boot) | Auto-configuration conventions, modular layout, observability defaults. |
| [Moduliths](https://github.com/moduliths/moduliths) | Spring Modulith: enforces module boundaries and verifies them with tests. |
| [ArchUnit](https://github.com/TNG/ArchUnit) | Architecture-as-code unit tests for Java packages, layers, annotations. |
| [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) | Canonical layered sample (compare rich-model vs service-only variants). |
| [DDD-by-examples / Cargo Tracking](https://github.com/ctpconsulting/cargo-tracker) | Eric Evans-style reference domain in Java. |
| [eclipse-collections](https://github.com/eclipse/eclipse-collections) | High-quality library code: tests, immutability, API design. |

### TypeScript / Frontend

| Project | Why It Is a Quality Reference |
| --- | --- |
| [TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/intro.html) | Authoritative language reference and style guidance. |
| [Google TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html) | Industry-standard formatting and structural rules. |
| [Angular Architecture Guide](https://angular.dev/guide/architecture) | Standalone components, signals, DI, router patterns. |
| [Nx](https://github.com/nrwl/nx) | Monorepo boundaries, affected graph, enforced module rules. |
| [Effect-TS](https://github.com/Effect-TS/effect) | Typed errors, dependency management, testability in TypeScript. |
| [ts-pattern](https://github.com/gvergnaud/ts-pattern) | Exhaustive pattern matching as a safety net. |

### Cross-Language

| Project | Why It Is a Quality Reference |
| --- | --- |
| [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/) | Per-topic secure coding guidance across stacks. |
| [Prometheus Best Practices](https://prometheus.io/docs/practices/) | Naming, labels, alerting, exemplars for observable quality. |
| [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/) | Standardised telemetry names; cross-service searchability. |
| [CNCF Landscape](https://landscape.cncf.io/) | Tooling taxonomy for capability-driven selection. |

---

## 4. Quality Attributes & Metrics

Translate stakeholder concerns into measurable properties. Use this matrix as a
starting checklist; cut it to the four that matter most for your context.

| Attribute | Representative Metrics | Common Tooling |
| --- | --- | --- |
| **Correctness** | Failed assertions, contract-test coverage, mutation score | JUnit, Vitest, Stryker, Pact |
| **Reliability** | MTTR, MTBF, error budget burn, crash-free sessions | Prometheus, SLO dashboards |
| **Performance** | p50/p95/p99 latency, throughput, saturation | k6, Gatling, wrk, JMH |
| **Security** | CVE count, SAST findings, SBOM coverage, secrets-leak incidents | Trivy, Semgrep, Snyk, gitleaks |
| **Maintainability** | Cyclomatic complexity, cognitive complexity, duplication, dependency freshness | SonarQube, CodeQL, ESLint, Checkstyle |
| **Testability** | Test/line ratio, branch coverage, test execution time | JaCoCo, istanbul, Coverage.py |
| **Evolvability** | API breaking-change count, lead time for changes, deploy frequency | OpenAPI diff, Semgrep, DORA metrics |
| **Observability** | Log/metric/trace coverage, RED/USE dashboard completeness | OpenTelemetry, Grafana, Loki |
| **Portability** | Container build success, image size, cold start time | Docker, ko, Buildpacks |

### Quality-Gate Recipe (Reference)

A pragmatic gate for a medium-sized service:

1. **Format & lint** — pre-commit hook, zero warnings in CI.
2. **Static analysis** — Sonar/CodeQL quality gate (reliability, security, maintainability rating ≥ A).
3. **Architecture tests** — ArchUnit / dependency-cruiser rules enforced as tests.
4. **Unit tests** — line coverage ≥ 80% on domain layer, branch coverage ≥ 70% overall.
5. **Mutation tests** — Stryker/PIT score ≥ 60% on critical packages.
6. **Contract tests** — Pact or Spring Cloud Contract for cross-service APIs.
7. **SBOM + CVE scan** — Trivy/Grype on every build, no high/critical CVEs.
8. **Build reproducibility** — signed images, pinned base images, SLSA L3 provenance.
9. **Performance budget** — k6 scripts in CI with p95 thresholds.
10. **DORA dashboard** — weekly review of the four DORA metrics.

---

## 5. Coding Conventions (Cross-Cutting)

Adopt a documented, machine-checked convention. Treat it as a contract, not a style.

| Concern | Recommended Reference |
| --- | --- |
| Naming | Microsoft REST API Guidelines, Google Style Guides per language. |
| API design | [Microsoft REST API Guidelines](https://github.com/Microsoft/api-guidelines), [Google API Design Guide](https://cloud.google.com/apis/design), [OpenAPI Specification](https://spec.openapis.org/oas/latest.html). |
| Error model | [RFC 7807 Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807). |
| Logging | [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html), structured JSON with correlation IDs. |
| Configuration | 12-Factor: env vars, no secrets in code, no per-environment files. |
| Dependency hygiene | Dependabot/Renovate + `npm audit` / `gradle dependencies --refresh-dependencies` + signed releases. |
| Commit history | Conventional Commits, signed commits, linear history on protected branches. |
| Branching | Trunk-based development with short-lived feature branches (per Adam Tornhill, *Software Design X-Rays*). |
| Code review | Google's [Code Review Developer Guide](https://google.github.io/eng-practices/review/): small CLs, single reviewer of last resort, fast turnaround. |

---

## 6. Process & Culture References

Quality is a system property, not a hero property. These references help establish the
surrounding process.

| Topic | Reference | Takeaway |
| --- | --- | --- |
| Trunk-based development | [trunkbaseddevelopment.com](https://trunkbaseddevelopment.com/) | Short-lived branches, feature flags, continuous integration. |
| Continuous Integration | Humble & Farley, *Continuous Delivery* | Keep the build green; integrate at least daily. |
| Continuous Delivery | [continuousdelivery.com](https://continuousdelivery.com/) | Build → test → deploy pipeline as the only path to production. |
| Accelerate (DORA) | Forsgren, Humble, Kim | The four key metrics that predict organisational performance. |
| Team Topologies | Skelton, Pais | Stream-aligned / enabling / complicated-subsystem / platform teams. |
| Fitness functions | Ford et al., *Building Evolutionary Architectures* | Automated checks that assert architectural characteristics. |
| Code review at scale | [Microsoft Research: Code Review at scale](https://www.microsoft.com/en-us/research/publication/code-review-at-scale-faqs-for-authors-and-reviewers/) | Reviewer load, latency, and quality trade-offs. |
| Postmortem culture | [Etsy Debriefing Facilitation Guide](https://extfiles.etsy.com/DebriefingFacilitationGuide.pdf), [Google SRE Book Ch. 15](https://sre.google/sre-book/postmortem-culture/) | Blameless, learning-oriented incident review. |
| Threat modeling | [OWASP Threat Modeling Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Threat_Modeling_Cheat_Sheet.html) | STRIDE / LINDDUN at design time. |
| Architecture Decision Records | [adr.github.io](https://adr.github.io/) | Markdown template for recording significant decisions. |

---

## 7. Internal Workspace References

Files in this workspace that operationalise the principles above. Prefer them as the
first stop when answering "how do we do X here?".

| Path | What It Provides |
| --- | --- |
| [`/docs/architecture/`](../..) | Internal architecture decision records and diagrams (C4). |
| [`../../java-clean-architecture.mdc`](../../rules/java-clean-architecture.mdc) | Java-specific Clean Architecture implementation standards. |
| [`../../ddd-standards.mdc`](../../rules/ddd-standards.mdc) | Rich domain model, value object, aggregate, repository standards. |
| [`../../tdd-standards.mdc`](../../rules/tdd-standards.mdc) | Red-green-refactor workflow, test naming, mock policy. |
| [`../../bdd-standards.mdc`](../../rules/bdd-standards.mdc) | BDD Gherkin conventions for executable acceptance criteria. |
| [`../../clean-architecture.mdc`](../../rules/clean-architecture.mdc) | Cross-language Clean Architecture rules and dependency direction. |
| [`../spring-ai/SKILL.md`](../spring-ai/SKILL.md) | Spring AI implementation standards (uses this architecture as a substrate). |
| [`../software-testing/SKILL.md`](../software-testing/SKILL.md) | Companion skill: test pyramid, mutation testing, contract testing. |
| [`./clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) | Clean Architecture Domain layer detailed examples. |
| [`./clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) | Clean Architecture Application layer detailed examples. |
| [`./clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) | Clean Architecture Infrastructure layer detailed examples. |
| [`./clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) | Clean Architecture Interface Adapters layer detailed examples. |
| [`./clean-architecture-end-to-end.md`](./clean-architecture-end-to-end.md) | End-to-end Clean Architecture walkthrough. |

---

## 8. Anti-Quality Watchlist

Symptoms that usually indicate the architecture is degrading. Pair with
[`./antipatterns.md`](./antipatterns.md).

- **Rising cognitive complexity** in hot files (CodeQL/Sonar trend).
- **Coverage cliff** in new modules because they were "added quickly".
- **Flaky test rate** above 1% — erodes trust in the gate.
- **Long-lived branches** (age > 3 days) accumulating merge risk.
- **Dependency drift** — major version behind by N releases, security backlog piling up.
- **"Works on my machine" tickets** — environmental drift, not handled in code.
- **TODO/FIXME debt** — count growing quarter over quarter.
- **Domain leakage** — infrastructure imports reaching into domain packages (ArchUnit will catch it).
- **Test-time bloat** — unit test suite > 10 minutes; feedback delay kills TDD.
- **Reviewer fatigue** — PRs over 800 LOC reviewed in < 15 minutes.

---

## 9. Quick-Start Quality Tooling Matrix

| Layer | Java | TypeScript / Angular | Cross-cutting |
| --- | --- | --- | --- |
| Format | `spotless` (palantir) | Prettier + `eslint --fix` | pre-commit framework |
| Lint | Checkstyle, Error Prone, Sonar | ESLint, Angular ESLint | `markdownlint` |
| Type safety | Java 21+ records/sealed types, NullAway | `strict: true`, `noUncheckedIndexedAccess` | — |
| Architecture rules | ArchUnit | `eslint-plugin-boundaries`, `dependency-cruiser` | — |
| Test | JUnit 5, Mockito, AssertJ, PIT (mutation) | Jest, Vitest, Testing Library, Stryker | Testcontainers |
| Contract | Spring Cloud Contract, Pact | Pact | Pact Broker |
| Security | OWASP Dependency-Check, Snyk, Trivy | `npm audit`, Snyk, Semgrep | Trivy, Grype |
| Coverage gate | JaCoCo ≥ 80% | istanbul/v8 coverage | — |
| Performance | JMH, Gatling | k6, Playwright perf | k6 |
| Supply chain | Sigstore cosign, SLSA generator | `npm provenance` | in-toto, Witness |
| Observability | Micrometer → Prometheus | OpenTelemetry browser SDK | OpenTelemetry Collector |

---

## 10. How To Use This Document

1. **Picking a metric**: start from §4 (Quality Attributes) → choose at most four
   attributes → adopt the matching tooling row from §9.
2. **Justifying a convention**: cite §2 (Standards) or §5 (Coding Conventions) in the ADR.
3. **Picking a reference implementation**: search §3 by language; clone, read, return
   — do not vendor.
4. **Diagnosing decay**: run §8 (Anti-Quality Watchlist) as a quarterly review; pair
   with `antipatterns.md` for the architectural side.
5. **Onboarding a new engineer**: assign them §1 (Foundational Books) one at a time,
   and the §7 internal links as the workspace-specific overlay.

If a quality concern is not covered here, prefer adding a new entry rather than
embedding it elsewhere — keep this file as the single entry point for code-quality
references in the workspace.
