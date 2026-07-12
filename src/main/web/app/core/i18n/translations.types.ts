// i18n model
export type Language = 'en' | 'zh' | 'ja' | 'fr' | 'es';

export const SUPPORTED_LANGUAGES: Language[] = ['en', 'zh', 'ja', 'fr', 'es'];

/** Sidebar nav keys reserved for future modules (no route wired yet). */
export const PLANNED_NAV_KEYS = [
  'supervisor',
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
    supervisor: string;
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
  };
  agents: {
    startConversation: string;
    inputPlaceholder: string;
    thinking: string;
    errorMessage: string;
    quickPrompts: {
      supervisor: string[];
      k8s: string[];
      monitoring: string[];
      model: string[];
      llmops: string[];
      aiops: string[];
      vectordb: string[];
    };
    descriptions: {
      supervisor: string;
      k8s: string;
      monitoring: string;
      model: string;
      llmops: string;
      aiops: string;
      vectordb: string;
    };
  };
  chat: {
    thinking: string;
    inputPlaceholder: string;
    welcomeTitle: string;
    welcomeDescription: string;
    suggestedPromptsTitle: string;
    suggestedPrompts: {
      key: string;
      label: string;
      description: string;
    }[];
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
