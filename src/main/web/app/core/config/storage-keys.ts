export const STORAGE_KEYS = {
  LANGUAGE: 'explore-ai.i18n.language',
  PRIVACY_CONSENT: 'explore-ai.privacy.consent',
  OAUTH_RETURN_URL: 'explore-ai.account.oauthReturnUrl',
  CHAT_ACTIVE_SESSION_ID: 'explore-ai.chat.activeSessionId',
  CHAT_PINNED_SESSION_IDS: 'explore-ai.chat.pinnedSessionIds',
} as const;

interface LegacyStorageKey {
  storage: () => Storage;
  legacyKey: string;
  key: string;
}

const LEGACY_STORAGE_KEYS: readonly LegacyStorageKey[] = [
  { storage: () => localStorage, legacyKey: 'language', key: STORAGE_KEYS.LANGUAGE },
  {
    storage: () => localStorage,
    legacyKey: 'explore-ai-privacy-consent',
    key: STORAGE_KEYS.PRIVACY_CONSENT,
  },
  {
    storage: () => sessionStorage,
    legacyKey: 'ea_oauth_return',
    key: STORAGE_KEYS.OAUTH_RETURN_URL,
  },
];

/** Copies values stored under legacy keys to their current keys; existing values win. */
export function migrateLegacyStorageKeys(): void {
  for (const { storage, legacyKey, key } of LEGACY_STORAGE_KEYS) {
    try {
      const store = storage();
      const legacyValue = store.getItem(legacyKey);
      if (legacyValue === null) {
        continue;
      }
      if (store.getItem(key) === null) {
        store.setItem(key, legacyValue);
      }
      store.removeItem(legacyKey);
    } catch {
      // Storage access throws when the browser blocks it.
    }
  }
}
