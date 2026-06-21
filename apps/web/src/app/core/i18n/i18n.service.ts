import { Injectable, signal, computed } from '@angular/core';
import { Language, Translations, translations, languageNames } from './i18n.model';

@Injectable({
  providedIn: 'root',
})
export class I18nService {
  private readonly _language = signal<Language>(this.getInitialLanguage());

  readonly language = this._language.asReadonly();
  readonly t = computed<Translations>(() => translations[this._language()]);
  readonly languageName = computed(() => languageNames[this._language()]);

  private getInitialLanguage(): Language {
    const stored = localStorage.getItem('language');
    if (stored && this.isValidLanguage(stored)) {
      return stored as Language;
    }

    const browserLang = navigator.language.split('-')[0];
    if (this.isValidLanguage(browserLang)) {
      return browserLang as Language;
    }

    return 'zh';
  }

  private isValidLanguage(lang: string): boolean {
    return ['en', 'zh', 'ja', 'fr', 'es'].includes(lang);
  }

  setLanguage(lang: Language): void {
    this._language.set(lang);
    localStorage.setItem('language', lang);
  }

  tReplace(template: string, values: Record<string, string | number>): string {
    return template.replace(/\{(\w+)\}/g, (_, key) => String(values[key] ?? `{${key}}`));
  }
}
