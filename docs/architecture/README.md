# 数字人服务架构图

本目录包含数字人服务的完整 C4 架构图和序列图。

## 目录结构

```
architecture/
├── README.md                          # 本文件，架构图索引
├── digital-human-context.puml         # C1 上下文图 - 系统边界和外部交互
├── digital-human-container.puml      # C2 容器图 - 服务内部架构
├── digital-human-component.puml       # C3 组件图 - 核心组件详情
├── digital-human-sequence-conversation.puml  # 完整对话流程时序图
├── digital-human-sequence-tts.puml         # TTS 语音合成流程时序图
└── digital-human-sequence-expression.puml  # 表情动画更新流程时序图
```

## 图表说明

### C4 模型层级

| 层级 | 文件 | 说明 |
|------|------|------|
| **C1 Context** | `digital-human-context.puml` | 系统上下文，展示数字人与外部系统的交互关系 |
| **C2 Container** | `digital-human-container.puml` | 容器视图，展示服务内部的主要容器和组件 |
| **C3 Component** | `digital-human-component.puml` | 组件视图，展示每个容器内的详细组件 |

### 序列图

| 图表 | 文件 | 说明 |
|------|------|------|
| 对话流程 | `digital-human-sequence-conversation.puml` | 用户发起对话到数字人回复的完整流程 |
| TTS 合成 | `digital-human-sequence-tts.puml` | 语音合成的详细流程，包括多引擎支持 |
| 表情更新 | `digital-human-sequence-expression.puml` | 表情、口型、手势的动画控制流程 |

## 查看图表

### 在线预览

可以使用 PlantUML 在线编辑器预览：

1. 访问 [PlantUML Online Editor](http://www.plantuml.com/plantuml/uml/)
2. 粘贴 `.puml` 文件内容
3. 点击 "Submit" 生成图表

### 本地生成

```bash
# 安装 PlantUML
brew install plantuml

# 生成 PNG
plantuml -png digital-human-context.puml
plantuml -png digital-human-container.puml
plantuml -png digital-human-component.puml
plantuml -png digital-human-sequence-conversation.puml
plantuml -png digital-human-sequence-tts.puml
plantuml -png digital-human-sequence-expression.puml
```

## 相关文档

- [数字人服务文档](../digital-human.md) - 完整的 API 参考和配置说明
- [主架构文档](../ARCHITECTURE.md) - AI-Test 平台整体架构
