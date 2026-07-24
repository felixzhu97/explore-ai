# CI/CD 文档

GitHub Actions 工作流相关的架构图。

## 文件

| 文件 | 工作流 | 说明 |
| ---- | ------ | ---- |
| `cicd-workflow.puml` | 总览 | 门禁 CI、CodeQL、E2E nightly 的触发与高层流程 |
| `cicd-codeql.puml` | codeql | Java/Kotlin 与 JavaScript/TypeScript 矩阵扫描 |

## 工作流（源文件）

| 工作流 | 说明 |
| ------ | ---- |
| `.github/workflows/ci.yml` | 唯一 PR/push 门禁：Backend + Frontend；Summary job 用 `$GITHUB_STEP_SUMMARY` 输出数字/Mermaid 报告（`scripts/write-ci-summary.mjs`） |
| `.github/workflows/codeql.yml` | 安全扫描 |
| `.github/workflows/e2e-nightly.yml` | Playwright 视觉回归（cron + 手动） |

## 查看

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- `plantuml -o png *.puml`
