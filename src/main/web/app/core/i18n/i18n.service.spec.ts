import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
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

    it('should have readonly languageName computed', () => {
      service = new I18nService();
      expect(service.languageName).toBeDefined();
      // eslint-disable-next-line @angular-eslint/no-uncalled-signals
      expect(typeof service.languageName).toBe('function');
    });
  });

  describe('when getting initial language', () => {
    it('should return stored language if valid', () => {
      storage['language'] = 'en';
      const newService = new I18nService();
      expect(newService.language()).toBe('en');
    });

    it('should return stored zh language', () => {
      storage['language'] = 'zh';
      const newService = new I18nService();
      expect(newService.language()).toBe('zh');
    });

    it('should return stored ja language', () => {
      storage['language'] = 'ja';
      const newService = new I18nService();
      expect(newService.language()).toBe('ja');
    });

    it('should return stored fr language', () => {
      storage['language'] = 'fr';
      const newService = new I18nService();
      expect(newService.language()).toBe('fr');
    });

    it('should return stored es language', () => {
      storage['language'] = 'es';
      const newService = new I18nService();
      expect(newService.language()).toBe('es');
    });

    it('should return navigator language if stored is invalid', () => {
      storage['language'] = 'invalid-lang';
      Object.defineProperty(navigator, 'language', {
        value: 'en',
        writable: true,
      });
      const newService = new I18nService();
      expect(newService.language()).toBe('en');
    });

    it('should return zh as default when both stored and navigator are invalid', () => {
      storage['language'] = 'invalid-lang';
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
      expect(storage['language']).toBe('ja');
    });

    it('should switch from zh to en correctly', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.language()).toBe('en');
      expect(storage['language']).toBe('en');
    });

    it('should switch from en to fr correctly', () => {
      storage['language'] = 'en';
      service = new I18nService();
      service.setLanguage('fr');
      expect(service.language()).toBe('fr');
      expect(storage['language']).toBe('fr');
    });

    it('should switch to es correctly', () => {
      service = new I18nService();
      service.setLanguage('es');
      expect(service.language()).toBe('es');
      expect(storage['language']).toBe('es');
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
      expect(enTranslations.nav.imageAnalysis).toBe('Image Analysis');
      expect(zhTranslations.nav.imageAnalysis).toBe('图像分析');
    });

    it('should update languageName computed', () => {
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
      expect(service.t().nav.imageAnalysis).toBe('Image Analysis');
    });

    it('should provide imageUploader translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().imageUploader).toBeDefined();
      expect(service.t().imageUploader.dropText).toBe('Drag & drop or click to upload');
    });

    it('should provide ragChat translations', () => {
      service = new I18nService();
      service.setLanguage('zh');
      expect(service.t().ragChat).toBeDefined();
      expect(service.t().ragChat.title).toBe('文档问答');
    });

    it('should provide agents translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().agents).toBeDefined();
      expect(service.t().agents.pipeline.templates.items.businessAnalysis.name).toBe(
        'Business analysis',
      );
    });

    it('should provide generate translations', () => {
      service = new I18nService();
      service.setLanguage('en');
      expect(service.t().generate).toBeDefined();
      expect(service.t().generate.tabs.image).toBe('Image Gen');
    });
  });
});
