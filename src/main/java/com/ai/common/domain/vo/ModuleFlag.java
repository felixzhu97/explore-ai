package com.ai.common.domain.vo;

public enum ModuleFlag {

    VISION("module-vision", "/api/vision"),
    AUDIO_ASR("module-audio-asr", "/ws/audio"),
    MCP("module-mcp", "/api/mcp"),
    EVAL("module-eval", "/api/eval");

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

    public String bootstrapProperty() {
        return "launchdarkly.bootstrap." + key;
    }

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
