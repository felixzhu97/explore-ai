import { inject } from '@angular/core';
import { type CanActivateFn, Router } from '@angular/router';
import type { FeatureFlagKey } from './feature-flag-keys';
import { FeatureFlagService } from './feature-flag.service';

/** Blocks the route when the module flag is off. */
export const moduleEnabledGuard = (flagKey: FeatureFlagKey): CanActivateFn => {
  return () => {
    if (inject(FeatureFlagService).isEnabled(flagKey)) {
      return true;
    }
    return inject(Router).createUrlTree(['/chat']);
  };
};
