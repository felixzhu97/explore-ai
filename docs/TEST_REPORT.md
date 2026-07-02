# AI-Explore 功能测试报告

**测试日期**: 2026-07-04
**测试环境**: macOS + Spring Boot 4.1.0 + Angular 22
**后端端口**: 9000
**前端端口**: 4200

---

## 测试环境配置

### Ollama 模型 (已安装)

```
mxbai-embed-large:latest
x/z-image-turbo:latest
llama3.3:latest
nomic-embed-text:latest
deepseek-r1:70b
qwen3.5:35b
qwen3-coder:30b
```

### 配置的模型 (application.yml)

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: deepseek-v4-flash
      image:
        options:
          model: dall-e-3
      audio:
        speech:
          options:
            model: gpt-4o-mini-tts

rag:
  ollama:
    chat:
      model: qwen3.5:35b
```

---

## 测试结果汇总


| 功能模块         | 状态      | 问题数 |
| ------------ | ------- | --- |
| AI 对话        | FAIL    | 1   |
| RAG 文档问答     | FAIL    | 2   |
| Tool Calling | FAIL    | 2   |
| 图像生成         | FAIL    | 1   |
| 语音合成 (TTS)   | FAIL    | 1   |
| 语音识别 (ASR)   | UNKNOWN | -   |
| 视觉分析         | PARTIAL | -   |

---

## Jira Bug 清单

| Issue Key | 标题 | 优先级 | 状态 |
|-----------|------|--------|------|
| [AI-82](https://felixzhu.atlassian.net/browse/AI-82) | AI 对话功能失败 - Ollama 使用未安装的 mistral 模型 | 高 | 待办 |
| [AI-83](https://felixzhu.atlassian.net/browse/AI-83) | 语音合成 (TTS) 功能失败 - gpt-4o-mini-tts 模型不存在 | 高 | 待办 |
| [AI-84](https://felixzhu.atlassian.net/browse/AI-84) | 图像生成功能失败 - DALL-E 模型在 DeepSeek API 不可用 | 高 | 待办 |
| [AI-85](https://felixzhu.atlassian.net/browse/AI-85) | RAG SSE 流式响应端点配置错误 | 中 | 待办 |
| [AI-86](https://felixzhu.atlassian.net/browse/AI-86) | RAG API 路由不一致 - /documents vs /documents/ | 中 | 待办 |

---

## 详细问题

### 1. AI 对话功能 - FAIL

#### 问题 1.1: Ollama 模型配置错误

**严重程度**: HIGH
**问题类型**: 配置错误

**现象描述**:
调用 `/api/chat` 接口时返回 500 错误，日志显示尝试连接 Ollama 但使用不存在的 `mistral` 模型。

**错误日志**:

```
org.springframework.ai.retry.NonTransientAiException: 404 - {"error":"model 'mistral' not found"}
```

**根本原因**:
代码中硬编码了 `mistral` 模型，但该模型未在 Ollama 中安装。实际安装的模型列表中不包含 `mistral`。

**受影响接口**:

- `POST /api/chat`
- `POST /api/text/chat/stream`

**修复建议**:

1. 检查并更新 `spring-ai` 默认模型配置
2. 明确指定使用的模型名称为可用的模型之一（如 `qwen3.5:35b` 或 `llama3.3:latest`）

---

### 2. RAG 文档问答 - FAIL

#### 问题 2.1: SSE 流式响应端点配置错误

**严重程度**: HIGH
**问题类型**: 端点配置错误

**现象描述**:
调用 `/api/rag/chat/stream` 接口返回 500 错误，日志显示 `HttpMediaTypeNotAcceptableException: No acceptable representation`。

**错误日志**:

```
Could not resolve parameter [0] of type Sinks.Many in public abstract reactor.core.publisher.Flux<java.lang.String>
```

**根本原因**:
RAG 流式端点可能使用了错误的参数解析方式。

**受影响接口**:

- `POST /api/rag/chat/stream`

**修复建议**:
检查 `RagController` 中 SSE 流式响应的实现，确保正确处理流式响应。

#### 问题 2.2: 文档列表 API 路由不一致

**严重程度**: MEDIUM
**问题类型**: API 设计问题

**现象描述**:

- `GET /api/rag/documents` 返回 404
- `GET /api/rag/documents/` (带尾斜杠) 返回 200

**根本原因**:
Controller 中映射为 `@GetMapping("/documents/")`，但文档中描述为 `/documents`。

**受影响接口**:

- `GET /api/rag/documents/` (正确)
- `GET /api/rag/documents` (404)

**修复建议**:
统一 API 路由，移除尾斜杠或更新文档。

---

### 3. Tool Calling 功能 - FAIL

#### 问题 3.1: Tool Calling 降级处理问题

**严重程度**: MEDIUM
**问题类型**: 功能异常

**现象描述**:
调用 `/api/tools/chat` 接口时返回通用错误消息，但未明确说明工具调用失败原因。

**响应**:

```json
{
  "answer": "抱歉，处理您的请求时发生错误，请稍后重试。",
  "toolCalls": null
}
```

**根本原因**:
Tool Calling 功能调用失败后返回了用户友好的错误消息，但日志中没有详细的错误信息。

**受影响接口**:

- `POST /api/tools/chat`

**修复建议**:

1. 增加更详细的错误日志
2. 考虑在前端显示具体的错误原因

#### 问题 3.2: MCP 客户端工具暴露配置问题

**严重程度**: LOW
**问题类型**: 配置提示

**现象描述**:
日志中显示 MCP Client 工具未被 MCP Server 暴露。

**日志**:

```
Found MCP Clients. The MCP Client tools will not be exposed by the MCP Server.
If you would like to expose the tools, set spring.ai.mcp.server.expose-mcp-client-tools=true.
```

**修复建议**:
根据业务需求决定是否需要暴露 MCP Client 工具。

---

### 4. 图像生成功能 - FAIL

#### 问题 4.1: DALL-E 模型不可用

**严重程度**: HIGH
**问题类型**: 配置错误

**现象描述**:
调用 `/api/images/generate` 接口返回错误。

**错误日志**:

```
Error: 生成图片时发生错误，请稍后重试。
```

**根本原因**:
配置中指定了 `dall-e-3` 模型，但 DeepSeek API 可能不支持该模型。

**受影响接口**:

- `POST /api/images/generate`

**修复建议**:

1. 检查 DeepSeek API 是否支持图像生成
2. 如不支持，考虑切换到支持图像生成的 API（如 OpenAI）

---

### 5. 语音合成 (TTS) 功能 - FAIL

#### 问题 5.1: TTS 模型配置错误

**严重程度**: HIGH
**问题类型**: 配置错误

**现象描述**:
调用 `/api/audio/speak` 接口返回 404 错误。

**错误日志**:

```
com.openai.errors.NotFoundException: 404: Unknown
```

**根本原因**:
配置中指定了 `gpt-4o-mini-tts` 模型，但 DeepSeek API 不支持此模型。

**受影响接口**:

- `POST /api/audio/speak`

**修复建议**:

1. DeepSeek API 可能不支持 TTS 功能
2. 考虑使用 Ollama 本地模型（如有可用 TTS 模型）
3. 或配置使用 OpenAI API

---

### 6. 语音识别 (ASR) 功能 - UNKNOWN

**测试状态**: 未测试

**原因**: WebSocket 连接测试需要专门的客户端。

---

### 7. 视觉分析功能 - PARTIAL (UI OK, Backend FAIL)

**测试状态**: UI 正常，后端需要测试图像

**前端 UI 测试结果**:
- 页面加载正常
- 三个选项卡 (Caption, Detect, OCR) 切换正常
- 上传按钮存在
- 拖拽上传区域存在
- 分析按钮在上传前正确禁用
- 无 JavaScript 错误

**建议**:
1. 添加示例/测试图像按钮方便测试
2. 增强上传按钮的可见性
3. 添加加载状态指示器
4. 添加 AI 服务不可用时的错误提示 UI

---

## 前端 UI 测试

### 观察结果

1. **前端界面加载正常**
  - 首页正确显示侧边栏菜单
  - 包含 "Document QA"、"Vision AI"、"AI Hub" 导航链接
2. **AI Hub 页面**
  - Provider 选择器: OpenAI, Anthropic Claude, Ollama
  - 模型选择器: gpt-4o, gpt-4o-mini, gpt-4-turbo
  - 消息输入框存在
  - 发送按钮存在
3. **RAG 页面**
  - 消息输入框存在
  - 预设问题按钮存在
  - 历史会话列表显示正常
4. **Vision AI 页面**
  - 图像上传区域存在 (Drag & drop)
  - 分析选项卡: Caption, Detect, OCR
  - 分析按钮存在

---

## 建议优先级

### P0 (立即修复)

1. Ollama 模型配置错误 - 影响所有 AI 对话功能
2. TTS 模型配置错误 - 影响语音合成功能
3. 图像生成 API - 影响图像生成功能

### P1 (尽快修复)

1. RAG SSE 流式响应端点问题
2. API 路由不一致

### P2 (后续优化)

1. Tool Calling 错误消息优化
2. MCP 工具暴露配置
3. ASR/视觉分析功能完整测试

---

## 测试方法

```bash
# 1. 启动后端
./gradlew bootRun

# 2. 启动前端
cd src/main/web && pnpm start

# 3. API 测试
# 健康检查
curl http://localhost:9000/actuator/health

# AI 对话 (失败 - 模型配置问题)
curl -X POST http://localhost:9000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello"}'

# RAG 文档列表 (成功)
curl http://localhost:9000/api/rag/documents/

# RAG 流式问答 (失败 - SSE 问题)
curl -X POST http://localhost:9000/api/rag/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"question": "test"}' \
  -H "Accept: text/event-stream"

# Tool Calling (失败 - 降级)
curl -X POST http://localhost:9000/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "weather in Beijing"}'

# 图像生成 (失败 - DALL-E 问题)
curl -X POST http://localhost:9000/api/images/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A cat", "width": 512, "height": 512}'

# TTS (失败 - 模型不存在)
curl -X POST http://localhost:9000/api/audio/speak \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello"}' \
  --output speech.mp3
```

