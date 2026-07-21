import { describe, expect, it, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { FEATURE_FLAG_KEYS } from '../config/feature-flag-keys';
import { FeatureFlagService } from '../feature-flag.service';
import { moduleEnabledGuard } from './module-enabled.guard';

describe('moduleEnabledGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  it('should_allowRoute_when_flagEnabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(true),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: FeatureFlagService, useValue: featureFlags }],
    });

    const guard = moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_VISION);
    const canActivate = TestBed.runInInjectionContext(() => guard(route, state));

    expect(canActivate).toBe(true);
  });

  it('should_redirectToChat_when_flagDisabled', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockReturnValue(false),
    };
    const router = {
      createUrlTree: vi.fn().mockReturnValue({} as UrlTree),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: FeatureFlagService, useValue: featureFlags },
        { provide: Router, useValue: router },
      ],
    });

    const guard = moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_MCP);
    const canActivate = TestBed.runInInjectionContext(() => guard(route, state));

    expect(router.createUrlTree).toHaveBeenCalledWith(['/chat']);
    expect(canActivate).toEqual({});
  });

  it('should_supportEvalFlag', () => {
    const featureFlags = {
      isEnabled: vi.fn().mockImplementation(
        (key: string) => key === FEATURE_FLAG_KEYS.MODULE_EVAL,
      ),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: FeatureFlagService, useValue: featureFlags }],
    });

    const guard = moduleEnabledGuard(FEATURE_FLAG_KEYS.MODULE_EVAL);
    const canActivate = TestBed.runInInjectionContext(() => guard(route, state));

    expect(canActivate).toBe(true);
  });
});
