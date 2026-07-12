# LaunchDarkly Setup

Feature flags for optional modules are managed in [LaunchDarkly](https://launchdarkly.com/pricing/) (Developer tier: $0/month).

## Console configuration

1. Create project: `explore-ai`
2. Create environments: `development` (local), `production` (Railway/Vercel)
3. Create boolean flags (enable **Make available to client-side SDKs** for each):


| Flag Key           | Local default | Prod default |
| ------------------ | ------------- | ------------ |
| `module-vision`    | `true`        | `false`      |
| `module-audio-asr` | `true`        | `false`      |
| `module-mcp`       | `true`        | `false`      |
| `module-eval`      | `true`        | `false`      |


4. Copy credentials per environment:
   - **SDK Key** (server) → `LAUNCHDARKLY_SDK_KEY`
   - **Client-side ID** (browser) → `LAUNCHDARKLY_CLIENT_SIDE_ID`

## Deployment configuration

### Railway (backend)

Set on service `explore-ai`, environment `production`:

| Variable | Value |
|----------|-------|
| `LAUNCHDARKLY_ENABLED` | `true` (already set) |
| `LAUNCHDARKLY_SDK_KEY` | Production **SDK Key** from LaunchDarkly |

```bash
railway variable set LAUNCHDARKLY_SDK_KEY=<prod-sdk-key>
```

### Vercel (frontend)

Set on project `explore-ai`:

| Variable | Environments |
|----------|--------------|
| `LAUNCHDARKLY_CLIENT_SIDE_ID` | Production, Preview |

Build injects this via `scripts/inject-launchdarkly-env.mjs` before `ng build`.

## Runtime behavior

- **API / WebSocket / UI routes**: evaluated at request time via LaunchDarkly SDK; toggling flags does not require redeploy
- **Heavy beans** (ONNX models, whisper WebSocket handler): read flag value at startup; changing these flags requires application restart

## Offline fallback

When `LAUNCHDARKLY_SDK_KEY` is empty or `LAUNCHDARKLY_ENABLED=false`, the app uses `launchdarkly.fallback.*` values from `application.yml` / `application-prod.yml`.

## References

- [Java Server SDK](https://docs.launchdarkly.com/sdk/server-side/java/)
- [JavaScript Client SDK](https://docs.launchdarkly.com/sdk/client-side/javascript/)
