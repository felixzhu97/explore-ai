# ADR: No site-wide Signal Forms / httpResource (AI-252)

Status: Accepted (Won't / Deferred this phase)  
Date: 2026-07-28

## Decision

Do **not** migrate the entire app to Signal Forms or `httpResource` in this phase.

## Allowed now

- AI-247: Metrics read-only GET → `httpResource`
- AI-248: one settings/billing Signal Forms pilot

## Revisit when

Pilot defect rate and migration cost from AI-247/AI-248 are reviewed.

## References

- https://angular.dev/api/common/http/httpResource
- https://angular.dev/guide/forms/signals/overview
