import { describe, expect, it, vi } from 'vitest';
import { FEATURE_FLAG_KEYS } from './feature-flag-keys';
import {
  groupNavTabs,
  isNavTabEnabled,
  MODULE_NAV_TABS,
} from './module-nav.config';

describe('module-nav.config', () => {
  const visionTab = MODULE_NAV_TABS.find(tab => tab.key === 'vision');
  const mcpTab = MODULE_NAV_TABS.find(tab => tab.key === 'mcp');
  const chatTab = MODULE_NAV_TABS.find(tab => tab.key === 'chat');
  const agentsTab = MODULE_NAV_TABS.find(tab => tab.key === 'agents');

  it('should_enableCoreTabs_when_noFlagConfigured', () => {
    const featureFlags = { isEnabled: vi.fn() };

    expect(isNavTabEnabled(chatTab!, featureFlags)).toBe(true);
    expect(featureFlags.isEnabled).not.toHaveBeenCalled();
  });

  it('should_hideOptionalTabs_when_flagsDisabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(false),
    };

    expect(isNavTabEnabled(visionTab!, featureFlags)).toBe(false);
    expect(isNavTabEnabled(mcpTab!, featureFlags)).toBe(false);
    expect(featureFlags.isEnabled).toHaveBeenCalledWith(FEATURE_FLAG_KEYS.MODULE_VISION);
    expect(featureFlags.isEnabled).toHaveBeenCalledWith(FEATURE_FLAG_KEYS.MODULE_MCP);
  });

  it('should_showOptionalTabs_when_flagsEnabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(true),
    };

    expect(isNavTabEnabled(visionTab!, featureFlags)).toBe(true);
    expect(isNavTabEnabled(mcpTab!, featureFlags)).toBe(true);
  });

  it('should_orderWorkCreateLab_when_allTabsEnabled', () => {
    const sections = groupNavTabs(MODULE_NAV_TABS);

    expect(sections.map(section => section.group)).toEqual(['work', 'create', 'lab']);
    expect(sections[0].tabs.map(tab => tab.key)).toEqual(['chat', 'rag', 'agents']);
    expect(sections[1].tabs.map(tab => tab.key)).toEqual(['generate']);
    expect(sections[2].tabs.map(tab => tab.key)).toEqual(['vision', 'asr', 'mcp', 'eval']);
  });

  it('should_omitLabGroup_when_labTabsDisabled', () => {
    const enabled = MODULE_NAV_TABS.filter((tab) => {
      if (tab.group !== 'lab') {
        return true;
      }
      return false;
    });

    const sections = groupNavTabs(enabled);

    expect(sections.map(section => section.group)).toEqual(['work', 'create']);
    expect(agentsTab?.group).toBe('work');
  });
});
