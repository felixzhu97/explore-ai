import type { Translations } from '../i18n/translations.types';
import { FEATURE_FLAG_KEYS, type FeatureFlagKey } from './feature-flag-keys';
import type { FeatureFlagService } from '../feature-flag.service';

export interface ModuleNavTab {
  key: string;
  labelKey: keyof Translations['nav'];
  path: string;
  flagKey?: FeatureFlagKey;
}

export const MODULE_NAV_TABS: ModuleNavTab[] = [
  { key: 'rag', labelKey: 'documentQA', path: '/rag' },
  { key: 'vision', labelKey: 'imageAnalysis', path: '/vision', flagKey: FEATURE_FLAG_KEYS.MODULE_VISION },
  { key: 'mcp', labelKey: 'mcp', path: '/mcp', flagKey: FEATURE_FLAG_KEYS.MODULE_MCP },
  { key: 'eval', labelKey: 'eval', path: '/eval', flagKey: FEATURE_FLAG_KEYS.MODULE_EVAL },
  { key: 'asr', labelKey: 'speechToText', path: '/asr', flagKey: FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR },
  { key: 'agents', labelKey: 'agents', path: '/agents', flagKey: FEATURE_FLAG_KEYS.MODULE_AGENTS },
  { key: 'chat', labelKey: 'chat', path: '/chat' },
  { key: 'generate', labelKey: 'generation', path: '/generate' },
];

export function isNavTabEnabled(
  tab: ModuleNavTab,
  featureFlags: Pick<FeatureFlagService, 'isEnabled'>,
): boolean {
  if (!tab.flagKey) {
    return true;
  }
  return featureFlags.isEnabled(tab.flagKey);
}
