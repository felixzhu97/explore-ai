import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { FEATURE_FLAG_KEYS } from './config/feature-flag-keys';
import { PRIVACY_CONSENT_STORAGE_KEY } from '../privacy/privacy-consent.storage';

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
    'module-pipelines': true,
    'module-automations': true,
    'module-skills': true,
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
    localStorage.clear();
    envState.launchDarklyClientSideId = '';
    envState.featureFlagFallback = {
      'module-vision': true,
      'module-audio-asr': true,
      'module-mcp': true,
      'module-eval': true,
      'module-pipelines': true,
      'module-automations': true,
      'module-skills': true,
    };
    mockClient.waitForInitialization.mockResolvedValue(undefined);
    mockClient.boolVariation.mockImplementation(
      (_key: string, fallback: boolean) => fallback,
    );
  });

  afterEach(() => {
    vi.useRealTimers();
    localStorage.clear();
  });

  function allowAnalytics(): void {
    localStorage.setItem(
      PRIVACY_CONSENT_STORAGE_KEY,
      JSON.stringify({ decided: true, analytics: true }),
    );
  }

  it('should use environment fallback when client side id missing', async () => {
    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    await service.initialize();

    expect(service.isEnabled(FEATURE_FLAG_KEYS.MODULE_VISION)).toBe(true);
    expect(mockClient.start).not.toHaveBeenCalled();
  });

  it('should skip launchdarkly when analytics consent missing', async () => {
    envState.launchDarklyClientSideId = 'client-id';
    const { createClient } = await import('@launchdarkly/js-client-sdk');
    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    await service.initialize();

    expect(createClient).not.toHaveBeenCalled();
    expect(service.isEnabled(FEATURE_FLAG_KEYS.MODULE_VISION)).toBe(true);
  });

  it('should sync flags after launchdarkly init', async () => {
    allowAnalytics();
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

  it('should fallback when launchdarkly init times out', async () => {
    allowAnalytics();
    vi.useFakeTimers();
    envState.launchDarklyClientSideId = 'client-id';
    envState.featureFlagFallback = {
      'module-vision': true,
      'module-audio-asr': false,
      'module-mcp': true,
      'module-eval': true,
      'module-pipelines': true,
      'module-automations': true,
      'module-skills': true,
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

  it('should return fallback for unknown flag key', async () => {
    const { FeatureFlagService } = await import('./feature-flag.service');
    TestBed.configureTestingModule({ providers: [FeatureFlagService] });
    const service = TestBed.inject(FeatureFlagService);

    expect(service.isEnabled('unknown-key' as typeof FEATURE_FLAG_KEYS.MODULE_VISION)).toBe(false);
  });
});
