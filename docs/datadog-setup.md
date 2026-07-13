# Datadog Setup

Full-stack observability for ExploreAI on the **us5** Datadog site.

| Layer | Service name | Platform |
|-------|--------------|----------|
| Backend API | `explore-ai-api` | Railway |
| Frontend Web | `explore-ai-web` | Vercel |

## Console configuration

1. Open [Datadog us5](https://us5.datadoghq.com)
2. Create an **API Key** with `APM Write` and `Metrics Write` permissions
3. Create a **RUM Application** for the Angular frontend and copy:
   - Application ID → `DD_APPLICATION_ID`
   - Client Token → `DD_CLIENT_TOKEN`

## Deployment configuration

### Railway (backend)

Set on service `explore-ai`, environment `production`:

| Variable | Value |
|----------|-------|
| `DD_API_KEY` | Datadog API Key |
| `DD_SITE` | `us5.datadoghq.com` |
| `DD_SERVICE` | `explore-ai-api` |
| `DD_ENV` | `production` |
| `DD_VERSION` | `0.0.1-SNAPSHOT` (or release tag) |
| `DD_LOGS_INJECTION` | `true` |
| `DD_TRACE_SAMPLE_RATE` | `1.0` (lower in high-traffic environments) |
| `DD_METRICS_ENABLED` | `true` |

```bash
railway variable set DD_API_KEY=<api-key>
railway variable set DD_SITE=us5.datadoghq.com
railway variable set DD_SERVICE=explore-ai-api
railway variable set DD_ENV=production
railway variable set DD_LOGS_INJECTION=true
railway variable set DD_METRICS_ENABLED=true
```

The Docker image bundles `dd-java-agent` for agentless APM. Custom AI metrics (`ai.chat.*`, `ai.rag.*`, `ai.tool.calls`) are exported via Micrometer when `DD_METRICS_ENABLED=true`.

### Vercel (frontend)

Set on project `explore-ai`:

| Variable | Environments |
|----------|--------------|
| `DD_APPLICATION_ID` | Production, Preview |
| `DD_CLIENT_TOKEN` | Production, Preview |
| `DD_SITE` | `us5.datadoghq.com` |
| `DD_SERVICE` | `explore-ai-web` |
| `DD_ENV` | `production` |

Build injects credentials via `scripts/inject-datadog-env.mjs` before `ng build`.

## Local development

When Datadog variables are unset:

- Backend starts normally; Micrometer Datadog export stays disabled (`DD_METRICS_ENABLED=false`)
- Frontend skips RUM initialization when `applicationId` or `clientToken` is empty
- Prometheus (`/actuator/prometheus`) and Actuator health remain available locally

## MCP plugin vs application instrumentation

| Capability | Datadog MCP (Cursor plugin) | Application instrumentation |
|------------|----------------------------|----------------------------|
| Purpose | Query data from the AI agent | Continuous production monitoring |
| Setup | `/ddsetup` + OAuth in Cursor | Railway/Vercel env vars + code |
| Data | Logs, metrics, traces, dashboards | APM, RUM, custom metrics |

Both use the same Datadog organization but serve different workflows.

## Verification checklist

| Check | Where to verify |
|-------|-----------------|
| Backend APM | APM → Services → `explore-ai-api` |
| Log correlation | Logs contain `dd.trace_id` and link to traces |
| Custom metrics | Metrics Explorer → `ai.chat.requests` |
| Frontend RUM | RUM → Applications → `explore-ai-web` |
| End-to-end trace | Browser session trace links to backend span |
| MCP access | Cursor → `/ddconfig` → authenticate us5 site |

## References

- [Datadog MCP Server Setup](https://docs.datadoghq.com/mcp_server/setup/)
- [Java Tracing](https://docs.datadoghq.com/tracing/trace_collection/dd_libraries/java/)
- [Browser RUM](https://docs.datadoghq.com/real_user_monitoring/browser/)
- [Micrometer Datadog Registry](https://docs.micrometer.io/micrometer/reference/implementations/datadog.html)
