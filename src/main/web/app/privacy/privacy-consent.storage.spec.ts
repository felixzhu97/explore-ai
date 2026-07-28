import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import {
  PRIVACY_CONSENT_STORAGE_KEY,
  hasAnalyticsConsent,
  needsPrivacyConsentDecision,
  readPrivacyConsent,
  writePrivacyConsent,
  writePrivacyPreferences,
} from './privacy-consent.storage';

describe('privacy-consent.storage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should_needDecision_whenNoStoredConsent', () => {
    expect(needsPrivacyConsentDecision()).toBe(true);
    expect(hasAnalyticsConsent()).toBe(false);
  });

  it('should_persistAnalyticsChoice_whenUserAccepts', () => {
    writePrivacyConsent(true);
    expect(readPrivacyConsent()).toMatchObject({ decided: true, analytics: true });
    expect(hasAnalyticsConsent()).toBe(true);
    expect(localStorage.getItem(PRIVACY_CONSENT_STORAGE_KEY)).toContain('"analytics":true');
  });

  it('should_rejectAnalytics_whenUserChoosesNecessaryOnly', () => {
    writePrivacyConsent(false);
    expect(hasAnalyticsConsent()).toBe(false);
    expect(needsPrivacyConsentDecision()).toBe(false);
  });

  it('should_persistContactEmail_whenPreferencesSaved', () => {
    writePrivacyPreferences({ analytics: true, contactEmail: 'privacy@example.com' });
    expect(readPrivacyConsent()).toMatchObject({
      analytics: true,
      contactEmail: 'privacy@example.com',
      decided: true,
    });
  });
});
