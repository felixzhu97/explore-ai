package com.ai.common.application.llm;

public record TextChatOptions(
        String provider,
        String model,
        boolean toolsEnabled
) {
    private static final String DEFAULT_PROVIDER = "openai";

    public TextChatOptions {
        provider = provider == null || provider.isBlank() ? DEFAULT_PROVIDER : provider.toLowerCase();
    }

    /** User-facing chat defaults: tools enabled unless explicitly disabled. */
    public static TextChatOptions defaults() {
        return new TextChatOptions(DEFAULT_PROVIDER, null, true);
    }

    /** Internal LLM calls (titles, eval, analysis) without tool calling. */
    public static TextChatOptions withoutTools() {
        return new TextChatOptions(DEFAULT_PROVIDER, null, false);
    }

    public static TextChatOptions of(String provider, String model) {
        return new TextChatOptions(provider, model, true);
    }

    /**
     * @param toolsEnabled {@code null} defaults to enabled; only explicit {@code false} disables tools
     */
    public static TextChatOptions of(String provider, String model, Boolean toolsEnabled) {
        return new TextChatOptions(provider, model, toolsEnabled == null || toolsEnabled);
    }

    public static TextChatOptions ollamaVision(String model) {
        return new TextChatOptions("ollama", model, false);
    }
}
