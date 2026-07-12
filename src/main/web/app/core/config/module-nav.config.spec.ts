import { describe, expect, it, vi } from 'vitest';
import { FEATURE_FLAG_KEYS } from '@core/config/feature-flag-keys';
import { isNavTabEnabled, MODULE_NAV_TABS } from '@core/config/module-nav.config';

describe('module-nav.config', () => {
  const visionTab = MODULE_NAV_TABS.find(tab => tab.key === 'vision');
  const mcpTab = MODULE_NAV_TABS.find(tab => tab.key === 'mcp');
  const chatTab = MODULE_NAV_TABS.find(tab => tab.key === 'chat');

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
});
