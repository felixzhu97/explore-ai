# Contributing

Thanks for your interest in Explore AI.

## Development

Requires Node.js 24.x (see `.nvmrc`) and [pnpm](https://pnpm.io/).

```bash
pnpm install
pnpm start
```

Useful checks:

```bash
pnpm typecheck
pnpm build
```

## Pull requests

1. Branch from `main` (or the active stack tip for dependent work).
2. Keep changes focused; keep domain and API names aligned with the
   [Glossary](docs/Glossary.md) when changing behavior or docs.
3. Ensure `pnpm typecheck` and `pnpm build` pass before opening a PR.
4. Open a PR against `main` with a clear description of why the change is
   needed.

## Code of Conduct

Participation is governed by our [Code of Conduct](CODE_OF_CONDUCT.md).
