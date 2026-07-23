import type { Translations } from '../i18n/translations.types';
import { FEATURE_FLAG_KEYS, type FeatureFlagKey } from './feature-flag-keys';
import type { FeatureFlagService } from '../feature-flag.service';

export type ModuleNavGroup = 'work' | 'create' | 'lab';

export type ModuleNavLabelKey = Exclude<keyof Translations['nav'], 'groups'>;

export interface ModuleNavTab {
  key: string;
  labelKey: ModuleNavLabelKey;
  path: string;
  group: ModuleNavGroup;
  flagKey?: FeatureFlagKey;
}

export const MODULE_NAV_GROUP_ORDER: readonly ModuleNavGroup[] = ['work', 'create', 'lab'];

export const MODULE_NAV_TABS: ModuleNavTab[] = [
  { key: 'chat', labelKey: 'chat', path: '/chat', group: 'work' },
  { key: 'rag', labelKey: 'documentQA', path: '/rag', group: 'work' },
  { key: 'agents', labelKey: 'agents', path: '/agents', group: 'work', flagKey: FEATURE_FLAG_KEYS.MODULE_AGENTS },
  { key: 'generate', labelKey: 'generation', path: '/generate', group: 'create' },
  { key: 'vision', labelKey: 'imageAnalysis', path: '/vision', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_VISION },
  { key: 'asr', labelKey: 'speechToText', path: '/asr', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR },
  { key: 'mcp', labelKey: 'mcp', path: '/mcp', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_MCP },
  { key: 'eval', labelKey: 'eval', path: '/eval', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_EVAL },
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

export function groupNavTabs(
  tabs: readonly ModuleNavTab[],
): { group: ModuleNavGroup; tabs: ModuleNavTab[] }[] {
  return MODULE_NAV_GROUP_ORDER
    .map(group => ({
      group,
      tabs: tabs.filter(tab => tab.group === group),
    }))
    .filter(section => section.tabs.length > 0);
}
