# Tech debt

Append a row when you take a shortcut; the PR that pays it removes the row.

| Item | Impact | Proposed fix | Added |
|------|--------|--------------|-------|
| `docs/Guideline.md` (249 lines) has no per-section owner | Hard to keep in sync with code | Split per feature or link sections from `AGENTS.md` only where needed | 2026-11-14 |
| 70 service / infra classes depend on `controller` types (DTOs, `OwnerContext`) | Domain and services are coupled to the HTTP contract | Move shared types to `service` or `domain`, map in `mapper/`; delete the line from `src/test/resources/archunit_store` as each one is fixed | 2026-11-14 |
