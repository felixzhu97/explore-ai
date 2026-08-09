import type { Language } from '../core/i18n/translations.types';
import { POLICY_DOCS } from './policies.docs.copy';

export type PolicySlug =
  | 'terms-of-use'
  | 'privacy-policy'
  | 'cookie-policy'
  | 'subprocessors';

export type LegacyLegalDocId = 'terms' | 'privacy' | 'cookies' | 'subprocessors';

export interface PolicySection {
  heading: string;
  paragraphs: string[];
}

export interface PolicyDocCopy {
  slug: PolicySlug;
  title: string;
  summary: string;
  subtitle: string;
  updated: string;
  sections: PolicySection[];
}

export interface PoliciesHubCopy {
  title: string;
  subtitle: string;
  indexHint: string;
  allPolicies: string;
  controlsLink: string;
  controlsSummary: string;
  backToChat: string;
  backToHub: string;
  disclaimer: string;
  links: { slug: PolicySlug; label: string; summary: string }[];
}

/** Map former `/legal/:doc` ids to `/policies/:slug`. */
export const LEGACY_LEGAL_TO_SLUG: Record<LegacyLegalDocId, PolicySlug> = {
  terms: 'terms-of-use',
  privacy: 'privacy-policy',
  cookies: 'cookie-policy',
  subprocessors: 'subprocessors',
};

export const POLICY_SLUGS: readonly PolicySlug[] = [
  'terms-of-use',
  'privacy-policy',
  'cookie-policy',
  'subprocessors',
] as const;

export function isPolicySlug(value: string | null | undefined): value is PolicySlug {
  return !!value && (POLICY_SLUGS as readonly string[]).includes(value);
}

export function resolvePolicySlug(raw: string | null | undefined): PolicySlug | null {
  if (!raw) {
    return null;
  }
  if (isPolicySlug(raw)) {
    return raw;
  }
  if (raw in LEGACY_LEGAL_TO_SLUG) {
    return LEGACY_LEGAL_TO_SLUG[raw as LegacyLegalDocId];
  }
  return null;
}

