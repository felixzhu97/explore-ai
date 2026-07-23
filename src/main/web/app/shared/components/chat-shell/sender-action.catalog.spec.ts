import { describe, expect, it } from 'vitest';
import { buildSenderActionGroups, filterSenderGroups } from './sender-action.catalog';
import type { Translations } from '../../../core/i18n/translations.types';
import { en } from '../../../core/i18n/locales/en';

describe('sender-action.catalog', () => {
  const featureFlags = { isEnabled: () => true };

  it('should_includeToolsAndAgents_when_fullScope', () => {
    const groups = buildSenderActionGroups({
      t: en as Translations,
      tools: [{ name: 'getWeather', description: 'Weather', source: 'LOCAL' }],
      agents: [
        {
          type: 'researcher',
          name: 'Researcher',
          description: 'Research',
          healthy: true,
          supervisor: false,
        },
      ],
      featureFlags,
      scope: 'full',
    });

    expect(groups.map(group => group.id)).toContain('tools');
    expect(groups.map(group => group.id)).toContain('agents');
    expect(groups.find(group => group.id === 'tools')?.items[0].promptTemplate).toContain(
      'weather',
    );
  });

  it('should_filterItems_when_queryTyped', () => {
    const groups = buildSenderActionGroups({
      t: en as Translations,
      tools: [
        { name: 'getWeather', description: 'Weather', source: 'LOCAL' },
        { name: 'searchWeb', description: 'Search', source: 'LOCAL' },
      ],
      agents: [],
      featureFlags,
      scope: 'rag',
    });

    const filtered = filterSenderGroups(groups, 'weather');
    expect(filtered).toHaveLength(1);
    expect(filtered[0].items).toHaveLength(1);
    expect(filtered[0].items[0].label).toBe('getWeather');
  });
});
