export * from './translations.types';
import type { Language } from './translations.types';
import type { Translations } from './translations.types';
import { en } from './locales/en';
import { zh } from './locales/zh';
import { ja } from './locales/ja';
import { fr } from './locales/fr';
import { es } from './locales/es';

export const translations: Record<Language, Translations> = {
  en,
  zh,
  ja,
  fr,
  es,
};

export const languageNames: Record<Language, string> = {
  en: 'English',
  zh: '中文',
  ja: '日本語',
  fr: 'Français',
  es: 'Español',
};
