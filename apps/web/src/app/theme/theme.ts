// Theme configuration
export const colors = {
  background: '#f5f5f7',
  surface: '#ffffff',
  surfaceSecondary: 'rgba(255, 255, 255, 0.72)',
  surfaceTertiary: 'rgba(255, 255, 255, 0.54)',

  primary: '#007aff',
  primaryHover: '#0071e3',
  primaryActive: '#0056b3',
  primaryLight: 'rgba(0, 122, 255, 0.12)',

  text: '#1d1d1f',
  textSecondary: '#86868b',
  textTertiary: '#6e6e73',

  border: 'rgba(0, 0, 0, 0.08)',
  borderLight: 'rgba(0, 0, 0, 0.04)',

  success: '#34c759',
  successLight: 'rgba(52, 199, 89, 0.12)',
  error: '#ff3b30',
  errorLight: 'rgba(255, 59, 48, 0.12)',
  warning: '#ff9500',
  warningLight: 'rgba(255, 149, 0, 0.12)',

  overlay: 'rgba(0, 0, 0, 0.4)',
  glass: 'rgba(255, 255, 255, 0.8)',
};

export const shadows = {
  sm: '0 1px 2px rgba(0, 0, 0, 0.04)',
  card: '0 2px 8px rgba(0, 0, 0, 0.08)',
  cardHover: '0 8px 24px rgba(0, 0, 0, 0.12)',
  elevated: '0 12px 40px rgba(0, 0, 0, 0.16)',
  input: '0 0 0 3px rgba(0, 122, 255, 0.2)',
};

export const radius = {
  sm: '6px',
  md: '10px',
  lg: '14px',
  xl: '20px',
  full: '9999px',
};

export const spacing = {
  xs: '4px',
  sm: '8px',
  md: '16px',
  lg: '24px',
  xl: '32px',
  xxl: '48px',
};

export const typography = {
  fontFamily: {
    display: '"SF Pro Display", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    body: '"SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    mono: '"SF Mono", Monaco, "Cascadia Code", "Roboto Mono", Consolas, monospace',
  },
  fontSize: {
    xs: '11px',
    sm: '12px',
    base: '14px',
    md: '15px',
    lg: '17px',
    xl: '20px',
    '2xl': '24px',
    '3xl': '28px',
    '4xl': '34px',
  },
  fontWeight: {
    normal: '400',
    medium: '500',
    semibold: '600',
    bold: '700',
  },
  lineHeight: {
    tight: '1.1',
    normal: '1.4',
    relaxed: '1.6',
  },
};

export const transitions = {
  fast: '0.15s ease',
  default: '0.2s ease',
  smooth: '0.25s cubic-bezier(0.25, 0.1, 0.25, 1)',
  spring: '0.35s cubic-bezier(0.32, 0.72, 0, 1)',
};

export const zIndex = {
  base: 0,
  dropdown: 100,
  sticky: 200,
  modal: 300,
  toast: 400,
};

export const theme = {
  colors,
  shadows,
  radius,
  spacing,
  typography,
  transitions,
  zIndex,
};

export type Theme = typeof theme;
export default theme;
