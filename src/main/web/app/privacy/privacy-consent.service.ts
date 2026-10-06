import { Service, inject, signal } from '@angular/core';
import {
  hasAnalyticsConsent,
  needsPrivacyConsentDecision,
  readPrivacyConsent,
  writePrivacyPreferences,
  type PrivacyConsentState,
} from './privacy-consent.storage';
import { initDatadogRum } from './datadog-rum.config';
import { FeatureFlagService } from '../feature-flags/feature-flag.service';

@Service()
export class PrivacyConsentService {
  readonly #featureFlags = inject(FeatureFlagService);

  readonly consent = signal<PrivacyConsentState>(readPrivacyConsent());
  readonly needsDecision = signal(needsPrivacyConsentDecision());

  /** Allows analytics. */
  acceptAnalytics(): void {
    this.#applyChoice(true);
  }

  /** Declines analytics. */
  rejectAnalytics(): void {
    this.#applyChoice(false);
  }

  /** Saves the privacy choices and applies them. */
  savePreferences(preferences: { analytics: boolean; contactEmail: string }): void {
    const next = writePrivacyPreferences(preferences);
    this.consent.set(next);
    this.needsDecision.set(false);
    if (preferences.analytics) {
      initDatadogRum();
      void this.#featureFlags.initialize();
    }
  }

  #applyChoice(analytics: boolean): void {
    const current = this.consent();
    const next = writePrivacyPreferences({
      analytics,
      contactEmail: current.contactEmail,
    });
    this.consent.set(next);
    this.needsDecision.set(false);
    if (analytics) {
      initDatadogRum();
      void this.#featureFlags.initialize();
    }
  }
}

export { hasAnalyticsConsent };
