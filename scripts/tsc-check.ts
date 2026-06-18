#!/usr/bin/env tsx
import { execSync } from 'child_process';
import { argv } from 'process';

const stagedFiles = argv.slice(2);
const tsFiles = stagedFiles.filter(f => f.endsWith('.ts'));

if (tsFiles.length === 0) {
  console.log('No TypeScript files to check');
  process.exit(0);
}

// Run tsc with web tsconfig, without passing staged files as args
try {
  execSync('tsc --noEmit --project apps/web/tsconfig.app.json', { stdio: 'inherit' });
  process.exit(0);
} catch {
  process.exit(1);
}
