# CI/CD 文档

GitHub Actions 工作流相关的架构图。

## 文件

| 文件 | 工作流 | 说明 |
| ---- | ------ | ---- |
| `cicd-workflow.puml`         | 总览     | 三个工作流的触发条件与高层流程概览          |
| `cicd-backend-ci.puml`       | backend-ci  | Java 后端构建、测试、JaCoCo 覆盖率与工件上传 |
| `cicd-codeql.puml`           | codeql      | Java/Kotlin 与 JavaScript/TypeScript 矩阵扫描 |
| `cicd-review-dog.puml`       | review-dog  | ESLint、TypeScript、Prettier、Vitest 并行检查 |

## 查看

- [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
- VS Code PlantUML 插件
- `plantuml -o png *.puml`
