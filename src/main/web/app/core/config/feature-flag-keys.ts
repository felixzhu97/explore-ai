export const FEATURE_FLAG_KEYS = {
  MODULE_VISION: 'module-vision',
  MODULE_AUDIO_ASR: 'module-audio-asr',
  MODULE_MCP: 'module-mcp',
  MODULE_EVAL: 'module-eval',
  MODULE_AGENTS: 'module-agents',
} as const;

export type FeatureFlagKey = typeof FEATURE_FLAG_KEYS[keyof typeof FEATURE_FLAG_KEYS];

export const MODULE_FLAG_FALLBACK: Record<FeatureFlagKey, boolean> = {
  [FEATURE_FLAG_KEYS.MODULE_VISION]: true,
  [FEATURE_FLAG_KEYS.MODULE_AUDIO_ASR]: true,
  [FEATURE_FLAG_KEYS.MODULE_MCP]: true,
  [FEATURE_FLAG_KEYS.MODULE_EVAL]: true,
  [FEATURE_FLAG_KEYS.MODULE_AGENTS]: true,
};
