# Automations mail (Resend)

Production outbound mail uses **Resend HTTP API** (`ResendEmailGateway`), not SMTP.

## Render secrets (required after deploy)

`render.yaml` declares:

| Key | Blueprint | Action |
| --- | --------- | ------ |
| `APP_MAIL_ENABLED` | `true` | — |
| `APP_MAIL_PROVIDER` | `resend` | — |
| `APP_MAIL_FROM` | `sync: false` | Set in Dashboard to a **verified-domain** address |
| `APP_MAIL_RESEND_API_KEY` | `sync: false` | Set in Dashboard (never commit) |

Steps:

1. Verify sending domain in [Resend Domains](https://resend.com/domains) (SPF/DKIM).
2. Create/rotate API key in [Resend API Keys](https://resend.com/api-keys).
3. Render → **explore-ai** → **Environment** → set the two Secrets → **Save** → redeploy.
4. `GET /actuator/health` should be UP (`management.health.mail` is disabled).
5. Trigger an Automation due run; confirm in [Resend Emails](https://resend.com/emails).

Local: see [QUICKSTART](QUICKSTART.md) Step 3. Optional SMTP/Mailpit: `APP_MAIL_PROVIDER=smtp`.
