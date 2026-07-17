import type { Translations } from '../translations.types';

export const en: Translations = {
  common: {
    loadFailed: 'Failed to load data',
    saveFailed: 'Failed to save',
    deleteFailed: 'Failed to delete',
    operationFailed: 'Operation failed',
    success: 'Success',
    error: 'An error occurred',
  },
  nav: {
    imageAnalysis: 'Image Analysis',
    documentQA: 'Document QA',
    mcp: 'MCP',
    eval: 'Eval',
    speechToText: 'Speech to Text',
    supervisor: 'Supervisor',
    kubernetes: 'K8s',
    monitoring: 'Monitoring',
    aiinfra: 'AI Infra',
    chat: 'Chat',
    generation: 'Generation',
    modelDev: 'Dev',
    modelOps: 'Ops',
    model: 'Models',
    llmops: 'LLMOps',
    aiops: 'AIOps',
    vectordb: 'VectorDB',
  },
  imageUploader: {
    imageLabel: 'Image',
    resultLabel: 'Analysis Result',
    dropText: 'Drag & drop or click to upload',
    dropHint: 'JPG, PNG, GIF, WebP',
    analyzing: 'Analyzing...',
    startAnalyze: 'Analyze',
    uploadToAnalyze: 'Upload an image to analyze',
    selectImageError: 'Please select an image file',
    fileTooLarge: 'Image must be 50MB or smaller',
    requestFailed: 'Request failed',
    processingFailed: 'Processing failed',
    clearImage: 'Clear image',
    caption: 'Caption',
    detect: 'Detect',
    ocr: 'OCR',
    noImageYet: 'Upload an image to get started',
    noDetections: 'No objects detected in this image',
    processingTime: 'Processed in {ms} ms',
    providerUnavailable: 'Vision service is temporarily unavailable',
    clickToEnlarge: 'Click to enlarge',
  },
  ragChat: {
    title: 'Document Q&A',
    modelBadge: 'RAG',
    uploadDocs: 'Upload Docs',
    upload: 'Upload',
    askQuestion: 'Ask me anything about your documents',
    inputPlaceholder: 'Type your question...',
    thinking: 'Thinking...',
    errorMessage: 'Sorry, an error occurred. Please ensure the RAG service is running.',
    sources: 'Sources',
    similarity: 'Similarity',
    whatIsThis: 'What is this about?',
    summarize: 'Summarize the key points',
    keyInfo: 'What are the key details?',
    explain: 'Explain the concepts',
    documents: 'Available Documents',
    noDocuments: 'No documents uploaded yet',
    selectedDocuments: 'Selected: {count}',
    selectAll: 'Select All',
    clearSelection: 'Clear',
    filesSelected: '{count} file(s) selected',
    uploadSuccess: '{name} uploaded successfully',
    uploadFailed: 'Failed to upload {name}',
    uploading: 'Uploading...',
    basedOn: 'Based on {count} source(s)',
    documentDeleted: 'Document deleted',
    deleteFailed: 'Delete failed, please retry',
    fileSelected: '{count} file(s) selected',
  },
  agents: {
    thinking: 'Thinking...',
    errorMessage: 'An error occurred. Please try again.',
    pipeline: {
      inputPlaceholder: 'Enter a short topic for this pipeline...',
      defaultMessage: 'Execute the configured agent pipeline for this task.',
      paletteTitle: 'Agents',
      canvasHint: 'Click or drag agents onto the canvas — edges chain automatically. Or start from a template.',
      clear: 'Clear',
      run: 'Run pipeline',
      emptyState: {
        title: 'Start with a template',
        description: 'Pick a pipeline, edit the task below, then run. You can also add agents from the left.',
      },
      hints: {
        empty: 'Add at least one worker agent to the canvas.',
        needConnections: 'Connect agent nodes before running the pipeline.',
        orphan: 'Connect every agent into one pipeline path.',
        cycle: 'Remove cycles from the pipeline graph.',
        invalid: 'Fix the pipeline graph before running.',
      },
      templates: {
        title: 'Templates',
        use: 'Use',
        skipped: 'Skipped unavailable agents: {types}',
        items: {
          webResearch: {
            name: 'Web research',
            description: 'Search the web, then synthesize a brief.',
          },
          knowledgeAnswer: {
            name: 'Knowledge answer',
            description: 'Retrieve documents, then synthesize an answer.',
          },
          weatherBrief: {
            name: 'Weather brief',
            description: 'Look up weather tools, then summarize for the user.',
          },
          businessAnalysis: {
            name: 'Business analysis',
            description:
              'Two web research passes, knowledge-base context, then a commercial strategy brief.',
          },
          techAnalysis: {
            name: 'Tech analysis',
            description:
              'Two research passes on tech signals, codebase knowledge, then a feasibility brief.',
          },
        },
        shortTopics: {
          webResearch: 'Web research brief',
          knowledgeAnswer: 'Knowledge-base answer',
          weatherBrief: 'Weather brief',
          businessAnalysis: 'AI product business analysis',
          techAnalysis: 'AI stack technical feasibility',
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
      title: 'Results',
      collapse: 'Collapse',
      expand: 'Expand results',
      empty: 'Run the pipeline to see results here.',
      expandMessage: 'Show more',
      collapseMessage: 'Show less',
    },
  },
  chat: {
    thinking: 'Thinking...',
    inputPlaceholder: 'Type your message...',
    welcomeTitle: 'How can I help you today?',
    welcomeDescription: 'Start a conversation with the agent',
    suggestedPromptsTitle: 'Suggested prompts',
    suggestedPrompts: [
      {
        key: 'explain',
        label: 'Explain a concept',
        description: 'Break down a topic in simple terms',
      },
      {
        key: 'summarize',
        label: 'Summarize this',
        description: 'Get a concise summary of key points',
      },
      {
        key: 'brainstorm',
        label: 'Brainstorm ideas',
        description: 'Generate creative options for a problem',
      },
    ],
  },
  generate: {
    tabs: {
      image: 'Image Gen',
      tts: 'Text to Speech',
    },
    image: {
      title: 'Image Generation',
      description: 'Generate images with your configured provider (Ollama by default)',
      promptLabel: 'Prompt',
      promptPlaceholder: 'Describe the image you want to generate...',
      negativePromptLabel: 'Negative Prompt',
      negativePromptPlaceholder: 'What to avoid in the image...',
      sizeLabel: 'Image Size',
      generateButton: 'Generate Image',
      generating: 'Generating...',
      preview: 'Preview',
      download: 'Download',
      emptyState: 'Generate an image to see preview',
      zoomLabel: 'Zoom image',
    },
    tts: {
      title: 'Text to Speech',
      description: 'Convert text to natural speech',
      textLabel: 'Text',
      textPlaceholder: 'Enter text to convert to speech...',
      voiceLabel: 'Voice',
      speedLabel: 'Speed',
      synthesizeButton: 'Synthesize',
      synthesizing: 'Synthesizing...',
      audioReady: 'Audio ready',
      downloadAudio: 'Download Audio',
    },
  },
  sidebar: {
    chatHistory: 'Chat History',
    newChat: 'New Chat',
    pinned: 'Pinned',
    recents: 'Recents',
    searchConversations: 'Search conversations...',
  },
};
