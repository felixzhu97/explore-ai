package com.ai.common.service.llm;

/** Documentation. */
public record TextChatOptions(
    String provider, String model, boolean toolsEnabled, String skillSystemPrompt) {
  private static final String DEFAULT_PROVIDER = "openai";

  /** Documentation. */
  public TextChatOptions {
    provider = provider == null || provider.isBlank() ? DEFAULT_PROVIDER : provider.toLowerCase();
    skillSystemPrompt =
        skillSystemPrompt == null || skillSystemPrompt.isBlank() ? null : skillSystemPrompt;
  }

  /** User-facing chat defaults: tools enabled unless explicitly disabled. */
  public static TextChatOptions defaults() {
    return new TextChatOptions(DEFAULT_PROVIDER, null, true, null);
  }

  /** Internal LLM calls (titles, eval, analysis) without tool calling. */
  public static TextChatOptions withoutTools() {
    return new TextChatOptions(DEFAULT_PROVIDER, null, false, null);
  }

  /** Documentation. */
  public static TextChatOptions of(String provider, String model) {
    return new TextChatOptions(provider, model, true, null);
  }

  /**
   * Creates options with an explicit tools-enabled flag.
   *
   * @param toolsEnabled {@code null} defaults to enabled; only explicit {@code false} disables
   *     tools
   */
  public static TextChatOptions of(String provider, String model, Boolean toolsEnabled) {
    return new TextChatOptions(provider, model, toolsEnabled == null || toolsEnabled, null);
  }

  /** Documentation. */
  public static TextChatOptions of(
      String provider, String model, Boolean toolsEnabled, String skillSystemPrompt) {
    return new TextChatOptions(
        provider, model, toolsEnabled == null || toolsEnabled, skillSystemPrompt);
  }

  /** Documentation. */
  public static TextChatOptions ollamaVision(String model) {
    return new TextChatOptions("ollama", model, false, null);
  }

  /** Documentation. */
  public TextChatOptions withSkillSystemPrompt(String skillSystemPrompt) {
    return new TextChatOptions(provider, model, toolsEnabled, skillSystemPrompt);
  }
}
