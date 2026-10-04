export interface PrivacyConsentState {
  decided: boolean;
  analytics: boolean;
  contactEmail: string;
  decidedAt?: string;
}

export const PRIVACY_CONSENT_STORAGE_KEY = 'explore-ai-privacy-consent';

export function readPrivacyConsent(): PrivacyConsentState {
  if (typeof localStorage === 'undefined') {
    return { decided: false, analytics: false, contactEmail: '' };
  }
  try {
    const raw = localStorage.getItem(PRIVACY_CONSENT_STORAGE_KEY);
    if (!raw) {
      return { decided: false, analytics: false, contactEmail: '' };
    }
    const parsed = JSON.parse(raw) as Partial<PrivacyConsentState>;
    return {
      decided: Boolean(parsed.decided),
      analytics: Boolean(parsed.analytics),
      contactEmail: typeof parsed.contactEmail === 'string' ? parsed.contactEmail : '',
      decidedAt: typeof parsed.decidedAt === 'string' ? parsed.decidedAt : undefined,
    };
  } catch {
    return { decided: false, analytics: false, contactEmail: '' };
  }
}

export function writePrivacyConsent(analytics: boolean): PrivacyConsentState {
  const current = readPrivacyConsent();
  return writePrivacyPreferences({ analytics, contactEmail: current.contactEmail });
}

export function writePrivacyPreferences(preferences: {
  analytics: boolean;
  contactEmail: string;
}): PrivacyConsentState {
  const next: PrivacyConsentState = {
    decided: true,
    analytics: preferences.analytics,
    contactEmail: preferences.contactEmail,
    decidedAt: new Date().toISOString(),
  };
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(PRIVACY_CONSENT_STORAGE_KEY, JSON.stringify(next));
  }
  return next;
}

export function hasAnalyticsConsent(): boolean {
  const state = readPrivacyConsent();
  return state.decided && state.analytics;
}

export function needsPrivacyConsentDecision(): boolean {
  return !readPrivacyConsent().decided;
}
