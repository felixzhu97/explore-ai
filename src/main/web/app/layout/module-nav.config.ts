import type { Translations } from '../i18n/translations';
import { FEATURE_FLAG_KEYS, type FeatureFlagKey } from '../feature-flags/feature-flag-keys';
import type { FeatureFlagService } from '../feature-flags/feature-flag.service';

export type ModuleNavGroup = 'work' | 'create' | 'lab';

export type ModuleNavLabelKey = Exclude<keyof Translations['nav'], 'groups' | 'more'>;

export interface ModuleNavTab {
  key: string;
  labelKey: ModuleNavLabelKey;
  path: string;
  group: ModuleNavGroup;
  flagKey?: FeatureFlagKey;
}

export interface ModuleNavSection {
  group: ModuleNavGroup;
  tabs: ModuleNavTab[];
}

/** Groups shown inside the More flyout (not top-level accordion). */
export const MORE_NAV_GROUP_ORDER: readonly ModuleNavGroup[] = ['create', 'lab'];

export const MODULE_NAV_TABS: ModuleNavTab[] = [
  { key: 'chat', labelKey: 'chat', path: '/chat', group: 'work' },
  { key: 'rag', labelKey: 'rag', path: '/rag', group: 'work' },
  { key: 'metrics', labelKey: 'metrics', path: '/metrics', group: 'work' },
  { key: 'pipelines', labelKey: 'pipelines', path: '/pipelines', group: 'work', flagKey: FEATURE_FLAG_KEYS.MODULE_PIPELINES },
  { key: 'automations', labelKey: 'automations', path: '/automations', group: 'work', flagKey: FEATURE_FLAG_KEYS.MODULE_AUTOMATIONS },
  { key: 'agents', labelKey: 'agents', path: '/agents', group: 'work', flagKey: FEATURE_FLAG_KEYS.MODULE_PIPELINES },
  { key: 'skills', labelKey: 'skills', path: '/skills', group: 'work', flagKey: FEATURE_FLAG_KEYS.MODULE_SKILLS },
  { key: 'generate', labelKey: 'generate', path: '/generate', group: 'create' },
  { key: 'vision', labelKey: 'vision', path: '/vision', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_VISION },
  { key: 'speechToText', labelKey: 'speechToText', path: '/speech-to-text', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR },
  { key: 'mcp', labelKey: 'mcp', path: '/mcp', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_MCP },
  { key: 'eval', labelKey: 'eval', path: '/eval', group: 'lab', flagKey: FEATURE_FLAG_KEYS.MODULE_EVAL },
];

export function isNavTabEnabled(
  tab: ModuleNavTab,
  featureFlags: Pick<FeatureFlagService, 'isEnabled'>,
): boolean {
  if (tab.flagKey === undefined) {
    return true;
  }
  return featureFlags.isEnabled(tab.flagKey);
}

/**
 * Top-level sidebar links (Work modules except Chat).
 * Chat is opened via New Chat, so it is omitted from the primary list.
 */
export function primaryNavTabs(tabs: readonly ModuleNavTab[]): ModuleNavTab[] {
  return tabs.filter(tab => tab.group === 'work' && tab.key !== 'chat');
}

/** Create + Lab sections for the More flyout; empty sections omitted. */
export function moreNavSections(tabs: readonly ModuleNavTab[]): ModuleNavSection[] {
  return MORE_NAV_GROUP_ORDER
    .map(group => ({
      group,
      tabs: tabs.filter(tab => tab.group === group),
    }))
    .filter(section => section.tabs.length > 0);
}
