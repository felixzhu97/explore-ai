import type { Translations } from '../translations.types';

export const fr: Translations = {
  common: {
    loadFailed: 'Échec du chargement',
    saveFailed: 'Échec de la sauvegarde',
    deleteFailed: 'Échec de la suppression',
    operationFailed: 'L\'opération a échoué',
    success: 'Succès',
    error: 'Une erreur s\'est produite',
  },
  nav: {
    imageAnalysis: 'Analyse d\'images',
    documentQA: 'Documents',
    mcp: 'MCP',
    eval: 'Évaluation',
    speechToText: 'Transcription vocale',
    agents: 'Agent',
    kubernetes: 'K8s',
    monitoring: 'Surveillance',
    aiinfra: 'AI Infra',
    chat: 'Chat',
    generation: 'Génération',
    modelDev: 'Dev',
    modelOps: 'Ops',
    model: 'Modèles',
    llmops: 'LLMOps',
    aiops: 'AIOps',
    vectordb: 'VectorDB',
    groups: {
      work: 'Travail',
      create: 'Création',
      lab: 'Labo',
    },
  },
  imageUploader: {
    imageLabel: 'Image',
    resultLabel: 'Résultat d\'analyse',
    dropText: 'Glisser-déposer ou cliquer',
    dropHint: 'JPG, PNG, GIF, WebP',
    analyzing: 'Analyse en cours...',
    startAnalyze: 'Lancer l\'analyse',
    uploadToAnalyze: 'Télécharger une image à analyser',
    selectImageError: 'Veuillez sélectionner un fichier image',
    fileTooLarge: 'L\'image doit faire 50 Mo ou moins',
    requestFailed: 'Échec de la requête',
    processingFailed: 'Échec du traitement',
    clearImage: 'Effacer l\'image',
    caption: 'Description',
    detect: 'Détection',
    ocr: 'OCR',
    noImageYet: 'Téléchargez une image pour commencer',
    noDetections: 'Aucun objet détecté dans cette image',
    processingTime: 'Traité en {ms} ms',
    providerUnavailable: 'Le service d\'analyse d\'image est temporairement indisponible',
    clickToEnlarge: 'Cliquer pour agrandir',
  },
  ragChat: {
    title: 'Q&R Documents',
    modelBadge: 'RAG',
    uploadDocs: 'Documents',
    upload: 'Téléverser',
    askQuestion: 'Posez-moi des questions sur vos documents',
    inputPlaceholder: 'Tapez votre question...',
    thinking: 'Réflexion...',
    errorMessage:
        'Désolé, une erreur s\'est produite. Assurez-vous que le service RAG fonctionne.',
    sources: 'Sources',
    similarity: 'Similarité',
    whatIsThis: 'De quoi s agit-il ?',
    summarize: 'Résumez les points clés',
    keyInfo: 'Quelles sont les informations clés ?',
    explain: 'Expliquez les concepts',
    documents: 'Documents disponibles',
    noDocuments: 'Aucun document téléchargé',
    selectedDocuments: 'Sélectionnés: {count}',
    selectAll: 'Tout sélectionner',
    clearSelection: 'Effacer',
    filesSelected: '{count} fichier(s) sélectionné(s)',
    uploadSuccess: '{name} téléchargé avec succès',
    uploadFailed: 'Échec du téléchargement de {name}',
    uploading: 'Téléversement...',
    basedOn: 'Basé sur {count} source(s)',
    documentDeleted: 'Document supprimé',
    deleteFailed: 'Échec de la suppression, veuillez réessayer',
    fileSelected: '{count} fichier(s) sélectionné(s)',
    openAgentWorkbench: 'Ouvrir le pipeline Agent',
  },
  agents: {
    thinking: 'Réflexion...',
    errorMessage: 'Une erreur s\'est produite. Veuillez réessayer.',
    pipeline: {
      inputPlaceholder: 'Décrivez la tâche du pipeline...',
      defaultMessage: 'Exécutez le pipeline d\'agents configuré pour cette tâche.',
      paletteTitle: 'Agents',
      canvasHint: 'Cliquez ou glissez un agent : les liens se chaînent automatiquement. Ou partez d\'un modèle.',
      clear: 'Effacer',
      run: 'Lancer le pipeline',
      emptyState: {
        title: 'Construire un pipeline multi-étapes',
        description: 'Choisissez un modèle ou ajoutez des agents, définissez la tâche, lancez, puis consultez les résultats.',
      },
      hints: {
        empty: 'Ajoutez au moins un agent worker sur le canevas.',
        needConnections: 'Connectez les nœuds avant d\'exécuter le pipeline.',
        orphan: 'Connectez tous les agents dans un seul chemin.',
        cycle: 'Supprimez les cycles du graphe.',
        invalid: 'Corrigez le pipeline avant de l\'exécuter.',
      },
      templates: {
        title: 'Modèles',
        use: 'Utiliser',
        skipped: 'Agents indisponibles ignorés : {types}',
        items: {
          webResearch: {
            name: 'Recherche web',
            description: 'Rechercher sur le web, puis synthétiser.',
          },
          knowledgeAnswer: {
            name: 'Réponse documentaire',
            description: 'Récupérer des documents, puis synthétiser.',
          },
          weatherBrief: {
            name: 'Brief météo',
            description: 'Appeler les outils météo, puis résumer.',
          },
          businessAnalysis: {
            name: 'Analyse business',
            description: 'Deux passes web + knowledge, brief de stratégie commerciale.',
          },
          techAnalysis: {
            name: 'Analyse technique',
            description: 'Signaux tech + docs du dépôt, brief de faisabilité.',
          },
        },

        shortTopics: {
          webResearch: 'Brief recherche web',
          knowledgeAnswer: 'Réponse documentaire',
          weatherBrief: 'Brief météo',
          businessAnalysis: 'Analyse business produit AI',
          techAnalysis: 'Faisabilité technique stack AI',
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
      title: 'Résultats',
      collapse: 'Réduire',
      expand: 'Afficher les résultats',
      empty: 'Lancez le pipeline pour voir les résultats ici.',
      expandMessage: 'Voir plus',
      collapseMessage: 'Réduire',
    },
  },

  chat: {
    thinking: 'Réflexion...',
    inputPlaceholder: 'Tapez votre message...',
    welcomeTitle: 'Comment puis-je vous aider aujourd\'hui ?',
    welcomeDescription: 'Posez une question — ou ouvrez Agent pour un pipeline multi-étapes',
    openAgentWorkbench: 'Ouvrir le pipeline Agent',
    suggestedPromptsTitle: 'Suggestions',
    suggestedPrompts: [
      {
        key: 'explain',
        label: 'Expliquer un concept',
        description: 'Décomposer un sujet simplement',
      },
      {
        key: 'summarize',
        label: 'Résumer',
        description: 'Obtenir un résumé concis des points clés',
      },
      {
        key: 'brainstorm',
        label: 'Brainstormer',
        description: 'Générer des idées créatives pour un problème',
      },
    ],
  },
  generate: {
    tabs: {
      image: 'Génération d\'images',
      tts: 'Synthèse vocale',
    },
    image: {
      title: 'Génération d\'images',
      description: 'Générez des images avec votre fournisseur configuré (Ollama par défaut)',
      promptLabel: 'Prompt',
      promptPlaceholder: 'Décrivez l\'image que vous voulez générer...',
      negativePromptLabel: 'Prompt négatif',
      negativePromptPlaceholder: 'Ce qu\'il faut éviter dans l\'image...',
      sizeLabel: 'Taille de l\'image',
      generateButton: 'Générer l\'image',
      generating: 'Génération...',
      preview: 'Aperçu',
      download: 'Télécharger',
      emptyState: 'Générez une image pour voir apercu',
      zoomLabel: 'Agrandir l\'image',
    },
    tts: {
      title: 'Synthèse vocale',
      description: 'Convertissez le texte en parole naturelle',
      textLabel: 'Texte',
      textPlaceholder: 'Entrez le texte à convertir en parole...',
      voiceLabel: 'Voix',
      speedLabel: 'Vitesse',
      synthesizeButton: 'Synthétiser',
      synthesizing: 'Synthèse...',
      audioReady: 'Audio prêt',
      downloadAudio: 'Télécharger l\'audio',
    },
  },
  sidebar: {
    chatHistory: 'Historique des chats',
    newChat: 'Nouveau chat',
    pinned: 'Épinglé',
    recents: 'Récents',
    searchConversations: 'Rechercher des conversations...',
  },
};
