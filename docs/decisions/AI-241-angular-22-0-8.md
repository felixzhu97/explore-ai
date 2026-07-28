# AI-241 — Pin Angular to 22.0.8

Status: Implemented  
Type: Implementation

## Goal

Pin `@angular/*` to 22.0.8 (or verified equivalent patch) to pick up NG0318-related fixes.

## What changed

- **dependencies**: `@angular/animations`, `common`, `compiler`, `core`, `forms`, `platform-browser`, `platform-browser-dynamic`, `router` pinned to **22.0.8** (from ^22.0.0 / 22.0.2 resolved).
- **dependencies**: `@angular/cdk` pinned to **22.0.6** (latest 22.0.x on npm; no 22.0.8 release for CDK).
- **devDependencies**: `@angular/build`, `@angular/cli`, `@angular/compiler-cli` pinned to **22.0.8** (from ^22.0.0 / 22.0.2–22.0.3 resolved).
- **lockfile**: `pnpm-lock.yaml` refreshed via `pnpm install`.

## Verification

- `pnpm test`
- `pnpm typecheck`

## Out of scope

- Full Angular major upgrade beyond 22.x
- Signal Forms / httpResource migrations (AI-247, AI-248)

## References

- https://github.com/angular/angular/releases/tag/22.0.8
- https://angular.dev/update-guide
