import { Service, signal, computed } from '@angular/core';
import { STORAGE_KEYS } from '../storage-keys';
import { type Language, type Translations, translations, languageNames } from './translations';
import { hasText } from '../shared/presence';

@Service()
export class I18nService {
  readonly #languageState = signal<Language>(this.#getInitialLanguage());

  readonly language = this.#languageState.asReadonly();
  readonly t = computed<Translations>(() => translations[this.#languageState()]);
  readonly languageName = computed(() => languageNames[this.#languageState()]);

  /** Switches the UI language and remembers it. */
  setLanguage(lang: Language): void {
    this.#languageState.set(lang);
    localStorage.setItem(STORAGE_KEYS.LANGUAGE, lang);
  }

  /** Fills the {placeholders} in a translated string. */
  tReplace(template: string, values: Record<string, string | number>): string {
    return template.replace(/\{(\w+)\}/g, (_, key: string) => String(values[key] ?? `{${key}}`));
  }

  #getInitialLanguage(): Language {
    const stored = localStorage.getItem(STORAGE_KEYS.LANGUAGE);
    if (hasText(stored) && this.#isValidLanguage(stored)) {
      return stored as Language;
    }

    const [browserLang = ''] = navigator.language.split('-');
    if (this.#isValidLanguage(browserLang)) {
      return browserLang as Language;
    }

    return 'zh';
  }

  #isValidLanguage(lang: string): boolean {
    return ['en', 'zh', 'ja', 'fr', 'es'].includes(lang);
  }
}
