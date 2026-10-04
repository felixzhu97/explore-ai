import { en } from './en';
import { zh } from './zh';
import { ja } from './ja';
import { fr } from './fr';
import { es } from './es';

export type Language = 'en' | 'zh' | 'ja' | 'fr' | 'es';

export const SUPPORTED_LANGUAGES: Language[] = ['en', 'zh', 'ja', 'fr', 'es'];

/** Sidebar nav keys reserved for future modules (no route wired yet). */
export const PLANNED_NAV_KEYS = [
  'kubernetes',
  'monitoring',
  'aiInfra',
  'modelDev',
  'modelOps',
  'model',
  'llmOps',
  'aiOps',
  'vectorDb',
] as const satisfies readonly (keyof Translations['nav'])[];

export interface Translations {
  common: {
    success: string;
    save: string;
    saving: string;
    cancel: string;
    delete: string;
    edit: string;
    add: string;
    adding: string;
    name: string;
    thinking: string;
    errors: {
      generic: string;
      loadFailed: string;
      saveFailed: string;
      deleteFailed: string;
      operationFailed: string;
    };
  };
  nav: {
    vision: string;
    rag: string;
    mcp: string;
    eval: string;
    speechToText: string;
    pipelines: string;
    automations: string;
    agents: string;
    skills: string;
    kubernetes: string;
    monitoring: string;
    aiInfra: string;
    chat: string;
    metrics: string;
    privacy: string;
    legal: string;
    policies: string;
    generate: string;
    modelDev: string;
    modelOps: string;
    model: string;
    llmOps: string;
    aiOps: string;
    vectorDb: string;
    more: string;
    groups: {
      work: string;
      create: string;
      lab: string;
    };
  };
  account: {
    guest: string;
    signedIn: string;
    plan: string;
    language: string;
    help: string;
    menuLabel: string;
    login: string;
    logout: string;
    loginSuccess: string;
    logoutSuccess: string;
    loginDialogTitle: string;
    loginDialogDescription: string;
    continueWithGoogle: string;
    continueWithGithub: string;
    continueWithExploreIam: string;
    logoutDialogTitle: string;
    logoutConfirm: string;
    logoutCancel: string;
    errors: {
      loginFailed: string;
      logoutFailed: string;
    };
  };
  vision: {
    imageLabel: string;
    resultLabel: string;
    dropText: string;
    dropHint: string;
    analyzing: string;
    startAnalyze: string;
    uploadToAnalyze: string;
    clearImage: string;
    caption: string;
    detect: string;
    ocr: string;
    noImageYet: string;
    noDetections: string;
    processingTime: string;
    clickToEnlarge: string;
    errors: {
      invalidImage: string;
      fileTooLarge: string;
      requestFailed: string;
      processingFailed: string;
      providerUnavailable: string;
    };
  };
  rag: {
    title: string;
    modelBadge: string;
    uploadDocuments: string;
    upload: string;
    askQuestion: string;
    inputPlaceholder: string;
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
    uploading: string;
    basedOn: string;
    openReference: string;
    documentDeleted: string;
    deleteDocument: string;
    fileSelected: string;
    errors: {
      generic: string;
      uploadFailed: string;
      deleteFailed: string;
    };
  };
  pipelines: {
    results: {
      title: string;
      collapse: string;
      expand: string;
      emptyState: string;
      expandMessage: string;
      collapseMessage: string;
    };
    inputPlaceholder: string;
    taskPlaceholder: string;
    defaultMessage: string;
    paletteTitle: string;
    paletteHint: string;
    paletteEmpty: string;
    paletteShow: string;
    paletteHide: string;
    addAgent: string;
    addAgentTitle: string;
    addAgentHint: string;
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
    nodeEditor: {
      title: string;
      hint: string;
      dblclickHint: string;
      description: string;
      systemPrompt: string;
      toolKeys: string;
      toolKeysHint: string;
    };
    templates: {
      title: string;
      use: string;
      skipped: string;
      hint: string;
      addTemplate: string;
      newTemplateName: string;
      editMode: string;
      useMode: string;
      editModeHint: string;
      useModeHint: string;
      backToTemplates: string;
      useThisGraph: string;
      editThisGraph: string;
      previewHint: string;
      emptyState: string;
      saveCanvas: string;
      newTemplate: string;
      customize: string;
      inLibrary: string;
      createTitle: string;
      editTitle: string;
      description: string;
      agentTypes: string;
      agentTypesHint: string;
      useCanvas: string;
      canvasEmpty: string;
      shortTopic: string;
      shortTopicHint: string;
      briefPrompt: string;
      briefPromptHint: string;
      enable: string;
      disable: string;
      statusDisabled: string;
      deleteConfirm: string;
      added: string;
      errors: {
        saveFailed: string;
        updateFailed: string;
        deleteFailed: string;
        nameRequired: string;
      };
    };
    errors: {
      generic: string;
    };
  };
  chat: {
    inputPlaceholder: string;
    welcomeTitle: string;
    welcomeDescription: string;
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
    loadingModels: string;
    a2uiPending: string;
    diagramLabel: string;
    diagramPending: string;
    diagramRendering: string;
    diagramRenderFailed: string;
    diagramExpand: string;
    diagramDownload: string;
  };
  metrics: {
    overviewTitle: string;
    overviewSubtitle: string;
    domainTitle: string;
    domainSubtitle: string;
    dayFilter: string;
    modelFilter: string;
    clearFilters: string;
    range7d: string;
    range30d: string;
    loading: string;
    loadingOverview: string;
    unknownDomain: string;
    unknownDomainTitle: string;
    backToOverview: string;
    openDomainHint: string;
    charts: {
      aiRequests: string;
      aiRequestsEmpty: string;
      requestsByDomain: string;
      requestsByDomainEmpty: string;
      requestsOverTime: string;
      requestsOverTimeEmpty: string;
      callsByModel: string;
      callsByModelEmpty: string;
      documentsByStatus: string;
      documentsByStatusEmpty: string;
    };
    kpi: {
      aiRequests: string;
      errorRate: string;
      p95Latency: string;
      tokens: string;
      sessions: string;
      documents: string;
      requests: string;
      errors: string;
    };
    health: {
      heading: string;
      emptyState: string;
      chat: string;
      rag: string;
      agents: string;
      toolsMcp: string;
      vision: string;
      sessionsDetail: string;
      documentsDetail: string;
      healthyDetail: string;
      toolsDetail: string;
      visionDetail: string;
    };
    drilldown: {
      recentHeading: string;
      filteredHeading: string;
      eventsCount: string;
      emptyState: string;
      time: string;
      operation: string;
      outcome: string;
      latency: string;
      model: string;
    };
    errors: {
      loadOverviewFailed: string;
      loadDomainFailed: string;
    };
  };
  eval: {
    title: string;
    subtitle: string;
    userMessage: string;
    assistantResponse: string;
    evaluate: string;
    evaluating: string;
    overall: string;
    coherence: string;
    relevance: string;
    helpfulness: string;
    factuality: string;
    safetyIssues: string;
    yes: string;
    no: string;
    suggestions: string;
    emptyState: string;
    errors: {
      requestFailed: string;
    };
  };
  speechToText: {
    title: string;
    connectionLabel: string;
    connect: string;
    disconnect: string;
    stop: string;
    testPayload: string;
    transcript: string;
    emptyState: string;
    lastServerMessage: string;
    connectionState: {
      disconnected: string;
      connecting: string;
      connected: string;
      error: string;
    };
    errors: {
      connectionFailed: string;
      notConnected: string;
      generic: string;
    };
  };
  mcp: {
    title: string;
    subtitle: string;
    serverLabel: string;
    clientLabel: string;
    toolsCount: string;
    loading: string;
    tools: string;
    emptyState: string;
    tryWithTools: string;
    placeholder: string;
    send: string;
    sending: string;
    errors: {
      healthFailed: string;
      clientStatusFailed: string;
      toolsFailed: string;
      chatFailed: string;
    };
  };
  agents: {
    title: string;
    subtitle: string;
    newAgent: string;
    builtins: string;
    builtinsHint: string;
    myAgents: string;
    emptyState: string;
    loading: string;
    typeKey: string;
    description: string;
    systemPrompt: string;
    toolKeys: string;
    enable: string;
    disable: string;
    statusDisabled: string;
    deleteConfirm: string;
    customize: string;
    editOverride: string;
    createTitle: string;
    editTitle: string;
    errors: {
      loadFailed: string;
      saveFailed: string;
      deleteFailed: string;
      updateFailed: string;
      nameRequired: string;
    };
  };
  automations: {
    title: string;
    subtitle: string;
    newSchedule: string;
    emptyState: string;
    emptyStateHint: string;
    pipelineTemplate: string;
    pipelineTemplatePlaceholder: string;
    noPipelineTemplates: string;
    email: string;
    timezone: string;
    frequency: string;
    frequencyDaily: string;
    frequencyWeekly: string;
    frequencyCustom: string;
    runAt: string;
    runAtPlaceholder: string;
    runAtHint: string;
    brief: string;
    briefPlaceholder: string;
    enable: string;
    disable: string;
    reschedule: string;
    deleteConfirm: string;
    history: string;
    nextRun: string;
    lastRun: string;
    statusEnabled: string;
    statusDisabled: string;
    statusCompleted: string;
    nextRunNone: string;
    onceCompletedHint: string;
    runStatus: string;
    emailStatus: string;
    errors: {
      pipelineTemplateRequired: string;
      loadFailed: string;
      saveFailed: string;
      deleteFailed: string;
      nameRequired: string;
      emailRequired: string;
      emailInvalid: string;
      briefRequired: string;
      runAtRequired: string;
      runAtPast: string;
    };
  };
  skills: {
    title: string;
    subtitle: string;
    newSkill: string;
    templates: string;
    templatesHint: string;
    yourSkills: string;
    emptyState: string;
    description: string;
    instructions: string;
    enable: string;
    disable: string;
    statusDisabled: string;
    deleteConfirm: string;
    customize: string;
    inLibrary: string;
    createTitle: string;
    editTitle: string;
    added: string;
    errors: {
      loadFailed: string;
      saveFailed: string;
      deleteFailed: string;
      updateFailed: string;
      nameRequired: string;
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
      generate: string;
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
      synthesize: string;
      synthesizing: string;
      audioReady: string;
      downloadAudio: string;
      emptyState: string;
      pause: string;
      play: string;
    };
  };
  sidebar: {
    chatHistory: string;
    newChat: string;
    pinned: string;
    recents: string;
    searchConversations: string;
    openMenu: string;
    collapse: string;
    expand: string;
    closeMenu: string;
    closeOverlay: string;
    deleteChat: string;
    pinChat: string;
    unpinChat: string;
  };
  privacy: {
    consentTitle: string;
    consentBody: string;
    consentLearnMore: string;
    consentAccept: string;
    consentReject: string;
  };
}

export const translations: Record<Language, Translations> = {
  en,
  zh,
  ja,
  fr,
  es,
};

export const languageNames: Record<Language, string> = {
  en: 'English',
  zh: '中文',
  ja: '日本語',
  fr: 'Français',
  es: 'Español',
};
