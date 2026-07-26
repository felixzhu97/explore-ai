import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { FEATURE_FLAG_KEYS } from './config/feature-flag-keys';

const mockClient = {
  on: vi.fn(),
  start: vi.fn(),
  waitForInitialization: vi.fn(),
  boolVariation: vi.fn((_key: string, fallback: boolean) => fallback),
};

const envState = vi.hoisted(() => ({
  launchDarklyClientSideId: '',
  featureFlagFallback: {
    'module-vision': true,
    'module-audio-asr': true,
    'module-mcp': true,
    'module-eval': true,
    'module-agents': true,
  },
}));

vi.mock('@launchdarkly/js-client-sdk', () => ({
  createClient: vi.fn(() => mockClient),
}));

vi.mock('../../environments/environment', () => ({
  environment: envState,
}));

describe('FeatureFlagService', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    vi.resetModules();
    envState.launchDarklyClientSideId = '';
    envState.featureFlagFallback = {
      'module-vision': true,
      'module-audio-asr': true,
      'module-mcp': true,
      'module-eval': true,
      'module-agents': true,
    };
    mockClient.waitForInitialization.mockResolvedValue(undefined);
    mockClient.boolVariation.mockImplementation(
      (_key: string, fallback: boolean) => fallback,
    );
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should_use_environment_fallback_when_client_side_id_missing', async () => {
    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    await service.initialize();

    expect(service.isEnabled(FEATURE_FLAG_KEYS.MODULE_VISION)).toBe(true);
    expect(mockClient.start).not.toHaveBeenCalled();
  });

  it('should_sync_flags_after_launchdarkly_init', async () => {
    envState.launchDarklyClientSideId = 'client-id';
    mockClient.boolVariation.mockImplementation(
      (key: string) => key === FEATURE_FLAG_KEYS.MODULE_VISION,
    );

    const { createClient } = await import('@launchdarkly/js-client-sdk');
    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    await service.initialize();

    expect(createClient).toHaveBeenCalled();
    expect(service.isEnabled(FEATURE_FLAG_KEYS.MODULE_VISION)).toBe(true);
  });

  it('should_fallback_when_launchdarkly_init_times_out', async () => {
    vi.useFakeTimers();
    envState.launchDarklyClientSideId = 'client-id';
    envState.featureFlagFallback = {
      'module-vision': true,
      'module-audio-asr': false,
      'module-mcp': true,
      'module-eval': true,
      'module-agents': true,
    };
    mockClient.waitForInitialization.mockReturnValue(
      new Promise(() => undefined),
    );

    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    const initPromise = service.initialize();
    await vi.advanceTimersByTimeAsync(5000);
    await initPromise;

    expect(service.isEnabled(FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR)).toBe(false);
  });

  it('should_return_fallback_for_unknown_flag_key', async () => {
    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    expect(service.isEnabled('unknown-key' as typeof FEATURE_FLAG_KEYS.MODULE_VISION)).toBe(false);
  });
});
