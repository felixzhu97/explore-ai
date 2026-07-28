# ADR: No site-wide Signal Forms / httpResource (AI-252)

Status: Implemented  
Date: 2026-07-28

## Decision

Do **not** migrate the entire app to Signal Forms or `httpResource` in this phase. Expand coverage incrementally beyond AI-247/AI-248 pilots while leaving Chat and RAG inputs on existing patterns.

## Implemented (AI-252)

### httpResource — read-only GET

| File | Endpoint |
|------|----------|
| `src/main/web/app/agents/agents.page.ts` | `${API_BASE_URL}/agents/list` |
| `src/main/web/app/layout/components/sidebar-user-menu/sidebar-user-menu.component.ts` | `${API_BASE_URL}/account/me` |

### Signal Forms — settings surface

| File | Scope |
|------|-------|
| `src/main/web/app/privacy/privacy-preferences-form.component.ts` | Privacy analytics toggle + optional contact email |
| `src/main/web/app/privacy/privacy.page.ts` | Wires preferences form; Chat/RAG unchanged |

### Supporting changes

| File | Change |
|------|--------|
| `src/main/web/app/privacy/privacy-consent.storage.ts` | `contactEmail` + `writePrivacyPreferences` |
| `src/main/web/app/privacy/privacy-consent.service.ts` | `savePreferences` |
| `src/main/web/app/privacy/privacy.page.copy.ts` | Form copy strings |

### Tests

| File |
|------|
| `src/main/web/app/agents/agents.page.spec.ts` |
| `src/main/web/app/layout/components/sidebar-user-menu/sidebar-user-menu.component.spec.ts` |
| `src/main/web/app/privacy/privacy-preferences-form.component.spec.ts` |

## Still deferred (site-wide)

- Chat composer / provider selectors
- RAG document upload and query inputs
- Remaining service-layer `HttpClient.get` → `httpResource` migrations

## Revisit when

Pilot defect rate and migration cost from AI-247/AI-248/AI-252 are reviewed.

## References

- https://angular.dev/api/common/http/httpResource
- https://angular.dev/guide/forms/signals/overview
