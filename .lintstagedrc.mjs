/**
 * lint-staged: eslint on staged *.ts.
 * Full `pnpm typecheck` runs in `.husky/pre-commit` (matches CI Frontend typecheck).
 */
export default {
  '*.ts': ['eslint --fix'],
};
