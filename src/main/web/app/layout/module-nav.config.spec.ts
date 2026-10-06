import { describe, expect, it, vi } from 'vitest';
import { FEATURE_FLAG_KEYS } from '../feature-flags/feature-flag-keys';
import {
  isNavTabEnabled,
  MODULE_NAV_TABS,
  listMoreNavSections,
  listPrimaryNavTabs,
} from './module-nav.config';

describe('module-nav.config', () => {
  const visionTab = MODULE_NAV_TABS.find(tab => tab.key === 'vision');
  const mcpTab = MODULE_NAV_TABS.find(tab => tab.key === 'mcp');
  const chatTab = MODULE_NAV_TABS.find(tab => tab.key === 'chat');
  const pipelinesTab = MODULE_NAV_TABS.find(tab => tab.key === 'pipelines');

  it('should enable core tabs when no flag configured', () => {
    const featureFlags = { isEnabled: vi.fn() };

    expect(isNavTabEnabled(chatTab!, featureFlags)).toBe(true);
    expect(featureFlags.isEnabled).not.toHaveBeenCalled();
  });

  it('should hide optional tabs when flags disabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(false),
    };

    expect(isNavTabEnabled(visionTab!, featureFlags)).toBe(false);
    expect(isNavTabEnabled(mcpTab!, featureFlags)).toBe(false);
    expect(featureFlags.isEnabled).toHaveBeenCalledWith(FEATURE_FLAG_KEYS.MODULE_VISION);
    expect(featureFlags.isEnabled).toHaveBeenCalledWith(FEATURE_FLAG_KEYS.MODULE_MCP);
  });

  it('should show optional tabs when flags enabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(true),
    };

    expect(isNavTabEnabled(visionTab!, featureFlags)).toBe(true);
    expect(isNavTabEnabled(mcpTab!, featureFlags)).toBe(true);
  });

  it('should keep work tabs except chat in primary nav', () => {
    expect(listPrimaryNavTabs(MODULE_NAV_TABS).map(tab => tab.key)).toEqual([
      'rag',
      'metrics',
      'pipelines',
      'automations',
      'agents',
      'skills',
    ]);
  });

  it('should put create and lab into more sections', () => {
    const sections = listMoreNavSections(MODULE_NAV_TABS);

    expect(sections.map(section => section.group)).toEqual(['create', 'lab']);
    expect(sections[0]?.tabs.map(tab => tab.key)).toEqual(['generate']);
    expect(sections[1]?.tabs.map(tab => tab.key)).toEqual(['vision', 'speechToText', 'mcp', 'eval']);
  });

  it('should hide more sections when create and lab tabs are disabled', () => {
    const workOnly = MODULE_NAV_TABS.filter(tab => tab.group === 'work');

    expect(listMoreNavSections(workOnly)).toEqual([]);
    expect(listPrimaryNavTabs(workOnly).length).toBeGreaterThan(0);
  });

  it('should omit lab group from more when lab tabs disabled', () => {
    const withoutLab = MODULE_NAV_TABS.filter(tab => tab.group !== 'lab');
    const sections = listMoreNavSections(withoutLab);

    expect(sections.map(section => section.group)).toEqual(['create']);
    expect(pipelinesTab?.group).toBe('work');
  });
});