const HUB: Record<Language, PoliciesHubCopy> = {
  en: {
    title: 'Policies',
    subtitle: 'Terms, privacy, cookies, and sub-processors for ExploreAI',
    indexHint: 'Choose a document to review how ExploreAI works and how data is handled.',
    allPolicies: 'All policies',
    controlsLink: 'Privacy controls',
    controlsSummary: 'Manage analytics consent and erase this browser’s ExploreAI data.',
    backToChat: 'Back to chat',
    backToHub: 'All policies',
    disclaimer:
      'Draft for product commercialization. Not a substitute for legal advice. Contact us via Privacy controls.',
    links: [
      {
        slug: 'terms-of-use',
        label: 'Terms of Use',
        summary: 'Rules for using ExploreAI, accounts, content, and disclaimers.',
      },
      {
        slug: 'privacy-policy',
        label: 'Privacy Policy',
        summary: 'What data we process, why we process it, and your choices.',
      },
      {
        slug: 'cookie-policy',
        label: 'Cookie Policy',
        summary: 'Necessary identity cookies and optional analytics technologies.',
      },
      {
        slug: 'subprocessors',
        label: 'Sub-processors',
        summary: 'Third parties that may process data to deliver the service.',
      },
    ],
  },
  zh: {
    title: '政策',
    subtitle: 'ExploreAI 的服务条款、隐私、Cookie 与子处理方说明',
    indexHint: '选择一份文档，了解 ExploreAI 如何运作以及如何处理数据。',
    allPolicies: '全部政策',
    controlsLink: '隐私控制',
    controlsSummary: '管理分析同意，并擦除本浏览器中的 ExploreAI 数据。',
    backToChat: '返回对话',
    backToHub: '全部政策',
    disclaimer: '商业化阶段草稿，不构成法律意见。请通过隐私控制页联系我们。',
    links: [
      { slug: 'terms-of-use', label: '使用条款', summary: '使用规则、账号、内容与免责说明。' },
      { slug: 'privacy-policy', label: '隐私政策', summary: '我们处理哪些数据、为何处理，以及你的选择。' },
      { slug: 'cookie-policy', label: 'Cookie 政策', summary: '必要身份 Cookie 与可选分析技术。' },
      { slug: 'subprocessors', label: '子处理方', summary: '为提供服务可能处理数据的第三方。' },
    ],
  },
  ja: {
    title: 'ポリシー',
    subtitle: 'ExploreAI の利用規約、プライバシー、Cookie、委託先',
    indexHint: 'ExploreAI の仕組みとデータの扱いを確認する文書を選んでください。',
    allPolicies: 'すべてのポリシー',
    controlsLink: 'プライバシー設定',
    controlsSummary: '分析の同意を管理し、このブラウザの ExploreAI データを消去します。',
    backToChat: 'チャットへ戻る',
    backToHub: 'すべてのポリシー',
    disclaimer: '商用化向けの草案であり、法的助言ではありません。プライバシー設定からご連絡ください。',
    links: [
      { slug: 'terms-of-use', label: '利用規約', summary: '利用ルール、アカウント、コンテンツ、免責。' },
      { slug: 'privacy-policy', label: 'プライバシーポリシー', summary: '処理するデータ、目的、選択肢。' },
      { slug: 'cookie-policy', label: 'Cookie ポリシー', summary: '必要な識別 Cookie と任意の分析技術。' },
      { slug: 'subprocessors', label: '委託先', summary: 'サービス提供のためにデータを処理し得る第三者。' },
    ],
  },
  fr: {
    title: 'Politiques',
    subtitle: 'Conditions, confidentialité, cookies et sous-traitants ExploreAI',
    indexHint: 'Choisissez un document pour comprendre le service et le traitement des données.',
    allPolicies: 'Toutes les politiques',
    controlsLink: 'Contrôles de confidentialité',
    controlsSummary: 'Gérer le consentement analytique et effacer les données ExploreAI de ce navigateur.',
    backToChat: 'Retour au chat',
    backToHub: 'Toutes les politiques',
    disclaimer:
      'Brouillon pour la commercialisation. Ne remplace pas un avis juridique. Contact via les contrôles de confidentialité.',
    links: [
      {
        slug: 'terms-of-use',
        label: 'Conditions d\'utilisation',
        summary: 'Règles d\'usage, comptes, contenus et exclusions de garantie.',
      },
      {
        slug: 'privacy-policy',
        label: 'Politique de confidentialité',
        summary: 'Données traitées, finalités et vos choix.',
      },
      {
        slug: 'cookie-policy',
        label: 'Politique de cookies',
        summary: 'Cookies d\'identité nécessaires et analytics optionnels.',
      },
      {
        slug: 'subprocessors',
        label: 'Sous-traitants',
        summary: 'Tiers susceptibles de traiter des données pour le service.',
      },
    ],
  },
  es: {
    title: 'Políticas',
    subtitle: 'Términos, privacidad, cookies y subencargados de ExploreAI',
    indexHint: 'Elige un documento para revisar el servicio y el tratamiento de datos.',
    allPolicies: 'Todas las políticas',
    controlsLink: 'Controles de privacidad',
    controlsSummary: 'Gestiona el consentimiento de analítica y borra datos de ExploreAI en este navegador.',
    backToChat: 'Volver al chat',
    backToHub: 'Todas las políticas',
    disclaimer:
      'Borrador para comercialización. No sustituye asesoría legal. Contacto vía controles de privacidad.',
    links: [
      {
        slug: 'terms-of-use',
        label: 'Términos de uso',
        summary: 'Reglas de uso, cuentas, contenido y exenciones.',
      },
      {
        slug: 'privacy-policy',
        label: 'Política de privacidad',
        summary: 'Qué datos tratamos, para qué y tus opciones.',
      },
      {
        slug: 'cookie-policy',
        label: 'Política de cookies',
        summary: 'Cookies de identidad necesarias y analítica opcional.',
      },
      {
        slug: 'subprocessors',
        label: 'Subencargados',
        summary: 'Terceros que pueden tratar datos para prestar el servicio.',
      },
    ],
  },
};

export function policiesHubCopy(lang: Language): PoliciesHubCopy {
  return HUB[lang] ?? HUB.en;
}

export function policyDocCopy(slug: PolicySlug, lang: Language): PolicyDocCopy {
  return (POLICY_DOCS[lang] ?? POLICY_DOCS.en)[slug];
}
