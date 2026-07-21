import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import type { FeatureFlagKey } from '../config/feature-flag-keys';
import { FeatureFlagService } from '../feature-flag.service';

export const moduleEnabledGuard = (flagKey: FeatureFlagKey): CanActivateFn => {
  return () => {
    if (inject(FeatureFlagService).isEnabled(flagKey)) {
      return true;
    }
    return inject(Router).createUrlTree(['/chat']);
  };
};
