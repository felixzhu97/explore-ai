import { Injectable, inject, signal } from '@angular/core';
import {
  hasAnalyticsConsent,
  needsPrivacyConsentDecision,
  readPrivacyConsent,
  writePrivacyConsent,
  type PrivacyConsentState,
} from './privacy-consent.storage';
import { initDatadogRum } from '../core/config/datadog-rum.config';
import { FeatureFlagService } from '../core/feature-flag.service';

@Injectable({ providedIn: 'root' })
export class PrivacyConsentService {
  private readonly featureFlags = inject(FeatureFlagService);

  readonly consent = signal<PrivacyConsentState>(readPrivacyConsent());
  readonly needsDecision = signal(needsPrivacyConsentDecision());

  acceptAnalytics(): void {
    this.applyChoice(true);
  }

  rejectAnalytics(): void {
    this.applyChoice(false);
  }

  private applyChoice(analytics: boolean): void {
    const next = writePrivacyConsent(analytics);
    this.consent.set(next);
    this.needsDecision.set(false);
    if (analytics) {
      initDatadogRum();
      void this.featureFlags.initialize();
    }
  }
}

export { hasAnalyticsConsent };
