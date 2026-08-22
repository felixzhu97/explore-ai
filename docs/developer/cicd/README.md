# CI/CD 文档

GitHub Actions 工作流相关的架构图。

## 文件

| 文件 | 工作流 | 说明 |
| ---- | ------ | ---- |
| `cicd-workflow.puml` | 总览 | 门禁 CI 与 E2E nightly 的触发与高层流程 |

## 工作流（源文件）

| 工作流 | 说明 |
| ------ | ---- |
| `.github/workflows/ci.yml` | PR/push 门禁：Backend（spotless、checkstyle、test）+ Frontend（typecheck、lint、test、build） |
| `.github/workflows/e2e-nightly.yml` | Playwright 视觉回归（cron + 手动） |

## 查看

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- `plantuml -o png *.puml`
