import { inject } from '@angular/core';
import { CanMatchFn, Route, UrlSegment } from '@angular/router';
import type { FeatureFlagKey } from '@core/config/feature-flag-keys';
import { FeatureFlagService } from '@core/services/feature-flag.service';

export const moduleEnabledGuard = (flagKey: FeatureFlagKey): CanMatchFn => {
  return (
    _route: Route,
    _segments: UrlSegment[],
    _snapshot?,
  ) => {
    void _route;
    void _segments;
    void _snapshot;
    return inject(FeatureFlagService).isEnabled(flagKey);
  };
};
