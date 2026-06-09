import { describe, it, expect } from 'vitest';
import {
  Language,
  SUPPORTED_LANGUAGES,
  Translations,
  translations,
  languageNames,
} from './i18n.model';

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
      const requiredKeys = ['nav', 'imageUploader', 'ragChat', 'agents', 'aiHub'] as const;

      SUPPORTED_LANGUAGES.forEach((lang) => {
        const langTranslations = translations[lang];
        requiredKeys.forEach((key) => {
          expect(langTranslations[key]).toBeDefined();
        });
      });
    });

    describe('nav translations', () => {
      const requiredNavKeys = [
        'visionAI',
        'documentQA',
        'supervisor',
        'kubernetes',
        'monitoring',
        'aiinfra',
        'aiHub',
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
            expect(Array.isArray(translations[lang].agents.quickPrompts[type])).toBe(true);
            expect(translations[lang].agents.quickPrompts[type].length).toBeGreaterThan(0);
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
            expect(typeof translations[lang].agents.descriptions[type]).toBe('string');
          });
        });
      });
    });

    describe('aiHub translations', () => {
      it('should have aiHub base translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].aiHub.title).toBeDefined();
          expect(translations[lang].aiHub.modelBadge).toBeDefined();
          expect(translations[lang].aiHub.statusText).toBeDefined();
        });
      });

      it('should have tabs translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].aiHub.tabs.chat).toBeDefined();
          expect(translations[lang].aiHub.tabs.image).toBeDefined();
          expect(translations[lang].aiHub.tabs.tts).toBeDefined();
        });
      });

      it('should have chat translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].aiHub.chat.title).toBeDefined();
          expect(translations[lang].aiHub.chat.description).toBeDefined();
          expect(translations[lang].aiHub.chat.placeholder).toBeDefined();
          expect(translations[lang].aiHub.chat.inputPlaceholder).toBeDefined();
          expect(translations[lang].aiHub.chat.thinking).toBeDefined();
          expect(translations[lang].aiHub.chat.error).toBeDefined();
          expect(translations[lang].aiHub.chat.provider).toBeDefined();
          expect(translations[lang].aiHub.chat.model).toBeDefined();
        });
      });

      it('should have image translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].aiHub.image.title).toBeDefined();
          expect(translations[lang].aiHub.image.description).toBeDefined();
          expect(translations[lang].aiHub.image.promptLabel).toBeDefined();
          expect(translations[lang].aiHub.image.promptPlaceholder).toBeDefined();
          expect(translations[lang].aiHub.image.negativePromptLabel).toBeDefined();
          expect(translations[lang].aiHub.image.negativePromptPlaceholder).toBeDefined();
          expect(translations[lang].aiHub.image.sizeLabel).toBeDefined();
          expect(translations[lang].aiHub.image.generateButton).toBeDefined();
          expect(translations[lang].aiHub.image.generating).toBeDefined();
          expect(translations[lang].aiHub.image.preview).toBeDefined();
          expect(translations[lang].aiHub.image.download).toBeDefined();
          expect(translations[lang].aiHub.image.emptyState).toBeDefined();
        });
      });

      it('should have tts translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].aiHub.tts.title).toBeDefined();
          expect(translations[lang].aiHub.tts.description).toBeDefined();
          expect(translations[lang].aiHub.tts.textLabel).toBeDefined();
          expect(translations[lang].aiHub.tts.textPlaceholder).toBeDefined();
          expect(translations[lang].aiHub.tts.voiceLabel).toBeDefined();
          expect(translations[lang].aiHub.tts.speedLabel).toBeDefined();
          expect(translations[lang].aiHub.tts.synthesizeButton).toBeDefined();
          expect(translations[lang].aiHub.tts.synthesizing).toBeDefined();
          expect(translations[lang].aiHub.tts.audioReady).toBeDefined();
          expect(translations[lang].aiHub.tts.downloadAudio).toBeDefined();
        });
      });

      it('should have quickPrompts translations', () => {
        SUPPORTED_LANGUAGES.forEach((lang) => {
          expect(translations[lang].aiHub.quickPrompts.greeting).toBeDefined();
          expect(translations[lang].aiHub.quickPrompts.help).toBeDefined();
          expect(translations[lang].aiHub.quickPrompts.creative).toBeDefined();
        });
      });
    });
  });

  describe('translations content consistency', () => {
    it('should have visionAI as "Vision AI" in all languages', () => {
      SUPPORTED_LANGUAGES.forEach((lang) => {
        expect(translations[lang].nav.visionAI).toBe('Vision AI');
      });
    });

    it('should have aiHub as "AI Hub" in all languages', () => {
      SUPPORTED_LANGUAGES.forEach((lang) => {
        expect(translations[lang].nav.aiHub).toBe('AI Hub');
      });
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
        return matches ? matches.map((m) => m.slice(1, -1)) : [];
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
