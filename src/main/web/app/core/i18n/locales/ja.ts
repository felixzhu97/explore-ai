import type { Translations } from '../translations.types';

export const ja: Translations = {
  common: {
    loadFailed: 'データの読み込みに失敗しました',
    saveFailed: '保存に失敗しました',
    deleteFailed: '削除に失敗しました',
    operationFailed: '操作に失敗しました',
    success: '成功',
    error: 'エラーが発生しました',
  },
  nav: {
    imageAnalysis: '画像分析',
    documentQA: 'ドキュメント',
    mcp: 'MCP',
    eval: '評価',
    speechToText: '音声認識',
    agents: 'Agent',
    pipelines: 'ワークフロー',
    skills: 'Skills',
    kubernetes: 'K8s',
    monitoring: '監視',
    aiinfra: 'AI インフラ',
    chat: 'チャット',
    metrics: 'メトリクス',
    privacy: 'プライバシー',
    legal: '法的情報',
    generation: '生成',
    modelDev: '開発',
    modelOps: '運用',
    model: 'モデル',
    llmops: 'LLMOps',
    aiops: 'AIOps',
    vectordb: 'VectorDB',
    groups: {
      work: '作業',
      create: '作成',
      lab: 'ラボ',
    },
  },
  account: {
    guest: 'ゲスト',
    plan: '{plan} プラン',
    language: '言語',
    help: 'ヘルプ',
    menuLabel: 'アカウントメニュー',
  },
  imageUploader: {
    imageLabel: '画像',
    resultLabel: '分析結果',
    dropText: 'ドラッグ＆ドロップまたはクリック',
    dropHint: 'JPG, PNG, GIF, WebP',
    analyzing: '分析中...',
    startAnalyze: '分析開始',
    uploadToAnalyze: '画像をアップロードして分析',
    selectImageError: '画像ファイルを選択してください',
    fileTooLarge: '画像は50MB以下にしてください',
    requestFailed: 'リクエスト失敗',
    processingFailed: '処理失敗',
    clearImage: '画像をクリア',
    caption: '画像説明',
    detect: '検出',
    ocr: '文字認識',
    noImageYet: '画像をアップロードして分析',
    noDetections: 'この画像から物体は検出されませんでした',
    processingTime: '処理時間 {ms} ms',
    providerUnavailable: '画像分析サービスは一時的に利用できません',
    clickToEnlarge: 'クリックして拡大',
  },
  ragChat: {
    title: 'ドキュメント Q&A',
    modelBadge: 'RAG',
    uploadDocs: 'ドキュメント',
    upload: 'アップロード',
    askQuestion: 'ドキュメントについて質問してください',
    inputPlaceholder: '質問を入力...',
    thinking: '考え中...',
    errorMessage: 'エラーが発生しました。RAGサービスが実行されていることを確認してください。',
    sources: '参照元',
    similarity: '類似度',
    whatIsThis: 'これは何ですか？',
    summarize: '要点をまとめて',
    keyInfo: '重要な情報は？',
    explain: '概念を説明して',
    documents: '利用可能なドキュメント',
    documentsShort: 'ドキュメント',
    showDocuments: 'ドキュメントを表示',
    hideDocuments: 'ドキュメントを隠す',
    noDocuments: 'アップロードされたドキュメントはありません',
    selectedDocuments: '選択中: {count}',
    selectAll: '全て選択',
    clearSelection: 'クリア',
    filesSelected: '{count} ファイル選択済み',
    uploadSuccess: '{name} アップロード成功',
    uploadFailed: '{name} アップロード失敗',
    uploading: 'アップロード中...',
    basedOn: '{count} ソースに基づく',
    documentDeleted: 'ドキュメントが削除されました',
    deleteFailed: '削除に失敗しました。もう一度お試しください',
    fileSelected: '{count} ファイル選択済み',
    openPipelineWorkbench: 'ワークフローを開く',
  },
  agents: {
    title: 'Agent',
    newSession: '新規',
    deleteSession: '削除',
    emptySessions: 'セッションなし',
    defaultTitle: 'New Agent',
    thinking: '考え中...',
    errorMessage: 'エラーが発生しました。',
    inputPlaceholder: 'Agent に調査を依頼...',
    welcomeTitle: 'Agent と協働',
    welcomeDescription: '記憶・再計画・ツール実行を会話で確認できます。',
    traceTitle: 'トレース',
    trace: {
      plan: '計画',
      replan: '再計画',
      thought: '思考',
      tool: 'ツール',
      evaluation: '評価',
    },
  },
  pipelines: {
    thinking: '考え中...',
    errorMessage: 'エラーが発生しました。もう一度お試しください。',
    pipeline: {
      inputPlaceholder: 'パイプラインのタスクを入力...',
      defaultMessage: '設定したエージェントパイプラインを実行してください。',
      paletteTitle: 'Agents',
      canvasHint: '左のエージェントをクリック/ドラッグすると自動で接続されます。テンプレートからも開始できます。',
      clear: 'クリア',
      run: 'パイプライン実行',
      emptyState: {
        title: '複数ステップのパイプラインを組む',
        description: 'テンプレートを選ぶかエージェントを追加し、タスクを設定して実行し、結果を確認します。',
      },
      hints: {
        empty: 'キャンバスに少なくとも1つのワーカーを追加してください。',
        needConnections: '実行前にエージェント同士を接続してください。',
        orphan: 'すべてのエージェントを1本のパイプラインに接続してください。',
        cycle: 'パイプラインの循環を解消してください。',
        invalid: 'パイプラインを修正してから実行してください。',
      },
      templates: {
        title: 'テンプレート',
        use: '使用',
        skipped: '利用できないエージェントをスキップしました: {types}',
        items: {
          webResearch: {
            name: 'Web調査',
            description: 'Web検索後に要約します。',
          },
          knowledgeAnswer: {
            name: 'ナレッジ回答',
            description: '文書検索後に回答を整理します。',
          },
          weatherBrief: {
            name: '天気ブリーフ',
            description: '天気ツールを呼び、要点をまとめます。',
          },
          businessAnalysis: {
            name: 'ビジネス分析',
            description: '2回のWeb調査とナレッジ、商業戦略ブリーフ。',
          },
          techAnalysis: {
            name: '技術分析',
            description: '技術シグナル調査とリポジトリ知識、実現性ブリーフ。',
          },
        },

        shortTopics: {
          webResearch: 'Web調査ブリーフ',
          knowledgeAnswer: 'ナレッジ回答',
          weatherBrief: '天気ブリーフ',
          businessAnalysis: 'AIプロダクト事業分析',
          techAnalysis: 'AIスタック技術実現性',
        },
        briefPrompts: {
          webResearch:
            'Research the topic in the user context (or AI agent orchestration if unspecified). Produce a short evidence-based brief with sources.',
          knowledgeAnswer:
            'Answer using the knowledge base first. If documents are thin, say what is missing. End with a clear recommendation.',
          weatherBrief:
            'Look up current weather for the city mentioned (default: Beijing) and summarize conditions for the user.',
          businessAnalysis: `Produce a Business Brief for AI chat/RAG/agent products (or the topic in context).

Workflow for workers:
1) Research pass 1 — scan dated commercial signals from Google, Apple, Microsoft, NVIDIA, Meta, OpenAI, DeepMind, Anthropic, Vercel, Cursor (product/pricing/GTM).
2) Research pass 2 — competitors, buyer demand, and monetization patterns.
3) Knowledge — retrieve any indexed product/docs context for this codebase.
4) Analyst — synthesize.

Required output sections:
## Thesis
## Watchlist scan (dated signals + links)
## Business read (who pays, value chain, moat vs commodity)
## Options (2–3: bet / why now / business move / effort / risk)
## Recommendation (primary + defer)
## Next actions (3–5 executable)
## References

Separate Fact vs Inference vs Recommendation. Do not invent URLs.`,
          techAnalysis: `Produce a Technical Feasibility Brief for AI chat/RAG/agent products (or the topic in context).

Workflow for workers:
1) Research pass 1 — vendor/platform tech moves (OpenAI, Anthropic, Google, Microsoft, NVIDIA, Vercel, Spring AI / Angular ecosystem).
2) Research pass 2 — Hugging Face Trending and recent arXiv (cs.AI / cs.LG / cs.CL) methods relevant to the stack.
3) Knowledge — retrieve indexed docs about this repo’s stack (Java/Spring AI, Angular, RAG, agents).
4) Analyst — synthesize.

Required output sections:
## Thesis
## Tech signals (dated + links)
## Technical read (maturity, stack fit, cost/latency/data, build vs buy)
## Options (2–3: tech move / business link / effort / risk)
## Recommendation (primary + defer)
## Next actions (3–5 executable spikes)
## References

Separate Fact vs Inference vs Recommendation. Do not invent URLs.`,
        },

      },
    },
    results: {
      title: '結果',
      collapse: '折りたたむ',
      expand: '結果を表示',
      empty: 'パイプラインを実行すると結果がここに表示されます。',
      expandMessage: 'もっと見る',
      collapseMessage: '閉じる',
    },
  },
  chat: {
    thinking: '考え中...',
    inputPlaceholder: 'メッセージを入力...',
    welcomeTitle: '今日は何をお手伝いできますか？',
    welcomeDescription: '何でも聞くか、Agent で多段パイプラインを開く',
    openPipelineWorkbench: 'ワークフローを開く',
    suggestedPromptsTitle: 'おすすめプロンプト',
    suggestedPrompts: [
      {
        key: 'explain',
        label: '概念を説明',
        description: 'トピックをわかりやすく解説',
      },
      {
        key: 'summarize',
        label: '要約する',
        description: '要点を簡潔にまとめる',
      },
      {
        key: 'brainstorm',
        label: 'アイデアを出す',
        description: '問題に対する創造的な案を生成',
      },
    ],
    skills: 'Skills',
    skillsEmpty: '有効な Skill がありません',
    skillsManage: 'Skills を管理',
    skillsSelected: '{count} 件選択中',
  },
  skillsPage: {
    title: 'Skills',
    subtitle: 'チャットで再利用できる指示パック',
    newSkill: '新規 Skill',
    templates: 'テンプレート',
    templatesHint: 'ワンクリックでライブラリに追加',
    yourSkills: 'あなたの Skills',
    empty: 'まだ Skill がありません。テンプレートを追加するか作成してください。',
    name: '名前',
    description: '説明',
    instructions: '指示',
    save: '保存',
    saving: '保存中…',
    cancel: 'キャンセル',
    edit: '編集',
    enable: '有効化',
    disable: '無効化',
    statusDisabled: '無効',
    delete: '削除',
    deleteConfirm: 'Skill「{name}」を削除しますか？',
    add: '追加',
    adding: '追加中…',
    customize: 'カスタマイズ',
    inLibrary: '追加済み',
    createTitle: 'Skill を作成',
    editTitle: 'Skill を編集',
    loadFailed: 'Skills の読み込みに失敗しました',
    saveFailed: 'Skill の保存に失敗しました',
    deleteFailed: 'Skill の削除に失敗しました',
    updateFailed: 'Skill の更新に失敗しました',
    nameRequired: '名前と指示は必須です',
    added: 'Skills に追加しました',
  },
  generate: {
    tabs: {
      image: '画像生成',
      tts: '音声合成',
    },
    image: {
      title: '画像生成',
      description: '設定済みプロバイダーで画像を生成（デフォルトは Ollama）',
      promptLabel: 'プロンプト',
      promptPlaceholder: '生成したい画像を説明してください...',
      negativePromptLabel: 'ネガティブプロンプト',
      negativePromptPlaceholder: '画像で避けるべきもの...',
      sizeLabel: '画像サイズ',
      generateButton: '画像を生成',
      generating: '生成中...',
      preview: 'プレビュー',
      download: 'ダウンロード',
      emptyState: '画像を生成してプレビューを表示',
      zoomLabel: '画像を拡大',
    },
    tts: {
      title: 'テキスト読み上げ',
      description: 'テキストを自然な音声に変換',
      textLabel: 'テキスト',
      textPlaceholder: '読み上げるテキストを入力...',
      voiceLabel: '音声',
      speedLabel: '速度',
      synthesizeButton: '合成',
      synthesizing: '合成中...',
      audioReady: '音声準備完了',
      downloadAudio: '音声をダウンロード',
      emptyState: '合成するとプレビューが表示されます',
    },
  },
  sidebar: {
    chatHistory: 'チャット履歴',
    newChat: '新しいチャット',
    pinned: 'ピン留め',
    recents: '最近',
    searchConversations: '会話を検索...',
  },
  privacy: {
    consentTitle: 'Analytics cookies',
    consentBody:
      'We use a necessary identity cookie for chat. Analytics (Datadog / LaunchDarkly) is optional.',
    consentLearnMore: 'Privacy details',
    consentAccept: 'Allow analytics',
    consentReject: 'Necessary only',
  },
};
