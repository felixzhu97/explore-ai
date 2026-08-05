// i18n model
export type Language = 'en' | 'zh' | 'ja' | 'fr' | 'es';

export const SUPPORTED_LANGUAGES: Language[] = ['en', 'zh', 'ja', 'fr', 'es'];

/** Sidebar nav keys reserved for future modules (no route wired yet). */
export const PLANNED_NAV_KEYS = [
  'kubernetes',
  'monitoring',
  'aiinfra',
  'modelDev',
  'modelOps',
  'model',
  'llmops',
  'aiops',
  'vectordb',
] as const satisfies readonly (keyof Translations['nav'])[];

export interface Translations {
  common: {
    loadFailed: string;
    saveFailed: string;
    deleteFailed: string;
    operationFailed: string;
    success: string;
    error: string;
  };
  nav: {
    imageAnalysis: string;
    documentQA: string;
    mcp: string;
    eval: string;
    speechToText: string;
    agents: string;
    pipelines: string;
    skills: string;
    kubernetes: string;
    monitoring: string;
    aiinfra: string;
    chat: string;
    metrics: string;
    privacy: string;
    legal: string;
    generation: string;
    modelDev: string;
    modelOps: string;
    model: string;
    llmops: string;
    aiops: string;
    vectordb: string;
    groups: {
      work: string;
      create: string;
      lab: string;
    };
  };
  account: {
    guest: string;
    plan: string;
    language: string;
    help: string;
    menuLabel: string;
  };
  imageUploader: {
    imageLabel: string;
    resultLabel: string;
    dropText: string;
    dropHint: string;
    analyzing: string;
    startAnalyze: string;
    uploadToAnalyze: string;
    selectImageError: string;
    fileTooLarge: string;
    requestFailed: string;
    processingFailed: string;
    clearImage: string;
    caption: string;
    detect: string;
    ocr: string;
    noImageYet: string;
    noDetections: string;
    processingTime: string;
    providerUnavailable: string;
    clickToEnlarge: string;
  };
  ragChat: {
    title: string;
    modelBadge: string;
    uploadDocs: string;
    upload: string;
    askQuestion: string;
    inputPlaceholder: string;
    thinking: string;
    errorMessage: string;
    sources: string;
    similarity: string;
    whatIsThis: string;
    summarize: string;
    keyInfo: string;
    explain: string;
    documents: string;
    documentsShort: string;
    showDocuments: string;
    hideDocuments: string;
    noDocuments: string;
    selectedDocuments: string;
    selectAll: string;
    clearSelection: string;
    filesSelected: string;
    uploadSuccess: string;
    uploadFailed: string;
    uploading: string;
    basedOn: string;
    documentDeleted: string;
    deleteFailed: string;
    fileSelected: string;
    openPipelineWorkbench: string;
  };
  agents: {
    title: string;
    newSession: string;
    deleteSession: string;
    emptySessions: string;
    defaultTitle: string;
    thinking: string;
    errorMessage: string;
    inputPlaceholder: string;
    welcomeTitle: string;
    welcomeDescription: string;
    traceTitle: string;
    trace: {
      plan: string;
      replan: string;
      thought: string;
      tool: string;
      evaluation: string;
    };
  };

  pipelines: {
    thinking: string;
    errorMessage: string;
    pipeline: {
      inputPlaceholder: string;
      defaultMessage: string;
      paletteTitle: string;
      canvasHint: string;
      clear: string;
      run: string;
      emptyState: {
        title: string;
        description: string;
      };
      hints: {
        empty: string;
        needConnections: string;
        orphan: string;
        cycle: string;
        invalid: string;
      };
      templates: {
        title: string;
        use: string;
        skipped: string;
        templatesHint: string;
        myTemplates: string;
        emptyLibrary: string;
        newTemplate: string;
        add: string;
        adding: string;
        customize: string;
        inLibrary: string;
        createTitle: string;
        editTitle: string;
        name: string;
        description: string;
        agentTypes: string;
        shortTopic: string;
        briefPrompt: string;
        save: string;
        saving: string;
        cancel: string;
        edit: string;
        enable: string;
        disable: string;
        statusDisabled: string;
        delete: string;
        deleteConfirm: string;
        added: string;
        saveFailed: string;
        updateFailed: string;
        deleteFailed: string;
        nameRequired: string;
      };
    };
    results: {
      title: string;
      collapse: string;
      expand: string;
      empty: string;
      expandMessage: string;
      collapseMessage: string;
    };
  };
  chat: {
    thinking: string;
    inputPlaceholder: string;
    welcomeTitle: string;
    welcomeDescription: string;
    openPipelineWorkbench: string;
    suggestedPromptsTitle: string;
    suggestedPrompts: {
      key: string;
      label: string;
      description: string;
    }[];
    skills: string;
    skillsEmpty: string;
    skillsManage: string;
    skillsSelected: string;
  };
  skillsPage: {
    title: string;
    subtitle: string;
    newSkill: string;
    templates: string;
    templatesHint: string;
    yourSkills: string;
    empty: string;
    name: string;
    description: string;
    instructions: string;
    save: string;
    saving: string;
    cancel: string;
    edit: string;
    enable: string;
    disable: string;
    statusDisabled: string;
    delete: string;
    deleteConfirm: string;
    add: string;
    adding: string;
    customize: string;
    inLibrary: string;
    createTitle: string;
    editTitle: string;
    loadFailed: string;
    saveFailed: string;
    deleteFailed: string;
    updateFailed: string;
    nameRequired: string;
    added: string;
  };
  generate: {
    tabs: {
      image: string;
      tts: string;
    };
    image: {
      title: string;
      description: string;
      promptLabel: string;
      promptPlaceholder: string;
      negativePromptLabel: string;
      negativePromptPlaceholder: string;
      sizeLabel: string;
      generateButton: string;
      generating: string;
      preview: string;
      download: string;
      emptyState: string;
      zoomLabel: string;
    };
    tts: {
      title: string;
      description: string;
      textLabel: string;
      textPlaceholder: string;
      voiceLabel: string;
      speedLabel: string;
      synthesizeButton: string;
      synthesizing: string;
      audioReady: string;
      downloadAudio: string;
      emptyState: string;
    };
  };
  sidebar: {
    chatHistory: string;
    newChat: string;
    pinned: string;
    recents: string;
    searchConversations: string;
  };
  privacy: {
    consentTitle: string;
    consentBody: string;
    consentLearnMore: string;
    consentAccept: string;
    consentReject: string;
  };
}
