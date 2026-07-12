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

    public static TextChatOptions defaults() {
        return new TextChatOptions(DEFAULT_PROVIDER, null, false);
    }

    public static TextChatOptions of(String provider, String model) {
        return new TextChatOptions(provider, model, false);
    }

    public static TextChatOptions of(String provider, String model, Boolean toolsEnabled) {
        return new TextChatOptions(provider, model, Boolean.TRUE.equals(toolsEnabled));
    }

    public static TextChatOptions ollamaVision(String model) {
        return new TextChatOptions("ollama", model, false);
    }
}
