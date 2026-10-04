import { describe, expect, it } from 'vitest';
import { mergeHistoryWithUiState, type UiMessage } from './chat.service';

describe('mergeHistoryWithUiState', () => {
  it('should keep local sources when api sources missing', () => {
    const previous: UiMessage[] = [
      {
        id: 'assistant_1',
        role: 'assistant',
        content: 'Paris is the capital.',
        timestamp: 2,
        sources: [
          {
            title: 'Paris – Wikipedia',
            url: 'https://en.wikipedia.org/wiki/Paris',
            snippet: 'Capital of France',
          },
        ],
      },
    ];
    const history: UiMessage[] = [
      {
        id: 'uuid-assistant',
        role: 'assistant',
        content: 'Paris is the capital.',
        timestamp: 2,
      },
    ];

    const merged = mergeHistoryWithUiState(history, previous);

    expect(merged[0].id).toBe('uuid-assistant');
    expect(merged[0].sources).toEqual(previous[0].sources);
  });

  it('should prefer api sources when present', () => {
    const previous: UiMessage[] = [
      {
        id: 'assistant_1',
        role: 'assistant',
        content: 'answer',
        timestamp: 2,
        sources: [{ title: 'Old', url: 'https://old.example', snippet: '' }],
      },
    ];
    const history: UiMessage[] = [
      {
        id: 'uuid-assistant',
        role: 'assistant',
        content: 'answer',
        timestamp: 2,
        sources: [{ title: 'New', url: 'https://new.example', snippet: 'from api' }],
      },
    ];

    const merged = mergeHistoryWithUiState(history, previous);

    expect(merged[0].sources?.[0].url).toBe('https://new.example');
  });
});
