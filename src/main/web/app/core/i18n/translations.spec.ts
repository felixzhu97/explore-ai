import { describe, it, expect } from 'vitest';
import { Language, SUPPORTED_LANGUAGES, translations, languageNames } from './translations';

describe('translations', () => {
  describe('Language type', () => {
    it('should be a union of valid language codes', () => {
      const validLanguages: Language[] = ['en', 'zh', 'ja', 'fr', 'es'];
      validLanguages.forEach((lang) => {
        expect(['en', 'zh', 'ja', 'fr', 'es']).toContain(lang);
      });
    });
  });

  describe('SUPPORTED_LANGUAGES', () => {
    it('should contain all supported languages', () => {
      expect(SUPPORTED_LANGUAGES).toHaveLength(5);
      expect(SUPPORTED_LANGUAGES).toContain('en');
      expect(SUPPORTED_LANGUAGES).toContain('zh');
      expect(SUPPORTED_LANGUAGES).toContain('ja');
      expect(SUPPORTED_LANGUAGES).toContain('fr');
      expect(SUPPORTED_LANGUAGES).toContain('es');
    });

    it('should have unique language codes', () => {
      const uniqueLanguages = new Set(SUPPORTED_LANGUAGES);
      expect(uniqueLanguages.size).toBe(SUPPORTED_LANGUAGES.length);
    });
  });

  describe('translations object structure', () => {
    it('should have translations for all supported languages', () => {
      SUPPORTED_LANGUAGES.forEach((lang) => {
        expect(translations[lang]).toBeDefined();
      });
    });

    it('should have required top-level keys for all languages', () => {
      const requiredKeys = [
        'nav',
        'vision',
        'rag',
        'pipelines',
        'chat',
        'generate',
        'metrics',
        'eval',
        'speechToText',
        'mcp',
      ] as const;

      SUPPORTED_LANGUAGES.forEach((lang) => {
        const langTranslations = translations[lang];
        requiredKeys.forEach((key) => {
          expect(langTranslations[key]).toBeDefined();
        });
      });
    });

    describe('nav translations', () => {
      const requiredNavKeys = [
        'vision',
        'rag',
        'mcp',
        'eval',
        'speechToText',
        'pipelines',
        'skills',
        'kubernetes',
        'monitoring',
        'aiInfra',
        'chat',
        'generate',
        'modelDev',
        'modelOps',
        'model',
        'llmOps',
        'aiOps',
        'vectorDb',
        'more',
      ] as const;

      it('should have nav translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          requiredNavKeys.forEach((key) => {
            expect(translations[lang].nav[key]).toBeDefined();
            expect(typeof translations[lang].nav[key]).toBe('string');
          });
        });
      });

      it('should have nav group labels when all languages loaded', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].nav.groups.work).toBeDefined();
          expect(translations[lang].nav.groups.create).toBeDefined();
          expect(translations[lang].nav.groups.lab).toBeDefined();
        });
      });
    });

    describe('vision translations', () => {
      const requiredKeys = [
        'imageLabel',
        'resultLabel',
        'dropText',
        'dropHint',
        'analyzing',
        'startAnalyze',
        'uploadToAnalyze',
        'clearImage',
        'caption',
        'detect',
        'ocr',
        'noImageYet',
        'clickToEnlarge',
      ] as const;

      it('should have vision translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          requiredKeys.forEach((key) => {
            expect(translations[lang].vision[key]).toBeDefined();
            expect(typeof translations[lang].vision[key]).toBe('string');
          });
        });
      });
    });

    describe('rag translations', () => {
      const requiredKeys = [
        'title',
        'modelBadge',
        'uploadDocuments',
        'upload',
        'askQuestion',
        'inputPlaceholder',
        'sources',
        'similarity',
        'whatIsThis',
        'summarize',
        'keyInfo',
        'explain',
        'documents',
        'documentsShort',
        'showDocuments',
        'hideDocuments',
        'noDocuments',
        'selectedDocuments',
        'selectAll',
        'clearSelection',
        'filesSelected',
        'uploadSuccess',
        'uploading',
        'basedOn',
        'openReference',
        'documentDeleted',
        'fileSelected',
      ] as const;

      it('should have rag translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          requiredKeys.forEach((key) => {
            expect(translations[lang].rag[key]).toBeDefined();
            expect(typeof translations[lang].rag[key]).toBe('string');
          });
        });
      });
    });

    describe('pipelines translations', () => {
      it('should have pipelines translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].common.thinking).toBeDefined();
          expect(translations[lang].pipelines.errors.generic).toBeDefined();
          expect(translations[lang].pipelines.inputPlaceholder).toBeDefined();
          expect(translations[lang].pipelines.taskPlaceholder).toBeDefined();
          expect(translations[lang].pipelines.paletteShow).toBeDefined();
          expect(translations[lang].pipelines.paletteHide).toBeDefined();
          expect(translations[lang].pipelines.nodeEditor.title).toBeDefined();
          expect(translations[lang].pipelines.emptyState.title).toBeDefined();
        });
      });

      it('should have pipeline template chrome translations', () => {
        const chromeKeys = [
          'title',
          'use',
          'skipped',
          'editMode',
          'useMode',
          'editModeHint',
          'useModeHint',
          'backToTemplates',
          'useThisGraph',
          'editThisGraph',
          'previewHint',
          'addTemplate',
          'newTemplateName',
          'saveCanvas',
          'newTemplate',
          'agentTypesHint',
          'shortTopicHint',
          'briefPromptHint',
        ] as const;

        SUPPORTED_LANGUAGES.forEach((lang) => {
          const templates = translations[lang].pipelines.templates;
          chromeKeys.forEach((key) => {
            expect(templates[key].length).toBeGreaterThan(0);
          });
          expect(translations[lang].pipelines.results.title).toBeDefined();
        });
      });
    });

    describe('chat translations', () => {
      it('should have chat translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].common.thinking).toBeDefined();
          expect(translations[lang].chat.inputPlaceholder).toBeDefined();
          expect(translations[lang].chat.welcomeTitle).toBeDefined();
          expect(translations[lang].chat.welcomeDescription).toBeDefined();
          expect(translations[lang].chat.suggestedPromptsTitle).toBeDefined();
          expect(translations[lang].chat.suggestedPrompts).toHaveLength(3);
          expect(translations[lang].chat.loadingModels).toBeDefined();
        });
      });
    });

    describe('lab page translations', () => {
      it('should have metrics page chrome for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].metrics.overviewTitle.length).toBeGreaterThan(0);
          expect(translations[lang].metrics.kpi.aiRequests.length).toBeGreaterThan(0);
          expect(translations[lang].metrics.drilldown.eventsCount).toContain('{total}');
        });
      });

      it('should have eval page asr page and mcp page for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].eval.evaluate.length).toBeGreaterThan(0);
          expect(
            translations[lang].speechToText.connectionState.connected.length,
          ).toBeGreaterThan(0);
          expect(translations[lang].mcp.toolsCount).toContain('{count}');
        });
      });
    });

    describe('generate translations', () => {
      it('should have generate tabs translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].generate.tabs.image).toBeDefined();
          expect(translations[lang].generate.tabs.tts).toBeDefined();
        });
      });

      it('should have image translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].generate.image.title).toBeDefined();
          expect(translations[lang].generate.image.description).toBeDefined();
          expect(translations[lang].generate.image.promptLabel).toBeDefined();
          expect(translations[lang].generate.image.promptPlaceholder).toBeDefined();
          expect(translations[lang].generate.image.negativePromptLabel)
            .toBeDefined();
          expect(translations[lang].generate.image.negativePromptPlaceholder)
            .toBeDefined();
          expect(translations[lang].generate.image.sizeLabel).toBeDefined();
          expect(translations[lang].generate.image.generate).toBeDefined();
          expect(translations[lang].generate.image.generating).toBeDefined();
          expect(translations[lang].generate.image.preview).toBeDefined();
          expect(translations[lang].generate.image.download).toBeDefined();
          expect(translations[lang].generate.image.emptyState).toBeDefined();
          expect(translations[lang].generate.image.zoomLabel).toBeDefined();
        });
      });

      it('should have tts translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].generate.tts.title).toBeDefined();
          expect(translations[lang].generate.tts.description).toBeDefined();
          expect(translations[lang].generate.tts.textLabel).toBeDefined();
          expect(translations[lang].generate.tts.textPlaceholder).toBeDefined();
          expect(translations[lang].generate.tts.voiceLabel).toBeDefined();
          expect(translations[lang].generate.tts.speedLabel).toBeDefined();
          expect(translations[lang].generate.tts.synthesize).toBeDefined();
          expect(translations[lang].generate.tts.synthesizing).toBeDefined();
          expect(translations[lang].generate.tts.audioReady).toBeDefined();
          expect(translations[lang].generate.tts.downloadAudio).toBeDefined();
          expect(translations[lang].generate.tts.emptyState).toBeDefined();
        });
      });
    });
  });

  describe('translations content consistency', () => {
    it('should have localized image analysis nav labels', () => {
      expect(translations.en.nav.vision).toBe('Image Analysis');
      expect(translations.zh.nav.vision).toBe('图像分析');
      expect(translations.ja.nav.vision).toBe('画像分析');
      expect(translations.fr.nav.vision).toBe('Analyse d\'images');
      expect(translations.es.nav.vision).toBe('Análisis de imágenes');
    });

    it('should have chat nav label in all languages', () => {
      expect(translations.en.nav.chat).toBe('Chat');
      expect(translations.zh.nav.chat).toBe('对话');
    });

    it('should have kubernetes as "K8s" in all languages', () => {
      SUPPORTED_LANGUAGES.forEach((lang) => {
        expect(translations[lang].nav.kubernetes).toBe('K8s');
      });
    });
  });

  describe('languageNames', () => {
    it('should have names for all supported languages', () => {
      SUPPORTED_LANGUAGES.forEach((lang) => {
        expect(languageNames[lang]).toBeDefined();
        expect(typeof languageNames[lang]).toBe('string');
        expect(languageNames[lang].length).toBeGreaterThan(0);
      });
    });

    it('should have correct language names', () => {
      expect(languageNames.en).toBe('English');
      expect(languageNames.zh).toBe('中文');
      expect(languageNames.ja).toBe('日本語');
      expect(languageNames.fr).toBe('Français');
      expect(languageNames.es).toBe('Español');
    });
  });

  describe('template variable consistency', () => {
    it('should have consistent template variables across languages', () => {
      const extractVariables = (str: string) => {
        const matches = str.match(/\{(\w+)\}/g);
        return matches ? matches.map(m => m.slice(1, -1)) : [];
      };

      SUPPORTED_LANGUAGES.forEach((lang) => {
        const ragTranslations = translations[lang].rag;
        const variables = [
          ...extractVariables(ragTranslations.selectedDocuments),
          ...extractVariables(ragTranslations.filesSelected),
          ...extractVariables(ragTranslations.uploadSuccess),
          ...extractVariables(ragTranslations.errors.uploadFailed),
          ...extractVariables(ragTranslations.basedOn),
          ...extractVariables(ragTranslations.fileSelected),
        ];

        expect(variables).toContain('count');
        expect(variables).toContain('name');
      });
    });
  });
});
