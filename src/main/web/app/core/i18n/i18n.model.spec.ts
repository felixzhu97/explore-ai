import { describe, it, expect } from 'vitest';
import { Language, SUPPORTED_LANGUAGES, translations, languageNames } from './i18n.model';

describe('i18n.model', () => {
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
      const requiredKeys = ['nav', 'imageUploader', 'ragChat', 'agents', 'chat', 'generate'] as const;

      SUPPORTED_LANGUAGES.forEach((lang) => {
        const langTranslations = translations[lang];
        requiredKeys.forEach((key) => {
          expect(langTranslations[key]).toBeDefined();
        });
      });
    });

    describe('nav translations', () => {
      const requiredNavKeys = [
        'imageAnalysis',
        'documentQA',
        'supervisor',
        'kubernetes',
        'monitoring',
        'aiinfra',
        'chat',
        'generation',
        'modelDev',
        'modelOps',
        'model',
        'llmops',
        'aiops',
        'vectordb',
      ] as const;

      it('should have nav translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          requiredNavKeys.forEach((key) => {
            expect(translations[lang].nav[key]).toBeDefined();
            expect(typeof translations[lang].nav[key]).toBe('string');
          });
        });
      });
    });

    describe('imageUploader translations', () => {
      const requiredKeys = [
        'imageLabel',
        'resultLabel',
        'dropText',
        'dropHint',
        'analyzing',
        'startAnalyze',
        'uploadToAnalyze',
        'selectImageError',
        'requestFailed',
        'processingFailed',
        'clearImage',
        'caption',
        'detect',
        'ocr',
        'noImageYet',
      ] as const;

      it('should have imageUploader translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          requiredKeys.forEach((key) => {
            expect(translations[lang].imageUploader[key]).toBeDefined();
            expect(typeof translations[lang].imageUploader[key]).toBe('string');
          });
        });
      });
    });

    describe('ragChat translations', () => {
      const requiredKeys = [
        'title',
        'modelBadge',
        'uploadDocs',
        'upload',
        'askQuestion',
        'inputPlaceholder',
        'thinking',
        'errorMessage',
        'sources',
        'similarity',
        'whatIsThis',
        'summarize',
        'keyInfo',
        'explain',
        'documents',
        'noDocuments',
        'selectedDocuments',
        'selectAll',
        'clearSelection',
        'filesSelected',
        'uploadSuccess',
        'uploadFailed',
        'uploading',
        'basedOn',
        'documentDeleted',
        'deleteFailed',
        'fileSelected',
      ] as const;

      it('should have ragChat translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          requiredKeys.forEach((key) => {
            expect(translations[lang].ragChat[key]).toBeDefined();
            expect(typeof translations[lang].ragChat[key]).toBe('string');
          });
        });
      });
    });

    describe('agents translations', () => {
      it('should have agents translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].agents.startConversation).toBeDefined();
          expect(translations[lang].agents.inputPlaceholder).toBeDefined();
          expect(translations[lang].agents.thinking).toBeDefined();
          expect(translations[lang].agents.errorMessage).toBeDefined();
        });
      });

      it('should have quickPrompts for all agent types', () => {
        const agentTypes = [
          'supervisor',
          'k8s',
          'monitoring',
          'model',
          'llmops',
          'aiops',
          'vectordb',
        ] as const;

        SUPPORTED_LANGUAGES.forEach((lang) => {
          agentTypes.forEach((type) => {
            expect(translations[lang].agents.quickPrompts[type]).toBeDefined();
            expect(
              Array.isArray(translations[lang].agents.quickPrompts[type]),
            ).toBe(true);
            expect(
              translations[lang].agents.quickPrompts[type].length,
            ).toBeGreaterThan(0);
          });
        });
      });

      it('should have descriptions for all agent types', () => {
        const agentTypes = [
          'supervisor',
          'k8s',
          'monitoring',
          'model',
          'llmops',
          'aiops',
          'vectordb',
        ] as const;

        SUPPORTED_LANGUAGES.forEach((lang) => {
          agentTypes.forEach((type) => {
            expect(translations[lang].agents.descriptions[type]).toBeDefined();
            expect(
              typeof translations[lang].agents.descriptions[type],
            ).toBe('string');
          });
        });
      });
    });

    describe('chat translations', () => {
      it('should have chat translations for all languages', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].chat.thinking).toBeDefined();
          expect(translations[lang].chat.inputPlaceholder).toBeDefined();
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
          expect(translations[lang].generate.image.generateButton).toBeDefined();
          expect(translations[lang].generate.image.generating).toBeDefined();
          expect(translations[lang].generate.image.preview).toBeDefined();
          expect(translations[lang].generate.image.download).toBeDefined();
          expect(translations[lang].generate.image.emptyState).toBeDefined();
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
          expect(translations[lang].generate.tts.synthesizeButton).toBeDefined();
          expect(translations[lang].generate.tts.synthesizing).toBeDefined();
          expect(translations[lang].generate.tts.audioReady).toBeDefined();
          expect(translations[lang].generate.tts.downloadAudio).toBeDefined();
        });
      });
    });
  });

  describe('translations content consistency', () => {
    it('should have localized imageAnalysis nav labels', () => {
      expect(translations.en.nav.imageAnalysis).toBe('Image Analysis');
      expect(translations.zh.nav.imageAnalysis).toBe('图像分析');
      expect(translations.ja.nav.imageAnalysis).toBe('画像分析');
      expect(translations.fr.nav.imageAnalysis).toBe('Analyse d\'images');
      expect(translations.es.nav.imageAnalysis).toBe('Análisis de imágenes');
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
        const ragChatTranslations = translations[lang].ragChat;
        const variables = [
          ...extractVariables(ragChatTranslations.selectedDocuments),
          ...extractVariables(ragChatTranslations.filesSelected),
          ...extractVariables(ragChatTranslations.uploadSuccess),
          ...extractVariables(ragChatTranslations.uploadFailed),
          ...extractVariables(ragChatTranslations.basedOn),
          ...extractVariables(ragChatTranslations.fileSelected),
        ];

        expect(variables).toContain('count');
        expect(variables).toContain('name');
      });
    });
  });
});
