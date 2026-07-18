#!/usr/bin/env node
/**
 * Parse JaCoCo XML report into ci-metrics/jacoco.json.
 * Looks for common report paths under build/reports.
 */
import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const candidates = [
  join(root, 'build/reports/jacoco/test/jacocoTestReport.xml'),
  join(root, 'build/reports/jacocoTestReport/jacocoTestReport.xml'),
  join(root, 'build/reports/jacoco/jacocoTestReport.xml'),
];

const xmlPath = candidates.find((p) => existsSync(p));
if (!xmlPath) {
  console.error('JaCoCo XML not found. Tried:\n' + candidates.join('\n'));
  process.exit(1);
}

const xml = readFileSync(xmlPath, 'utf8');

function lastCounter(type) {
  const re = new RegExp(
    `<counter type="${type}" missed="(\\d+)" covered="(\\d+)"`,
    'g',
  );
  let match;
  let last = null;
  while ((match = re.exec(xml)) !== null) {
    last = {
      missed: Number(match[1]),
      covered: Number(match[2]),
    };
  }
  return last;
}

function ratio(counter) {
  if (!counter) return null;
  const total = counter.missed + counter.covered;
  if (total === 0) return 0;
  return (counter.covered / total) * 100;
}

const line = lastCounter('LINE');
const branch = lastCounter('BRANCH');
if (!line || !branch) {
  console.error(`Could not parse LINE/BRANCH counters from ${xmlPath}`);
  process.exit(1);
}

const out = {
  source: xmlPath.replace(root + '/', ''),
  gates: { line: 60, branch: 55 },
  line: {
    covered: line.covered,
    missed: line.missed,
    total: line.covered + line.missed,
    percent: Number(ratio(line).toFixed(1)),
  },
  branch: {
    covered: branch.covered,
    missed: branch.missed,
    total: branch.covered + branch.missed,
    percent: Number(ratio(branch).toFixed(1)),
  },
};

const outDir = join(root, 'ci-metrics');
mkdirSync(outDir, { recursive: true });
const outFile = join(outDir, 'jacoco.json');
writeFileSync(outFile, JSON.stringify(out, null, 2) + '\n');
console.log(`Wrote ${outFile}`);
console.log(
  `LINE ${out.line.percent}% (${out.line.covered}/${out.line.total})  `
  + `BRANCH ${out.branch.percent}% (${out.branch.covered}/${out.branch.total})`,
);
