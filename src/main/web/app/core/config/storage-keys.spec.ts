import { afterEach, describe, expect, it } from 'vitest';
import { STORAGE_KEYS, migrateLegacyStorageKeys } from './storage-keys';

describe('migrateLegacyStorageKeys', () => {
  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should copy legacy values to current keys and remove the legacy keys', () => {
    const consent = JSON.stringify({ decided: true, analytics: true, contactEmail: '' });
    localStorage.setItem('language', 'fr');
    localStorage.setItem('explore-ai-privacy-consent', consent);
    sessionStorage.setItem('ea_oauth_return', '/rag');

    migrateLegacyStorageKeys();

    expect(localStorage.getItem(STORAGE_KEYS.LANGUAGE)).toBe('fr');
    expect(localStorage.getItem(STORAGE_KEYS.PRIVACY_CONSENT)).toBe(consent);
    expect(sessionStorage.getItem(STORAGE_KEYS.OAUTH_RETURN_URL)).toBe('/rag');
    expect(localStorage.getItem('language')).toBeNull();
    expect(localStorage.getItem('explore-ai-privacy-consent')).toBeNull();
    expect(sessionStorage.getItem('ea_oauth_return')).toBeNull();
  });

  it('should keep the current value when both legacy and current keys exist', () => {
    localStorage.setItem('language', 'fr');
    localStorage.setItem(STORAGE_KEYS.LANGUAGE, 'ja');

    migrateLegacyStorageKeys();

    expect(localStorage.getItem(STORAGE_KEYS.LANGUAGE)).toBe('ja');
    expect(localStorage.getItem('language')).toBeNull();
  });

  it('should leave storage untouched when no legacy keys exist', () => {
    localStorage.setItem(STORAGE_KEYS.LANGUAGE, 'en');

    migrateLegacyStorageKeys();

    expect(localStorage.getItem(STORAGE_KEYS.LANGUAGE)).toBe('en');
    expect(localStorage.length).toBe(1);
  });
});
