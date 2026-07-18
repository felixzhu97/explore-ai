#!/usr/bin/env node
/**
 * Build GitHub Actions Job Summary from ci-metrics + env check outcomes.
 */
import { existsSync, readFileSync, appendFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const metricsDir = process.env.CI_METRICS_DIR || join(root, 'ci-metrics');

function readJson(name) {
  const p = join(metricsDir, name);
  if (!existsSync(p)) return null;
  try {
    return JSON.parse(readFileSync(p, 'utf8'));
  } catch {
    return null;
  }
}

function env(name, fallback = '') {
  return process.env[name] ?? fallback;
}

function shortSha(sha) {
  return sha ? sha.slice(0, 8) : 'n/a';
}

function bar(percent, width = 20) {
  if (percent == null || Number.isNaN(percent)) return 'n/a';
  const filled = Math.max(0, Math.min(width, Math.round((percent / 100) * width)));
  return `${'█'.repeat(filled)}${'░'.repeat(width - filled)}`;
}

function formatDuration(ms) {
  if (ms == null || Number.isNaN(ms)) return 'n/a';
  if (ms < 1000) return `${Math.round(ms)}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function parseVitest(vitest) {
  if (!vitest) return null;
  const passed = vitest.numPassedTests ?? 0;
  const failed = vitest.numFailedTests ?? 0;
  const skipped = (vitest.numPendingTests ?? 0) + (vitest.numTodoTests ?? 0);
  const total = vitest.numTotalTests ?? passed + failed + skipped;
  let durationMs = vitest.duration;
  if (durationMs == null && Array.isArray(vitest.testResults)) {
    durationMs = vitest.testResults.reduce(
      (sum, t) => sum + (t.endTime && t.startTime ? t.endTime - t.startTime : 0),
      0,
    );
  }
  if (durationMs == null && vitest.startTime) {
    durationMs = Date.now() - vitest.startTime;
  }
  return { passed, failed, skipped, total, durationMs };
}

const checks = [
  {
    name: 'Backend',
    result: env('CI_GRADLE') || env('CI_BACKEND') || 'n/a',
    detail: 'Gradle test + JaCoCo',
  },
  {
    name: 'Frontend typecheck',
    result: env('CI_TYPECHECK') || 'n/a',
    detail: '`pnpm typecheck`',
  },
  {
    name: 'Frontend lint',
    result: env('CI_LINT') || 'n/a',
    detail: '`pnpm lint`',
  },
  {
    name: 'Frontend test',
    result: env('CI_TEST') || 'n/a',
    detail: '`pnpm test`',
  },
  {
    name: 'Frontend build',
    result: env('CI_BUILD') || 'n/a',
    detail: '`pnpm build`',
  },
];

const failedChecks = checks.filter(
  (c) => c.result === 'failure' || c.result === 'cancelled',
);
const jobFailed = [env('CI_BACKEND'), env('CI_FRONTEND')].includes('failure');
const overallFinal = jobFailed || failedChecks.length > 0
  ? 'failure'
  : checks.every((c) => c.result === 'skipped')
    ? 'skipped'
    : 'success';

const jacoco = readJson('jacoco.json');
const vitestRaw = readJson('vitest.json');
const vitest = parseVitest(vitestRaw);

const lines = [];
lines.push('## CI Summary');
lines.push('');
lines.push('### Run');
lines.push('| Field | Value |');
lines.push('| --- | --- |');
lines.push(`| Event | ${env('CI_EVENT', 'n/a')} |`);
lines.push(`| Ref | ${env('CI_REF', 'n/a')} |`);
lines.push(`| SHA | ${shortSha(env('CI_SHA'))} |`);
lines.push(`| Actor | ${env('CI_ACTOR', 'n/a')} |`);
lines.push('| Workflow | CI |');
lines.push(`| Overall | ${overallFinal} |`);
lines.push('');

lines.push('### Checks');
lines.push('| Check | Result | Detail |');
lines.push('| --- | --- | --- |');
for (const c of checks) {
  lines.push(`| ${c.name} | ${c.result} | ${c.detail} |`);
}
lines.push('');

lines.push('### Metrics');
lines.push('| Metric | Value | Gate |');
lines.push('| --- | --- | --- |');
if (vitest) {
  lines.push(`| Vitest passed | ${vitest.passed} | — |`);
  lines.push(`| Vitest failed | ${vitest.failed} | = 0 |`);
  lines.push(`| Vitest skipped | ${vitest.skipped} | — |`);
  lines.push(`| Vitest duration | ${formatDuration(vitest.durationMs)} | — |`);
} else {
  lines.push('| Vitest passed | n/a | — |');
  lines.push('| Vitest failed | n/a | = 0 |');
  lines.push('| Vitest skipped | n/a | — |');
  lines.push('| Vitest duration | n/a | — |');
}
if (jacoco) {
  const lg = jacoco.gates?.line ?? 60;
  const bg = jacoco.gates?.branch ?? 55;
  lines.push(
    `| JaCoCo line | ${jacoco.line.percent}% (${jacoco.line.covered}/${jacoco.line.total}) | ≥ ${lg}% |`,
  );
  lines.push(
    `| JaCoCo branch | ${jacoco.branch.percent}% (${jacoco.branch.covered}/${jacoco.branch.total}) | ≥ ${bg}% |`,
  );
} else {
  lines.push('| JaCoCo line | n/a | ≥ 60% |');
  lines.push('| JaCoCo branch | n/a | ≥ 55% |');
}
lines.push('');

lines.push('### Charts');
lines.push('');

function pushPie(title, slices) {
  const entries = Object.entries(slices).filter(([, n]) => Number(n) > 0);
  if (entries.length === 0) return;
  lines.push('```mermaid');
  lines.push(`pie title ${title}`);
  for (const [label, n] of entries) {
    lines.push(`  "${label}" : ${n}`);
  }
  lines.push('```');
  lines.push('');
}

// GitHub Mermaid supports pie; xychart-beta is not supported in Actions summaries.
if (vitest && vitest.total > 0) {
  pushPie('Vitest results', {
    passed: vitest.passed,
    failed: vitest.failed,
    skipped: vitest.skipped,
  });
} else if (vitest && vitest.total === 0) {
  lines.push('_Vitest: 0 tests_');
  lines.push('');
}

if (jacoco) {
  const lg = jacoco.gates?.line ?? 60;
  const bg = jacoco.gates?.branch ?? 55;
  pushPie('JaCoCo line coverage', {
    covered: jacoco.line.covered,
    missed: jacoco.line.missed,
  });
  pushPie('JaCoCo branch coverage', {
    covered: jacoco.branch.covered,
    missed: jacoco.branch.missed,
  });
  lines.push('### Coverage bars');
  lines.push('```');
  lines.push(
    `line   ${String(jacoco.line.percent).padStart(5)}%  ${bar(jacoco.line.percent)}  gate ${lg}%`,
  );
  lines.push(
    `branch ${String(jacoco.branch.percent).padStart(5)}%  ${bar(jacoco.branch.percent)}  gate ${bg}%`,
  );
  lines.push('```');
  lines.push('');
}

if (failedChecks.length > 0) {
  lines.push('### Failed');
  for (const c of failedChecks) {
    lines.push(`- ${c.name} (${c.detail.replace(/`/g, '')})`);
  }
  lines.push('');
}

lines.push('### Artifacts');
lines.push('- `jacoco-report` — HTML coverage (14d)');
lines.push('- `ci-metrics-backend` / `ci-metrics-frontend` — JSON used for this summary');
lines.push('');

const markdown = lines.join('\n');
const summaryPath = env('GITHUB_STEP_SUMMARY');
if (summaryPath) {
  appendFileSync(summaryPath, markdown);
  console.log(`Wrote job summary to ${summaryPath}`);
} else {
  process.stdout.write(markdown);
}
