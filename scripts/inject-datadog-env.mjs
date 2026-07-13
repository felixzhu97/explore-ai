import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const envPath = join(root, 'src/main/web/environments/environment.prod.ts');

const applicationId = process.env.DD_APPLICATION_ID?.trim() ?? '';
const clientToken = process.env.DD_CLIENT_TOKEN?.trim() ?? '';
const site = process.env.DD_SITE?.trim() || 'us5.datadoghq.com';
const service = process.env.DD_SERVICE?.trim() || 'explore-ai-web';
const env = process.env.DD_ENV?.trim() || 'production';

if (!applicationId || !clientToken) {
  console.log('DD_APPLICATION_ID or DD_CLIENT_TOKEN unset; keeping environment.prod.ts as-is');
  process.exit(0);
}

let content = readFileSync(envPath, 'utf8');
content = content.replace(/applicationId:\s*'[^']*'/, `applicationId: '${applicationId}'`);
content = content.replace(/clientToken:\s*'[^']*'/, `clientToken: '${clientToken}'`);
content = content.replace(/site:\s*'[^']*'/, `site: '${site}'`);
content = content.replace(/service:\s*'[^']*'/, `service: '${service}'`);
content = content.replace(/env:\s*'[^']*'/, `env: '${env}'`);

writeFileSync(envPath, content);
console.log('Datadog RUM credentials injected into environment.prod.ts');
