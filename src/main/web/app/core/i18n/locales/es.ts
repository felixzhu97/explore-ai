import type { Translations } from '../translations.types';

export const es: Translations = {
  common: {
    loadFailed: 'Error al cargar datos',
    saveFailed: 'Error al guardar',
    deleteFailed: 'Error al eliminar',
    operationFailed: 'La operación falló',
    success: 'Éxito',
    error: 'Se produjo un error',
  },
  nav: {
    imageAnalysis: 'Análisis de imágenes',
    documentQA: 'Documentos',
    mcp: 'MCP',
    eval: 'Evaluación',
    speechToText: 'Voz a texto',
    agents: 'Agent',
    kubernetes: 'K8s',
    monitoring: 'Monitoreo',
    aiinfra: 'AI Infra',
    chat: 'Chat',
    generation: 'Generación',
    modelDev: 'Desarrollo',
    modelOps: 'Operaciones',
    model: 'Modelos',
    llmops: 'LLMOps',
    aiops: 'AIOps',
    vectordb: 'VectorDB',
    groups: {
      work: 'Trabajo',
      create: 'Crear',
      lab: 'Lab',
    },
  },
  imageUploader: {
    imageLabel: 'Imagen',
    resultLabel: 'Resultado',
    dropText: 'Arrastrar o hacer clic',
    dropHint: 'JPG, PNG, GIF, WebP',
    analyzing: 'Analizando...',
    startAnalyze: 'Analizar',
    uploadToAnalyze: 'Subir imagen para analizar',
    selectImageError: 'Seleccione un archivo de imagen',
    fileTooLarge: 'La imagen debe ser de 50 MB o menos',
    requestFailed: 'Solicitud fallida',
    processingFailed: 'Procesamiento fallido',
    clearImage: 'Borrar imagen',
    caption: 'Descripción',
    detect: 'Detección',
    ocr: 'OCR',
    noImageYet: 'Suba una imagen para comenzar',
    noDetections: 'No se detectaron objetos en esta imagen',
    processingTime: 'Procesado en {ms} ms',
    providerUnavailable: 'El servicio de análisis de imágenes no está disponible temporalmente',
    clickToEnlarge: 'Clic para ampliar',
  },
  ragChat: {
    title: 'Q&A Documentos',
    modelBadge: 'RAG',
    uploadDocs: 'Documentos',
    upload: 'Subir',
    askQuestion: 'Pregúntame sobre tus documentos',
    inputPlaceholder: 'Escribe tu pregunta...',
    thinking: 'Pensando...',
    errorMessage:
        'Lo sentimos, occurred un error. Asegúrese de que el servicio RAG está funcionando.',
    sources: 'Fuentes',
    similarity: 'Similitud',
    whatIsThis: '¿De qué trata esto?',
    summarize: 'Resume los puntos clave',
    keyInfo: '¿Cuáles son los detalles clave?',
    explain: 'Explica los conceptos',
    documents: 'Documentos disponibles',
    documentsShort: 'Docs',
    showDocuments: 'Mostrar documentos',
    hideDocuments: 'Ocultar documentos',
    noDocuments: 'No hay documentos subidos',
    selectedDocuments: 'Seleccionados: {count}',
    selectAll: 'Seleccionar todo',
    clearSelection: 'Limpiar',
    filesSelected: '{count} archivo(s) seleccionado(s)',
    uploadSuccess: '{name} subido correctamente',
    uploadFailed: 'Error al subir {name}',
    uploading: 'Subiendo...',
    basedOn: 'Basado en {count} fuente(s)',
    documentDeleted: 'Documento eliminado',
    deleteFailed: 'Error al eliminar, por favor inténtelo de nuevo',
    fileSelected: '{count} archivo(s) seleccionado(s)',
    openAgentWorkbench: 'Abrir pipeline de Agent',
  },
  agents: {
    thinking: 'Pensando...',
    errorMessage: 'Ocurrió un error. Por favor, inténtalo de nuevo.',
    pipeline: {
      inputPlaceholder: 'Describe la tarea del pipeline...',
      defaultMessage: 'Ejecuta el pipeline de agentes configurado para esta tarea.',
      paletteTitle: 'Agents',
      canvasHint: 'Haz clic o arrastra agentes: se encadenan solos. O empieza con una plantilla.',
      clear: 'Limpiar',
      run: 'Ejecutar pipeline',
      emptyState: {
        title: 'Arma un pipeline de varios pasos',
        description: 'Elige una plantilla o añade agentes, define la tarea, ejecuta y revisa los resultados.',
      },
      hints: {
        empty: 'Añade al menos un agente worker al lienzo.',
        needConnections: 'Conecta los nodos antes de ejecutar el pipeline.',
        orphan: 'Conecta todos los agentes en una sola ruta.',
        cycle: 'Elimina los ciclos del grafo.',
        invalid: 'Corrige el pipeline antes de ejecutarlo.',
      },
      templates: {
        title: 'Plantillas',
        use: 'Usar',
        skipped: 'Agentes no disponibles omitidos: {types}',
        items: {
          webResearch: {
            name: 'Investigación web',
            description: 'Buscar en la web y luego sintetizar.',
          },
          knowledgeAnswer: {
            name: 'Respuesta de conocimiento',
            description: 'Recuperar documentos y luego sintetizar.',
          },
          weatherBrief: {
            name: 'Brief del clima',
            description: 'Usar herramientas del clima y resumir.',
          },
          businessAnalysis: {
            name: 'Análisis de negocio',
            description: 'Dos pases web + conocimiento, brief de estrategia comercial.',
          },
          techAnalysis: {
            name: 'Análisis técnico',
            description: 'Señales tech + docs del repo, brief de viabilidad.',
          },
        },

        shortTopics: {
          webResearch: 'Brief de investigación web',
          knowledgeAnswer: 'Respuesta de conocimiento',
          weatherBrief: 'Brief del clima',
          businessAnalysis: 'Análisis de negocio producto AI',
          techAnalysis: 'Viabilidad técnica stack AI',
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
      title: 'Resultados',
      collapse: 'Contraer',
      expand: 'Expandir resultados',
      empty: 'Ejecuta el pipeline para ver los resultados aquí.',
      expandMessage: 'Ver más',
      collapseMessage: 'Ver menos',
    },
  },

  chat: {
    thinking: 'Pensando...',
    inputPlaceholder: 'Escribe tu mensaje...',
    welcomeTitle: '¿En qué puedo ayudarte hoy?',
    welcomeDescription: 'Pregunta lo que quieras — o abre Agent para pipelines multi-paso',
    openAgentWorkbench: 'Abrir pipeline de Agent',
    suggestedPromptsTitle: 'Sugerencias',
    suggestedPrompts: [
      {
        key: 'explain',
        label: 'Explicar un concepto',
        description: 'Desglosa un tema de forma sencilla',
      },
      {
        key: 'summarize',
        label: 'Resumir',
        description: 'Obtén un resumen conciso de los puntos clave',
      },
      {
        key: 'brainstorm',
        label: 'Generar ideas',
        description: 'Crea opciones creativas para un problema',
      },
    ],
  },
  sender: {
    openActions: 'Abrir acciones',
    filterPlaceholder: 'Filtrar acciones…',
    empty: 'No hay acciones coincidentes',
    removeTool: 'Quitar herramienta',
    toolIntent: 'Usa la herramienta {name} para ayudar con esta solicitud.',
    groups: {
      tools: 'Herramientas',
      agents: 'Agents',
      navigate: 'Navegar',
      session: 'Sesión',
    },
    actions: {
      openAgentPipeline: 'Abrir pipeline de Agent',
      openAgentPipelineHint: 'Crear y ejecutar un pipeline de varios pasos',
      newChat: 'Nuevo chat',
      toggleTools: 'Alternar herramientas',
    },
  },
  generate: {
    tabs: {
      image: 'Generación de imágenes',
      tts: 'Texto a voz',
    },
    image: {
      title: 'Generación de imágenes',
      description: 'Genera imágenes con tu proveedor configurado (Ollama por defecto)',
      promptLabel: 'Prompt',
      promptPlaceholder: 'Describe la imagen que quieres generar...',
      negativePromptLabel: 'Prompt negativo',
      negativePromptPlaceholder: 'Lo que evitar en la imagen...',
      sizeLabel: 'Tamaño de imagen',
      generateButton: 'Generar imagen',
      generating: 'Generando...',
      preview: 'Vista previa',
      download: 'Descargar',
      emptyState: 'Genera una imagen para ver la vista previa',
      zoomLabel: 'Ampliar imagen',
    },
    tts: {
      title: 'Texto a voz',
      description: 'Convierte texto a voz natural',
      textLabel: 'Texto',
      textPlaceholder: 'Ingresa el texto a convertir a voz...',
      voiceLabel: 'Voz',
      speedLabel: 'Velocidad',
      synthesizeButton: 'Sintetizar',
      synthesizing: 'Sintetizando...',
      audioReady: 'Audio listo',
      downloadAudio: 'Descargar audio',
      emptyState: 'Sintetiza para oír la vista previa',
    },
  },
  sidebar: {
    chatHistory: 'Historial de chats',
    newChat: 'Nuevo chat',
    pinned: 'Fijado',
    recents: 'Recientes',
    searchConversations: 'Buscar conversaciones...',
  },
};
