# AI-249 — Keep upload progress on XHR

Status: Implemented  
Type: Implementation

## Goal

Keep document upload progress on XHR; document engineering rule against pure Fetch backend for progress.

## Decision

Angular 22 defaults to `FetchBackend`, which **cannot** report upload progress. Any feature that shows upload progress (RAG document upload today) must keep the XHR backend via `provideHttpClient(withXhr(), …)`.

Do **not** replace `withXhr()` with `withFetch()` (or rely on the Fetch default) for endpoints wired to progress UI.

## Implementation

- `app.config.ts`: `provideHttpClient(withXhr(), withInterceptors(…))` with inline comment (AI-249).
- `rag.service.ts`: document upload uses `reportProgress: true` + `observe: 'events'`; progress updates `uploadStatuses`.
- Tests: `rag.service.spec.ts` uses `withXhr()` and asserts upload progress events.

## References

- [HttpClient](https://angular.dev/guide/http)
- [provideHttpClient](https://angular.dev/api/common/http/provideHttpClient)
