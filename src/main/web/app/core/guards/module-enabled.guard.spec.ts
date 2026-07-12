import { describe, expect, it, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Route, UrlSegment, type CanMatchFn } from '@angular/router';
import { FEATURE_FLAG_KEYS } from '@core/config/feature-flag-keys';
import { FeatureFlagService } from '@core/services/feature-flag.service';
import { moduleEnabledGuard } from '@core/guards/module-enabled.guard';

describe('moduleEnabledGuard', () => {
  const route: Route = { path: 'vision' };
  const segments = [new UrlSegment('vision', {})];
  const snapshot = {} as Parameters<CanMatchFn>[2];

  it('should_allowRoute_when_flagEnabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(true),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: FeatureFlagService, useValue: featureFlags }],
    });

    const guard = moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_VISION);
    const canMatch = TestBed.runInInjectionContext(
      () => guard(route, segments, snapshot),
    );

    expect(canMatch).toBe(true);
  });

  it('should_blockRoute_when_flagDisabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(false),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: FeatureFlagService, useValue: featureFlags }],
    });

    const guard = moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_VISION);
    const canMatch = TestBed.runInInjectionContext(
      () => guard(route, segments, snapshot),
    );

    expect(canMatch).toBe(false);
  });
});
