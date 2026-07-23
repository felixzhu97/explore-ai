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
    kubernetes: string;
    monitoring: string;
    aiinfra: string;
    chat: string;
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
    openAgentWorkbench: string;
  };
  agents: {
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
        items: {
          webResearch: { name: string; description: string };
          knowledgeAnswer: { name: string; description: string };
          weatherBrief: { name: string; description: string };
          businessAnalysis: { name: string; description: string };
          techAnalysis: { name: string; description: string };
        };
        shortTopics: {
          webResearch: string;
          knowledgeAnswer: string;
          weatherBrief: string;
          businessAnalysis: string;
          techAnalysis: string;
        };
        briefPrompts: {
          webResearch: string;
          knowledgeAnswer: string;
          weatherBrief: string;
          businessAnalysis: string;
          techAnalysis: string;
        };
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
    openAgentWorkbench: string;
    suggestedPromptsTitle: string;
    suggestedPrompts: {
      key: string;
      label: string;
      description: string;
    }[];
  };
  sender: {
    openActions: string;
    filterPlaceholder: string;
    empty: string;
    groups: {
      tools: string;
      agents: string;
      navigate: string;
      session: string;
    };
    actions: {
      openAgentPipeline: string;
      openAgentPipelineHint: string;
      newChat: string;
      toggleTools: string;
    };
    toolPrompts: {
      getWeather: string;
      getForecast: string;
      searchWeb: string;
      searchDocuments: string;
      generic: string;
    };
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
}
