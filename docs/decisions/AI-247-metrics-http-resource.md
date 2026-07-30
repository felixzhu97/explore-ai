# AI-247 — Metrics httpResource

Status: Implemented  
Type: Implementation

## Goal

Migrate Metrics read-only GET loaders from `resource({ loader })` to `httpResource`.

## Scope

- `MetricsOverviewPage`: overview, series, drilldown GET via `httpResource`
- `MetricsDomainPage`: domain snapshot, RAG document series, drilldown GET via `httpResource`
- Domain distribution chart derived from overview via `computed` (no duplicate overview fetch)

## Out of scope

- SSE and upload paths (unchanged)
- Site-wide httpResource migration (AI-252)

## References

- https://angular.dev/api/common/http/httpResource
- https://angular.dev/guide/signals/resource
