package com.ai.common.domain.model;

/** Feature-flagged application modules, each with a flag key and the API path prefix it gates. */
public enum ModuleFlag {
  VISION("module-vision", "/api/vision"),
  AUDIO_ASR("module-audio-asr", "/ws/audio"),
  MCP("module-mcp", "/api/mcp"),
  EVAL("module-eval", "/api/eval"),
  PIPELINES("module-pipelines", "/api/pipelines"),
  AUTOMATIONS("module-automations", "/api/automations"),
  SKILLS("module-skills", "/api/skills");

  private final String key;
  private final String pathPrefix;

  ModuleFlag(String key, String pathPrefix) {
    this.key = key;
    this.pathPrefix = pathPrefix;
  }

  public String key() {
    return key;
  }

  public String pathPrefix() {
    return pathPrefix;
  }

  /** Returns the property that holds the module's bootstrap value. */
  public String bootstrapProperty() {
    return "launchdarkly.bootstrap." + key;
  }

  /** Returns the module whose path prefix matches the request path, or null if none does. */
  public static ModuleFlag fromPath(String requestPath) {
    if (requestPath == null) {
      return null;
    }
    for (ModuleFlag flag : values()) {
      if (requestPath.startsWith(flag.pathPrefix)) {
        return flag;
      }
    }
    return null;
  }
}
