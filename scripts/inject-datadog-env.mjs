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
  console.log('DD_APPLICATION_ID or DD_CLIENT_TOKEN unset; keeping environment.prod.ts datadog block as-is');
  process.exit(0);
}

let content = readFileSync(envPath, 'utf8');

content = content.replace(/datadog:\s*\{[\s\S]*?\}/, (match) => {
  let block = match;
  block = block.replace(/applicationId:\s*'[^']*'/, `applicationId: '${applicationId}'`);
  block = block.replace(/clientToken:\s*'[^']*'/, `clientToken: '${clientToken}'`);
  block = block.replace(/site:\s*'[^']*'/, `site: '${site}'`);
  block = block.replace(/service:\s*'[^']*'/, `service: '${service}'`);
  block = block.replace(/env:\s*'[^']*'/, `env: '${env}'`);
  return block;
});

writeFileSync(envPath, content);
console.log('Datadog RUM credentials injected into environment.prod.ts');
