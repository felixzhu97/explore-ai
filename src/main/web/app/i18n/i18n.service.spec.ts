import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { STORAGE_KEYS } from '../storage-keys';
import { I18nService } from './i18n.service';

describe('I18nService', () => {
  let service: I18nService;
  let storage: Record<string, string> = {};

  beforeEach(() => {
    storage = {};

    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage[key] ?? null,
      setItem: (key: string, value: string) => {
        storage[key] = value;
      },
      removeItem: (key: string) => {
        delete storage[key];
      },
      clear: () => {
        storage = {};
      },
    });

    // Mock navigator.language to ensure consistent behavior
    Object.defineProperty(navigator, 'language', {
      value: 'en',
      writable: true,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('when initializing', () => {
    it('should have readonly language signal', () => {
      service = new I18nService();
      expect(service.language).toBeDefined();
      // eslint-disable-next-line @angular-eslint/no-uncalled-signals
      expect(typeof service.language).toBe('function');
    });

    it('should have readonly t computed for translations', () => {
      service = new I18nService();
      expect(service.t).toBeDefined();
      // eslint-disable-next-line @angular-eslint/no-uncalled-signals
      expect(typeof service.t).toBe('function');
    });

    it('should have readonly language name computed', () => {
      service = new I18nService();
      expect(service.languageName).toBeDefined();
      // eslint-disable-next-line @angular-eslint/no-uncalled-signals
      expect(typeof service.languageName).toBe('function');
    });
  });

  describe('when getting initial language', () => {
    it('should return stored language if valid', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'en';
      const newService = new I18nService();
      expect(newService.language()).toBe('en');
    });

    it('should return stored zh language', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'zh';
      const newService = new I18nService();
      expect(newService.language()).toBe('zh');
    });

    it('should return stored ja language', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'ja';
      const newService = new I18nService();
      expect(newService.language()).toBe('ja');
    });

    it('should return stored fr language', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'fr';
      const newService = new I18nService();
      expect(newService.language()).toBe('fr');
    });

    it('should return stored es language', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'es';
      const newService = new I18nService();
      expect(newService.language()).toBe('es');
    });

    it('should return navigator language if stored is invalid', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'invalid-lang';
      Object.defineProperty(navigator, 'language', {
        value: 'en',
        writable: true,
      });
      const newService = new I18nService();
      expect(newService.language()).toBe('en');
    });

    it('should return zh as default when both stored and navigator are invalid', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'invalid-lang';
      Object.defineProperty(navigator, 'language', {
        value: 'invalid',
        writable: true,
      });
      const newService = new I18nService();
      expect(newService.language()).toBe('zh');
    });

    it('should return navigator language when no stored language', () => {
      Object.defineProperty(navigator, 'language', {
        value: 'fr',
        writable: true,
      });
      const newService = new I18nService();
      expect(newService.language()).toBe('fr');
    });
  });

  describe('when setting language', () => {
    it('should update signal and persist to storage', () => {
      service = new I18nService();
      service.setLanguage('ja');
      expect(service.language()).toBe('ja');
      expect(storage[STORAGE_KEYS.LANGUAGE]).toBe('ja');
    });

    it('should switch from zh to en correctly', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.language()).toBe('en');
      expect(storage[STORAGE_KEYS.LANGUAGE]).toBe('en');
    });

    it('should switch from en to fr correctly', () => {
      storage[STORAGE_KEYS.LANGUAGE] = 'en';
      service = new I18nService();
      service.setLanguage('fr');
      expect(service.language()).toBe('fr');
      expect(storage[STORAGE_KEYS.LANGUAGE]).toBe('fr');
    });

    it('should switch to es correctly', () => {
      service = new I18nService();
      service.setLanguage('es');
      expect(service.language()).toBe('es');
      expect(storage[STORAGE_KEYS.LANGUAGE]).toBe('es');
    });
  });

  describe('when language changes', () => {
    it('should update t computed translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      const enTranslations = service.t();

      service.setLanguage('zh');
      const zhTranslations = service.t();

      expect(enTranslations).not.toEqual(zhTranslations);
      expect(enTranslations.nav.vision).toBe('Image Analysis');
      expect(zhTranslations.nav.vision).toBe('图像分析');
    });

    it('should update language name computed', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.languageName()).toBe('English');

      service.setLanguage('zh');
      expect(service.languageName()).toBe('中文');

      service.setLanguage('ja');
      expect(service.languageName()).toBe('日本語');
    });
  });

  describe('tReplace', () => {
    it('should replace single template variable', () => {
      const result = service.tReplace('Hello {name}!', { name: 'World' });
      expect(result).toBe('Hello World!');
    });

    it('should replace multiple template variables', () => {
      const result = service.tReplace('Hello {name}, you have {count} messages', {
        name: 'Alice',
        count: 5,
      });
      expect(result).toBe('Hello Alice, you have 5 messages');
    });

    it('should keep unknown variables as placeholder', () => {
      const result = service.tReplace('Hello {name}!', {});
      expect(result).toBe('Hello {name}!');
    });

    it('should replace with numeric values', () => {
      const result = service.tReplace('Count: {num}', { num: 42 });
      expect(result).toBe('Count: 42');
    });

    it('should handle mixed known and unknown variables', () => {
      const result = service.tReplace('{greeting} {name}!', {
        greeting: 'Hi',
      });
      expect(result).toBe('Hi {name}!');
    });

    it('should replace multiple occurrences of same variable', () => {
      const result = service.tReplace('{name} said: {name}', { name: 'Bob' });
      expect(result).toBe('Bob said: Bob');
    });

    it('should handle empty template', () => {
      const result = service.tReplace('', { name: 'Test' });
      expect(result).toBe('');
    });

    it('should handle template without variables', () => {
      const result = service.tReplace('Hello World!', { name: 'Test' });
      expect(result).toBe('Hello World!');
    });
  });

  describe('translations accessibility', () => {
    it('should provide nav translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().nav).toBeDefined();
      expect(service.t().nav.vision).toBe('Image Analysis');
    });

    it('should provide image uploader translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().vision).toBeDefined();
      expect(service.t().vision.dropText).toBe('Drag & drop or click to upload');
    });

    it('should provide rag chat translations', () => {
      service = new I18nService();
      service.setLanguage('zh');
      expect(service.t().rag).toBeDefined();
      expect(service.t().rag.title).toBe('文档问答');
    });

    it('should provide pipelines translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().pipelines).toBeDefined();
      expect(service.t().pipelines.taskPlaceholder).toBeDefined();
      expect(service.t().pipelines.nodeEditor.title).toBeDefined();
      expect(service.t().pipelines.templates.title).toBe('Workflows');
      expect(service.t().pipelines.templates.addTemplate).toBeDefined();
      expect(service.t().pipelines.templates.saveCanvas).toBeDefined();
    });

    it('should provide generate translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().generate).toBeDefined();
      expect(service.t().generate.tabs.image).toBe('Image Gen');
    });
  });
});
