# Project Dependency Reference

When referencing dependencies **or research** in commits or PRs, use official documentation and research URLs. Keep this list updated when adding new dependencies. Prefer specific documentation pages over homepages. For lab research / open-source hubs, also use [business-tech-analysis sources](../../business-tech-analysis/references/sources.md) and arXiv abs pages.

## AI / Model reference set

For model-driven changes such as ASR, TTS, LLM, RAG, agent, benchmark, or algorithm updates, do not stop at a single docs link.

When available, use this full reference set in both the commit and PR:

1. One **academic** source, preferably the arXiv abs page or official paper page
2. One **Hugging Face** model, collection, or paper page
3. One official **vendor blog**, release note, or announcement page
4. The upstream **GitHub repository** or official implementation docs when they are the implementation source

Example reference set:

- [Qwen3-ASR Technical Report](https://arxiv.org/abs/2601.21337)
- [Qwen3-ASR - a Qwen Collection](https://huggingface.co/collections/Qwen/qwen3-asr)
- [Qwen3-ASR & Qwen3-ForcedAligner is Now Open Sourced](https://qwen.ai/blog?id=qwen3asr)
- [QwenLM/Qwen3-ASR](https://github.com/QwenLM/Qwen3-ASR)

Open models (Qwen / DeepSeek / 智谱 GLM / Intern / Llama / Gemma / Mistral): [Open models](../../business-tech-analysis/references/sources.md#open-models). Research hubs (incl. 上海 AI Lab / 达摩院·通义): [Open-source & research hubs](../../business-tech-analysis/references/sources.md#open-source--research-hubs-required). Speech & image (Qwen3-ASR / FunASR / CosyVoice / FLUX / Whisper / Stable Diffusion): [Open-source speech & image](../../business-tech-analysis/references/sources.md#open-source-speech--image).

## Frontend

| Library | Version | Official Docs |
|---------|---------|---------------|
| Angular | ^22.0.0 | [Angular](https://angular.dev) |
| AnalogJS | ^2.6.0 | [AnalogJS](https://analogjs.org/) |
| PrimeNG | — | [PrimeNG](https://primeng.org/installation) |
| ngx-echarts | — | [ngx-echarts](https://xieziyu.github.io/ngx-echarts/api-doc/) |
| ngx-markdown | — | [ngx-markdown](https://github.com/jfcere/ngx-markdown) |
| Tailwind CSS | ^4.3.1 | [Tailwind CSS](https://tailwindcss.com/docs) |
| PostCSS | ^8.5.15 | [PostCSS](https://postcss.org/docs/) |
| RxJS | ~7.8.0 | [RxJS](https://rxjs.dev/api) |
| DOMPurify | ^3.4.9 | [DOMPurify](https://github.com/cure53/DOMPurify/blob/master/docs/README.md) |
| Marked | ^18.0.5 | [Marked](https://marked.js.org/using_pro) |
| Zone.js | ~0.15.0 | [Zone.js](https://github.com/angular/angular.js/tree/main/packages/zone.js) |
| Sass | ^1.100.0 | [Dart Sass](https://sass-lang.com/documentation/) |
| TypeScript | ~6.0.0 | [TypeScript](https://www.typescriptlang.org/docs/handbook/intro.html) |
| ESLint | — | [ESLint](https://eslint.org/docs/latest/) |
| Vitest | ^4.0.8 | [Vitest](https://vitest.dev/guide/) |
| pnpm | (managed) | [pnpm](https://pnpm.io/cli/install) |

## Backend

| Library | Version | Official Docs |
|---------|---------|---------------|
| Java | 25 | [Java](https://docs.oracle.com/en/java/) |
| Spring Boot | (managed) | [Spring Boot](https://spring.io/projects/spring-boot) |
| Spring AI | (managed) | [Spring AI](https://spring.io/projects/spring-ai) |
| Spring Data JPA | (managed) | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) |
| Spring Retry | 2.0.10 | [Spring Retry](https://docs.spring.io/spring-retry/docs/current/reference/html/) |
| Hibernate ORM | (managed) | [Hibernate ORM](https://hibernate.org/orm) |
| Liquibase | — | [Liquibase](https://www.liquibase.org) |
| Micrometer | (managed) | [Micrometer](https://micrometer.io) |
| Jackson | (managed) | [Jackson](https://github.com/FasterXML/jackson) |
| MapStruct | — | [MapStruct](https://mapstruct.org) |
| Lombok | (managed) | [Lombok](https://projectlombok.org) |
| Logbook | — | [Logbook](https://github.com/zalando/logbook) |
| H2 Database | (managed) | [H2 Database](https://www.h2database.com) |
| MySQL Connector/J | — | [MySQL Connector/J](https://dev.mysql.com/doc/connector-j) |
| PostgreSQL JDBC | (managed) | [PostgreSQL JDBC Driver](https://jdbc.postgresql.org) |
| Apache PDFBox | 3.0.3 | [Apache PDFBox](https://pdfbox.apache.org) |
| Hypersistence Utils | 3.10.0 | [Hypersistence Utils](https://docs.hypersistence.io/hypersistence-utils/) |
| JUnit | 1.20.4 | [JUnit](https://junit.org) |
| Testcontainers | 1.20.4 | [Testcontainers](https://www.testcontainers.org) |
| LangChain4j | — | [LangChain4j](https://langchain4j.dev) |
| DJL (Deep Java Library) | — | [DJL](https://djl.ai) |
| Apache OpenNLP | — | [Apache OpenNLP](https://opennlp.apache.org) |
| Apache Tika | — | [Apache Tika](https://tika.apache.org) |

## Build & Tooling

| Tool | Version | Official Docs |
|------|---------|---------------|
| Gradle | (wrapper) | [Gradle](https://gradle.org) |
| JaCoCo | 0.8.13 | [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/) |
| Error Prone | — | [Error Prone](https://errorprone.info) |
| Checkstyle | — | [Checkstyle](https://checkstyle.org) |
| Husky | — | [Husky](https://typicode.github.io/husky) |
| lint-staged | — | [lint-staged](https://github.com/okonet/lint-staged) |

## Learning References

| Resource | Official Docs |
|----------|---------------|
| Martin Fowler | [martinfowler.com](https://martinfowler.com/) |
| Clean Code / Robert C. Martin | [cleancoder.com](http://cleancoder.com/products) |
| Medium | [medium.com](https://medium.com) |
| dev.to | [dev.to](https://dev.to) |
| arXiv | [arxiv.org](https://arxiv.org) |
| Hugging Face | [huggingface.co](https://huggingface.co) |
| Agile Manifesto | [agilemanifesto.org](https://agilemanifesto.org/) |
| Google Ecosystem (claim → URL) | [§ Google Ecosystem](#google-ecosystem) below |

## Google Ecosystem

**Full Google ecosystem** (engineering, SRE, AI/research, Android, Cloud) — not Cloud-only. **UI design stays Apple HIG** (do not cite Material Design for product UI in this repo). Pick the row whose **Claim in why** matches the commit/PR why paragraph. Prefer deep links over homepages.

### Engineering practices & style

| Claim in why | Official doc |
|--------------|--------------|
| Code review / CL quality | [Google Engineering Practices — Code Review](https://google.github.io/eng-practices/review/) |
| Java formatting / naming / Javadoc conventions | [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) |
| TypeScript style (frontend when relevant) | [Google TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html) |
| JavaScript style (frontend when relevant) | [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html) |
| Style guide index (other languages) | [Google Style Guides](https://google.github.io/styleguide/) |

### SRE & production

| Claim in why | Official doc |
|--------------|--------------|
| SRE practices / reliability culture | [Site Reliability Engineering — sre.google](https://sre.google/) |
| Latency / traffic / errors / saturation (golden signals) | [SRE Book — Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/) |
| Eliminating toil / automation | [SRE Book — Eliminating Toil](https://sre.google/sre-book/eliminating-toil/) |

### Android / mobile (when relevant)

| Claim in why | Official doc |
|--------------|--------------|
| Android app architecture / quality | [Android Developers — Guide to app architecture](https://developer.android.com/topic/architecture) |
| AOSP / Android Java conventions | [AOSP Java code style](https://source.android.com/docs/setup/contribute/code-style) |

### AI & research (complements arXiv / Hugging Face sets)

| Claim in why | Official doc |
|--------------|--------------|
| Gemini / Google AI developer APIs | [Google AI for Developers](https://ai.google.dev/) |
| Google Research publications | [research.google](https://research.google/) · [Publications](https://research.google/pubs/) |

### Google Cloud (subset of ecosystem)

| Claim in why | Official doc |
|--------------|--------------|
| Secure / resilient / performant / cost-effective topology | [Google Cloud Well-Architected Framework](https://docs.cloud.google.com/architecture/framework) |
| Reliability as a design pillar | [Well-Architected — Reliability](https://docs.cloud.google.com/architecture/framework/reliability) |
| Performance optimization | [Well-Architected — Performance optimization](https://docs.cloud.google.com/architecture/framework/performance-optimization) |
| Cost / right-sizing | [Well-Architected — Cost optimization](https://docs.cloud.google.com/architecture/framework/cost-optimization) |
| SLOs, ops readiness, reduce toil | [Well-Architected — Operational excellence](https://docs.cloud.google.com/architecture/framework/operational-excellence) |
| Scalable / resilient app patterns (incl. golden signals) | [Patterns for scalable and resilient apps](https://docs.cloud.google.com/architecture/scalable-and-resilient-apps) |
| Java on Google Cloud | [Java on Google Cloud](https://cloud.google.com/java) |

## Design References

Product UI design for this repo: **Apple HIG only** (not Material).

| Resource | Official Docs |
|----------|---------------|
| Apple Human Interface Guidelines | [developer.apple.com/design](https://developer.apple.com/design/) |
| Apple HIG (guidelines hub) | [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/) |
| Tailwind CSS (implementation utility) | [tailwindcss.com/docs](https://tailwindcss.com/docs) |

## UX References

| Resource | Official Docs |
|----------|---------------|
| Apple HIG - Visual Design | [developer.apple.com/design/human-interface-guidelines/foundations/layout](https://developer.apple.com/design/human-interface-guidelines/foundations/layout) |
| Apple HIG - Typography | [developer.apple.com/design/human-interface-guidelines/foundations/typography](https://developer.apple.com/design/human-interface-guidelines/foundations/typography) |
| Apple HIG - Color | [developer.apple.com/design/human-interface-guidelines/foundations/color](https://developer.apple.com/design/human-interface-guidelines/foundations/color) |
| Apple HIG - Motion | [developer.apple.com/design/human-interface-guidelines/foundations/motion](https://developer.apple.com/design/human-interface-guidelines/foundations/motion) |
| Apple HIG - Accessibility | [developer.apple.com/design/human-interface-guidelines/foundations/accessibility](https://developer.apple.com/design/human-interface-guidelines/foundations/accessibility) |
| shadcn/ui - Apple Design | [shadcn/ui - Apple](https://www.shadcn.io/design/apple) |
| Tailwind - Animating with Tailwind | [tailwindcss.com/docs/animation](https://tailwindcss.com/docs/animation) |

## Jira

| Resource | URL |
|----------|-----|
| Jira Site | https://felixzhu.atlassian.net |
| Project AI (ExploreAI) | https://felixzhu.atlassian.net/projects/AI
