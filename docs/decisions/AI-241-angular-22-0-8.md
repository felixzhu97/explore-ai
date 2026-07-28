# AI-241 — Pin Angular to 22.0.8

Status: Draft / In Progress  
Type: Implementation

## Goal

Pin `@angular/*` to 22.0.8 (or verified equivalent patch) to pick up NG0318-related fixes.

## Scope

- Update `package.json` / lockfile for `@angular/*` 22.0.8
- Run `npm test`, `npm run typecheck`, smoke e2e

## Out of scope

- Full Angular major upgrade beyond 22.x
- Signal Forms / httpResource migrations (AI-247, AI-248)

## References

- https://github.com/angular/angular/releases/tag/22.0.8
- https://angular.dev/update-guide
