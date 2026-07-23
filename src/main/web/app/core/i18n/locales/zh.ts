import type { Translations } from '../translations.types';

export const zh: Translations = {
  common: {
    loadFailed: '加载数据失败',
    saveFailed: '保存失败',
    deleteFailed: '删除失败',
    operationFailed: '操作失败',
    success: '成功',
    error: '发生错误',
  },
  nav: {
    imageAnalysis: '图像分析',
    documentQA: '文档问答',
    mcp: 'MCP',
    eval: '评估',
    speechToText: '语音转写',
    agents: 'Agent',
    kubernetes: 'K8s',
    monitoring: '监控',
    aiinfra: 'AI 基础设施',
    chat: '对话',
    generation: '生成',
    modelDev: '开发',
    modelOps: '运维',
    model: '模型',
    llmops: 'LLMOps',
    aiops: 'AIOps',
    vectordb: '向量库',
    groups: {
      work: '工作',
      create: '创作',
      lab: '实验室',
    },
  },
  imageUploader: {
    imageLabel: '图片',
    resultLabel: '分析结果',
    dropText: '拖拽图片或点击上传',
    dropHint: 'JPG, PNG, GIF, WebP',
    analyzing: '分析中...',
    startAnalyze: '开始分析',
    uploadToAnalyze: '上传图片开始分析',
    selectImageError: '请选择图片文件',
    fileTooLarge: '图片大小不能超过 50MB',
    requestFailed: '请求失败',
    processingFailed: '处理失败',
    clearImage: '清除图片',
    caption: '图像描述',
    detect: '目标检测',
    ocr: '文字识别',
    noImageYet: '上传图片开始分析',
    noDetections: '未在该图片中检测到目标',
    processingTime: '处理耗时 {ms} 毫秒',
    providerUnavailable: '图像分析服务暂时不可用',
    clickToEnlarge: '点击放大',
  },
  ragChat: {
    title: '文档问答',
    modelBadge: 'RAG',
    uploadDocs: '上传文档',
    upload: '上传',
    askQuestion: '开始问我关于文档的问题吧',
    inputPlaceholder: '输入问题...',
    thinking: '思考中...',
    errorMessage: '抱歉，发生了错误。请确保RAG服务正在运行。',
    sources: '参考来源',
    similarity: '相似度',
    whatIsThis: '这是什么内容？',
    summarize: '总结一下要点',
    keyInfo: '有哪些关键信息？',
    explain: '解释一下相关概念',
    documents: '可用文档',
    documentsShort: '文档',
    showDocuments: '显示文档',
    hideDocuments: '隐藏文档',
    noDocuments: '暂无上传的文档',
    selectedDocuments: '已选择: {count}',
    selectAll: '全选',
    clearSelection: '清除',
    filesSelected: '已选择 {count} 个文件',
    uploadSuccess: '{name} 上传成功',
    uploadFailed: '{name} 上传失败',
    uploading: '上传中...',
    basedOn: '基于 {count} 个来源',
    documentDeleted: '文档已删除',
    deleteFailed: '删除失败，请重试',
    fileSelected: '已选择 {count} 个文件',
    openAgentWorkbench: '打开 Agent 流水线',
  },
  agents: {
    thinking: '思考中...',
    errorMessage: '发生错误，请重试。',
    pipeline: {
      inputPlaceholder: '输入分析主题（可选补充）...',
      defaultMessage: '按当前编排执行多智能体流水线。',
      paletteTitle: 'Agents',
      canvasHint: '点击或拖拽左侧 Agent 到画布会自动串线；也可从模版一键开始。',
      clear: '清空',
      run: '运行流水线',
      emptyState: {
        title: '搭建多步流水线',
        description: '选择模版或添加 Agent，填写任务，运行流水线，再查看结果。',
      },
      hints: {
        empty: '请至少将一个专业 Agent 放到画布上。',
        needConnections: '请先连接 Agent 节点再运行。',
        orphan: '请将所有 Agent 连成一条可执行流水线。',
        cycle: '请移除流水线中的环路。',
        invalid: '请先修正流水线结构。',
      },
      templates: {
        title: '编排模版',
        use: '使用',
        skipped: '已跳过不可用 Agent：{types}',
        items: {
          webResearch: {
            name: '网页调研',
            description: '先网页搜索，再综合成简报。',
          },
          knowledgeAnswer: {
            name: '知识问答',
            description: '先检索文档，再综合回答。',
          },
          weatherBrief: {
            name: '天气简报',
            description: '先调用天气工具，再整理给用户。',
          },
          businessAnalysis: {
            name: '商业分析',
            description: '双轮网页扫描 + 知识库上下文，输出商业策略简报。',
          },
          techAnalysis: {
            name: '技术分析',
            description: '双轮技术信号调研 + 仓库知识，输出可行性简报。',
          },
        },
        shortTopics: {
          webResearch: '网页调研简报',
          knowledgeAnswer: '知识库问答',
          weatherBrief: '天气简报',
          businessAnalysis: 'AI 产品商业分析',
          techAnalysis: 'AI 技术栈可行性分析',
        },
        briefPrompts: {
          webResearch:
            '针对上下文主题（未指定则围绕 AI Agent 编排）做网页调研，给出有出处的简要结论。',
          knowledgeAnswer:
            '优先用知识库回答。若文档不足请说明缺口，并给出清晰建议。',
          weatherBrief:
            '查询提及城市的天气（默认北京），向用户简明总结实况。',
          businessAnalysis: `请输出面向 AI 对话/RAG/Agent 产品（或上下文主题）的「商业简报」。

工作流：
1) 调研第 1 轮：扫描 Google、Apple、Microsoft、NVIDIA、Meta、OpenAI、DeepMind、Anthropic、Vercel、Cursor 的带日期商业信号（产品/定价/GTM）。
2) 调研第 2 轮：竞品、买方需求与变现模式。
3) 知识库：检索本仓库/产品相关文档上下文。
4) 分析师：综合成稿。

必含章节：
## Thesis
## Watchlist scan（带日期信号 + 链接）
## Business read（谁付钱、价值链、护城河 vs 商品化）
## Options（2–3：下注 / 为何现在 / 商业动作 / 工作量 / 风险）
## Recommendation（主推 + 暂缓）
## Next actions（3–5 条可执行）
## References

区分事实 / 推断 / 建议；禁止编造 URL。`,
          techAnalysis: `请输出面向 AI 对话/RAG/Agent 产品（或上下文主题）的「技术可行性简报」。

工作流：
1) 调研第 1 轮：厂商/平台技术动向（OpenAI、Anthropic、Google、Microsoft、NVIDIA、Vercel、Spring AI / Angular 生态）。
2) 调研第 2 轮：Hugging Face Trending 与近期 arXiv（cs.AI / cs.LG / cs.CL）中与栈相关的方法。
3) 知识库：检索本仓库栈相关文档（Java/Spring AI、Angular、RAG、agents）。
4) 分析师：综合成稿。

必含章节：
## Thesis
## Tech signals（带日期 + 链接）
## Technical read（成熟度、栈匹配、成本/延迟/数据、build vs buy）
## Options（2–3：技术动作 / 商业关联 / 工作量 / 风险）
## Recommendation（主推 + 暂缓）
## Next actions（3–5 条可执行 spike）
## References

区分事实 / 推断 / 建议；禁止编造 URL。`,
        },
      },
    },
    results: {
      title: '运行结果',
      collapse: '收起',
      expand: '展开结果',
      empty: '运行流水线后，结果会显示在这里。',
      expandMessage: '展开',
      collapseMessage: '收起',
    },
  },
  chat: {
    thinking: '思考中...',
    inputPlaceholder: '输入消息...',
    welcomeTitle: '今天我能帮你什么？',
    welcomeDescription: '随便问，或打开 Agent 做多步流水线',
    openAgentWorkbench: '打开 Agent 流水线',
    suggestedPromptsTitle: '推荐提示',
    suggestedPrompts: [
      {
        key: 'explain',
        label: '解释一个概念',
        description: '用简单的方式拆解一个主题',
      },
      {
        key: 'summarize',
        label: '总结要点',
        description: '获取关键信息的简洁摘要',
      },
      {
        key: 'brainstorm',
        label: '头脑风暴',
        description: '为问题生成创意方案',
      },
    ],
  },
  generate: {
    tabs: {
      image: '图像生成',
      tts: '语音合成',
    },
    image: {
      title: '图像生成',
      description: '使用已配置的模型生成图像（默认 Ollama）',
      promptLabel: '提示词',
      promptPlaceholder: '描述你想要的图像...',
      negativePromptLabel: '负面提示词',
      negativePromptPlaceholder: '图像中要避免的内容...',
      sizeLabel: '图像尺寸',
      generateButton: '生成图像',
      generating: '生成中...',
      preview: '预览',
      download: '下载',
      emptyState: '生成图像以查看预览',
      zoomLabel: '放大图片',
    },
    tts: {
      title: '文本转语音',
      description: '将文本转换为自然语音',
      textLabel: '文本',
      textPlaceholder: '输入要转换为语音的文本...',
      voiceLabel: '声音',
      speedLabel: '语速',
      synthesizeButton: '合成',
      synthesizing: '合成中...',
      audioReady: '音频就绪',
      downloadAudio: '下载音频',
      emptyState: '合成后在此试听',
    },
  },
  sidebar: {
    chatHistory: '聊天历史',
    newChat: '新对话',
    pinned: '已固定',
    recents: '最近',
    searchConversations: '搜索对话...',
  },
};
