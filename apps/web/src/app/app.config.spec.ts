import { describe, it, expect } from 'vitest';
import { appConfig } from './app.config';

describe('appConfig', () => {
  describe('exports', () => {
    it('should export appConfig object', () => {
      expect(appConfig).toBeDefined();
      expect(typeof appConfig).toBe('object');
    });

    it('should have providers array', () => {
      expect(appConfig.providers).toBeDefined();
      expect(Array.isArray(appConfig.providers)).toBe(true);
    });

    it('should have at least 3 providers', () => {
      expect(appConfig.providers.length).toBeGreaterThanOrEqual(3);
    });
  });

  describe('configuration values', () => {
    it('should have providers defined', () => {
      expect(appConfig.providers.length).toBe(3);
    });
  });
});
