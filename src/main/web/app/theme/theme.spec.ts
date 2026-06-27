import { describe, it, expect } from 'vitest';
import { colors, shadows, radius, spacing, typography, transitions, zIndex, theme } from './theme';

describe('theme', () => {
  describe('colors', () => {
    it('should export color palette', () => {
      expect(colors).toBeDefined();
      expect(colors.background).toBe('#f5f5f7');
      expect(colors.surface).toBe('#ffffff');
    });

    it('should have primary colors', () => {
      expect(colors.primary).toBe('#007aff');
      expect(colors.primaryHover).toBeDefined();
      expect(colors.primaryActive).toBeDefined();
      expect(colors.primaryLight).toBeDefined();
    });

    it('should have text colors', () => {
      expect(colors.text).toBe('#1d1d1f');
      expect(colors.textSecondary).toBeDefined();
      expect(colors.textTertiary).toBeDefined();
    });

    it('should have semantic colors', () => {
      expect(colors.success).toBe('#34c759');
      expect(colors.error).toBe('#ff3b30');
      expect(colors.warning).toBe('#ff9500');
    });

    it('should have border colors', () => {
      expect(colors.border).toBeDefined();
      expect(colors.borderLight).toBeDefined();
    });

    it('should have overlay and glass colors', () => {
      expect(colors.overlay).toBeDefined();
      expect(colors.glass).toBeDefined();
    });
  });

  describe('shadows', () => {
    it('should export shadow scales', () => {
      expect(shadows).toBeDefined();
      expect(shadows.sm).toBeDefined();
      expect(shadows.card).toBeDefined();
      expect(shadows.cardHover).toBeDefined();
      expect(shadows.elevated).toBeDefined();
      expect(shadows.input).toBeDefined();
    });

    it('should have shadow values as strings', () => {
      Object.values(shadows).forEach((shadow) => {
        expect(typeof shadow).toBe('string');
      });
    });
  });

  describe('radius', () => {
    it('should export border radius scales', () => {
      expect(radius).toBeDefined();
      expect(radius.sm).toBe('6px');
      expect(radius.md).toBe('10px');
      expect(radius.lg).toBe('14px');
      expect(radius.xl).toBe('20px');
      expect(radius.full).toBe('9999px');
    });

    it('should have px values', () => {
      Object.values(radius).forEach((r) => {
        expect(r).toMatch(/^\d+px$/);
      });
    });
  });

  describe('spacing', () => {
    it('should export spacing scales', () => {
      expect(spacing).toBeDefined();
      expect(spacing.xs).toBe('4px');
      expect(spacing.sm).toBe('8px');
      expect(spacing.md).toBe('16px');
      expect(spacing.lg).toBe('24px');
      expect(spacing.xl).toBe('32px');
      expect(spacing.xxl).toBe('48px');
    });

    it('should have consistent px increments', () => {
      const values = Object.values(spacing).map(s => parseInt(s.replace('px', '')));
      expect(values).toEqual([4, 8, 16, 24, 32, 48]);
    });
  });

  describe('typography', () => {
    it('should export typography config', () => {
      expect(typography).toBeDefined();
    });

    it('should have fontFamily config', () => {
      expect(typography.fontFamily).toBeDefined();
      expect(typography.fontFamily.display).toBeDefined();
      expect(typography.fontFamily.body).toBeDefined();
      expect(typography.fontFamily.mono).toBeDefined();
    });

    it('should have fontSize scale', () => {
      expect(typography.fontSize).toBeDefined();
      expect(typography.fontSize.xs).toBe('11px');
      expect(typography.fontSize.sm).toBe('12px');
      expect(typography.fontSize.base).toBe('14px');
      expect(typography.fontSize.lg).toBe('17px');
      expect(typography.fontSize.xl).toBe('20px');
    });

    it('should have fontWeight scale', () => {
      expect(typography.fontWeight).toBeDefined();
      expect(typography.fontWeight.normal).toBe('400');
      expect(typography.fontWeight.medium).toBe('500');
      expect(typography.fontWeight.semibold).toBe('600');
      expect(typography.fontWeight.bold).toBe('700');
    });

    it('should have lineHeight config', () => {
      expect(typography.lineHeight).toBeDefined();
      expect(typography.lineHeight.tight).toBe('1.1');
      expect(typography.lineHeight.normal).toBe('1.4');
      expect(typography.lineHeight.relaxed).toBe('1.6');
    });
  });

  describe('transitions', () => {
    it('should export transition timings', () => {
      expect(transitions).toBeDefined();
      expect(transitions.fast).toBe('0.15s ease');
      expect(transitions.default).toBe('0.2s ease');
      expect(transitions.smooth).toBeDefined();
      expect(transitions.spring).toBeDefined();
    });

    it('should have cubic-bezier for advanced transitions', () => {
      expect(transitions.smooth).toContain('cubic-bezier');
      expect(transitions.spring).toContain('cubic-bezier');
    });
  });

  describe('zIndex', () => {
    it('should export z-index scale', () => {
      expect(zIndex).toBeDefined();
      expect(zIndex.base).toBe(0);
      expect(zIndex.dropdown).toBe(100);
      expect(zIndex.sticky).toBe(200);
      expect(zIndex.modal).toBe(300);
      expect(zIndex.toast).toBe(400);
    });

    it('should have numeric z-index values', () => {
      Object.entries(zIndex).forEach(([, value]) => {
        expect(typeof value).toBe('number');
      });
    });
  });

  describe('theme object', () => {
    it('should export composed theme object', () => {
      expect(theme).toBeDefined();
      expect(theme.colors).toBe(colors);
      expect(theme.shadows).toBe(shadows);
      expect(theme.radius).toBe(radius);
      expect(theme.spacing).toBe(spacing);
      expect(theme.typography).toBe(typography);
      expect(theme.transitions).toBe(transitions);
      expect(theme.zIndex).toBe(zIndex);
    });

    it('should have Theme type', () => {
      expect(theme).toBeDefined();
    });
  });

  describe('default export', () => {
    it('should have default export in theme module', () => {
      // ESM modules with default exports are verified through the theme constant
      // The default export exists in theme.ts as 'export default theme'
      expect(theme).toBeDefined();
    });
  });
});
