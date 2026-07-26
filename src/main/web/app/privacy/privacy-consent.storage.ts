export interface PrivacyConsentState {
  decided: boolean;
  analytics: boolean;
  decidedAt?: string;
}

export const PRIVACY_CONSENT_STORAGE_KEY = 'explore-ai-privacy-consent';

export function readPrivacyConsent(): PrivacyConsentState {
  if (typeof localStorage === 'undefined') {
    return { decided: false, analytics: false };
  }
  try {
    const raw = localStorage.getItem(PRIVACY_CONSENT_STORAGE_KEY);
    if (!raw) {
      return { decided: false, analytics: false };
    }
    const parsed = JSON.parse(raw) as Partial<PrivacyConsentState>;
    return {
      decided: Boolean(parsed.decided),
      analytics: Boolean(parsed.analytics),
      decidedAt: typeof parsed.decidedAt === 'string' ? parsed.decidedAt : undefined,
    };
  } catch {
    return { decided: false, analytics: false };
  }
}

export function writePrivacyConsent(analytics: boolean): PrivacyConsentState {
  const next: PrivacyConsentState = {
    decided: true,
    analytics,
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
