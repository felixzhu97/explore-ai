import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const envPath = join(root, 'src/main/web/environments/environment.prod.ts');

const clientSideId = process.env.LAUNCHDARKLY_CLIENT_SIDE_ID?.trim() ?? '';

if (!clientSideId) {
  console.log('LAUNCHDARKLY_CLIENT_SIDE_ID unset; keeping environment.prod.ts as-is');
  process.exit(0);
}

let content = readFileSync(envPath, 'utf8');

content = content.replace(
  /launchDarklyClientSideId:\s*'[^']*'/,
  `launchDarklyClientSideId: '${clientSideId}'`,
);

writeFileSync(envPath, content);
console.log('LaunchDarkly client-side ID injected into environment.prod.ts');
