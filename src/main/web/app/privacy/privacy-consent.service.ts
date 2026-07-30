import { Injectable, inject, signal } from '@angular/core';
import {
  hasAnalyticsConsent,
  needsPrivacyConsentDecision,
  readPrivacyConsent,
  writePrivacyPreferences,
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

  savePreferences(preferences: { analytics: boolean; contactEmail: string }): void {
    const next = writePrivacyPreferences(preferences);
    this.consent.set(next);
    this.needsDecision.set(false);
    if (preferences.analytics) {
      initDatadogRum();
      void this.featureFlags.initialize();
    }
  }

  private applyChoice(analytics: boolean): void {
    const current = this.consent();
    const next = writePrivacyPreferences({
      analytics,
      contactEmail: current.contactEmail,
    });
    this.consent.set(next);
    this.needsDecision.set(false);
    if (analytics) {
      initDatadogRum();
      void this.featureFlags.initialize();
    }
  }
}

export { hasAnalyticsConsent };
