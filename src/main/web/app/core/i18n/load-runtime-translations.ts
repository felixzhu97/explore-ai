import { loadTranslations } from '@angular/localize';

const SUPPORTED = ['en', 'zh', 'ja', 'fr', 'es'] as const;
export type RuntimeLanguage = (typeof SUPPORTED)[number];

type LocaleModule = { default: Record<string, string> } | Record<string, string>;

export function readStoredLanguage(): RuntimeLanguage {
  const stored = localStorage.getItem('language');
  if (stored && (SUPPORTED as readonly string[]).includes(stored)) {
    return stored as RuntimeLanguage;
  }

  const browserLang = navigator.language.split('-')[0];
  if ((SUPPORTED as readonly string[]).includes(browserLang)) {
    return browserLang as RuntimeLanguage;
  }

  return 'zh';
}

/**
 * Load `@angular/localize` runtime translations before bootstrap.
 * English uses source strings in templates / `$localize` (no map).
 */
export async function loadRuntimeTranslations(
  lang: RuntimeLanguage = readStoredLanguage(),
): Promise<RuntimeLanguage> {
  $localize.locale = lang;

  if (lang === 'en') {
    return lang;
  }

  const messages = await importLocaleMessages(lang);
  loadTranslations(messages);
  return lang;
}

async function importLocaleMessages(
  lang: Exclude<RuntimeLanguage, 'en'>,
): Promise<Record<string, string>> {
  switch (lang) {
    case 'zh':
      return unwrapJsonModule(await import('../../../locale/messages.zh.json'));
    case 'ja':
      return unwrapJsonModule(await import('../../../locale/messages.ja.json'));
    case 'fr':
      return unwrapJsonModule(await import('../../../locale/messages.fr.json'));
    case 'es':
      return unwrapJsonModule(await import('../../../locale/messages.es.json'));
  }
}

function unwrapJsonModule(mod: LocaleModule): Record<string, string> {
  if ('default' in mod && typeof mod.default === 'object' && mod.default !== null) {
    return mod.default;
  }
  return mod as Record<string, string>;
}
