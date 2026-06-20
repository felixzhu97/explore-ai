---

## title: AI Chat & Agent Platform - 用户故事地图

## 用户故事地图概览

### 发布计划与用户活动

```mermaid
journey
    title AI Chat & Agent Platform - 用户故事地图
    section MVP - 基础对话 + RAG
        AI 对话: 5: 用户
        会话管理: 4: 用户
        RAG 知识问答: 5: 用户
        文档管理: 4: 用户
    section V2 - 图像 + TTS + MCP
        图像生成: 5: 用户
        语音合成: 5: 用户
        MCP 工具调用: 4: 用户
        天气查询: 3: 用户
    section V3 - 多 Agent + AIOps
        Multi-Agent 对话: 4: 用户
        Agent 协作: 3: 用户
        AIOps 监控: 3: 管理员
        日志分析: 4: 管理员
```



---

## MVP - 基础对话 + RAG

### 1. AI 对话

```mermaid
journey
    title MVP - AI 对话
    section 💬 发送消息
        输入问题: 5: 用户
        获得 AI 回复: 5: 用户
        查看流式输出: 5: 用户
        取消请求: 3: 用户
    section 📋 会话管理
        创建新会话: 4: 用户
        查看历史会话: 4: 用户
        继续之前的对话: 5: 用户
        删除会话: 3: 用户
    section 📝 消息操作
        复制 AI 回答: 4: 用户
        Markdown 渲染: 5: 用户
        代码高亮: 5: 用户
        评价回答: 3: 用户
```



### 2. RAG 知识问答

```mermaid
journey
    title MVP - RAG 知识问答
    section 📤 文档上传
        上传 TXT 文件: 5: 用户
        上传 PDF 文件: 5: 用户
        查看上传进度: 4: 用户
        处理完成通知: 4: 用户
    section 📚 文档管理
        查看文档列表: 4: 用户
        查看文档状态: 3: 用户
        删除文档: 4: 用户
        重新上传覆盖: 4: 用户
    section 🔍 RAG 问答
        基于文档提问: 5: 用户
        查看流式回答: 5: 用户
        查看答案来源: 5: 用户
        指定文档检索: 4: 用户
        调整检索数量: 3: 用户
```



---

## V2 - 图像 + TTS + MCP

### 3. 图像生成

```mermaid
journey
    title V2 - 图像生成
    section ✏️ 生成配置
        输入图像描述: 5: 用户
        选择图像尺寸: 4: 用户
        选择图像质量: 4: 用户
        选择生成模型: 3: 用户
    section 🎨 生成过程
        查看生成进度: 4: 用户
        生成成功: 5: 用户
        生成失败: 2: 用户
        重新生成: 4: 用户
    section 📥 图像操作
        下载图像: 5: 用户
        复制图像 URL: 4: 用户
        新标签页打开: 4: 用户
        将图像用于对话: 4: 用户
        查看历史记录: 3: 用户
```



### 4. 语音合成

```mermaid
journey
    title V2 - 语音合成
    section 🔤 文本配置
        输入要转换的文本: 5: 用户
        选择声音: 4: 用户
        查看可用声音: 4: 用户
        调整语速: 3: 用户
    section ▶️ 播放控制
        预览语音: 5: 用户
        播放/暂停: 5: 用户
        查看波形可视化: 4: 用户
        调整音量: 3: 用户
    section 💾 导出操作
        下载语音文件: 5: 用户
        将对话转为语音: 4: 用户
        保存到本地: 4: 用户
```



### 5. MCP 工具调用

```mermaid
journey
    title V2 - MCP 工具调用
    section 🔧 工具管理
        查看可用工具: 4: 用户
        了解工具用途: 4: 用户
        查看调用历史: 3: 用户
        收藏常用工具: 3: 用户
    section ⚡ 工具调用
        通过对话自动调用: 5: 用户
        手动选择工具: 4: 用户
        查看调用参数: 3: 用户
        查看调用结果: 5: 用户
        查看调用状态: 4: 用户
    section 🔗 RAG 集成
        RAG 检索结果展示: 5: 用户
        查看检索来源: 4: 用户
        组合多个工具: 3: 用户
```



---

## V3 - 多 Agent + AIOps

### 6. Multi-Agent 对话

```mermaid
journey
    title V3 - Multi-Agent 对话
    section 🤖 Agent 配置
        创建自定义 Agent: 4: 用户
        配置 Agent 角色: 4: 用户
        设置系统提示词: 4: 用户
        绑定工具: 4: 用户
    section 💬 Agent 对话
        选择不同 Agent: 5: 用户
        Agent 之间协作: 4: 用户
        指定专业领域: 4: 用户
        查看思考过程: 5: 用户
        监控执行状态: 4: 用户
    section 📤 记录导出
        保存 Agent 对话: 4: 用户
        导出对话记录: 4: 用户
        分享对话链接: 3: 用户
```



### 7. AIOps 智能运维

```mermaid
journey
    title V3 - AIOps 智能运维
    section 📊 系统监控
        查看健康状态: 5: 管理员
        API 调用统计: 4: 管理员
        性能指标: 4: 管理员
        设置告警阈值: 3: 管理员
    section 🔍 日志分析
        分析错误日志: 5: 管理员
        追踪请求链路: 4: 管理员
        自然语言查询日志: 5: 管理员
        异常诊断建议: 4: 管理员
    section 📈 报告与自动化
        生成运维报告: 4: 管理员
        设置自动化告警: 4: 管理员
        配置自动恢复: 3: 管理员
        告警通知: 5: 管理员
```



---

## 用户角色与故事对照


| 角色       | 用户故事数量               | 优先级   |
| -------- | -------------------- | ----- |
| **最终用户** | 基础对话、RAG 问答、图像生成、TTS | P0-P1 |
| **开发者**  | MCP 工具调用、API 集成      | P1    |
| **管理员**  | AIOps 监控、日志分析        | P2    |


---

## 发布版本功能对照


| 版本      | 交付内容                | 故事数量 |
| ------- | ------------------- | ---- |
| **MVP** | AI 对话 + RAG 知识问答    | ~25  |
| **V2**  | 图像生成 + TTS + MCP 工具 | ~25  |
| **V3**  | Multi-Agent + AIOps | ~20  |


---

## 参考

- [Mermaid User Journey Syntax](https://mermaid.ai/open-source/syntax/userJourney.html)
- [User Story Mapping - Jeff Patton](https://www.jpattonassociates.com/user-story-mapping/)

