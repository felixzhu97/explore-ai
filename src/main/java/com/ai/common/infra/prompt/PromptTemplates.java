package com.ai.common.infra.prompt;

import java.util.Map;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Catalog of composed prompts loaded from {@code classpath:prompts/**}. Shared style/GFM fragments
 * are the single source for chat, RAG system, and agents.
 */
public class PromptTemplates {

  private final String sharedStyle;
  private final String defaultSystemPrompt;
  private final String ragSystemPrompt;
  private final PromptTemplate summarizationTemplate;
  private final PromptTemplate translationTemplate;
  private final PromptTemplate questionAnswerTemplate;
  private final String afterToolsReminder;

  public PromptTemplates() {
    this.sharedStyle = ClasspathPromptLoader.load("shared/style-minimal.st");
    String gfm = ClasspathPromptLoader.load("shared/format-gfm.st");
    String formatting = ClasspathPromptLoader.joinSections(gfm, sharedStyle);

    this.defaultSystemPrompt =
        ClasspathPromptLoader.joinSections(
            ClasspathPromptLoader.load("chat/system-role.st"),
            ClasspathPromptLoader.load("chat/tools-policy.st"),
            formatting,
            ClasspathPromptLoader.load("chat/a2ui-chart.st"),
            ClasspathPromptLoader.load("chat/mermaid-diagram.st"));

    this.ragSystemPrompt =
        ClasspathPromptLoader.joinSections(
            ClasspathPromptLoader.load("rag/system-role.st"),
            formatting,
            ClasspathPromptLoader.load("chat/a2ui-chart.st"),
            ClasspathPromptLoader.load("chat/mermaid-diagram.st"));

    this.summarizationTemplate =
        new PromptTemplate(ClasspathPromptLoader.load("task/summarization.st"));
    this.translationTemplate =
        new PromptTemplate(ClasspathPromptLoader.load("task/translation.st"));
    this.questionAnswerTemplate =
        new PromptTemplate(ClasspathPromptLoader.load("task/question-answer.st"));
    this.afterToolsReminder = ClasspathPromptLoader.load("guards/after-tools.st");
  }

  /** Returns the shared style instructions. */
  public String getSharedStyleInstructions() {
    return sharedStyle;
  }

  /** Returns the reminder added after tool calls. */
  public String getAfterToolsReminder() {
    return afterToolsReminder;
  }

  /** Returns the default system prompt. */
  public String getDefaultSystemPrompt() {
    return defaultSystemPrompt;
  }

  /** Returns the RAG system prompt. */
  public String getRagSystemPrompt() {
    return ragSystemPrompt;
  }

  /** Builds a prompt that summarizes the text. */
  public String buildSummarizationPrompt(String text) {
    return summarizationTemplate.render(Map.of("text", text));
  }

  /** Builds a prompt that translates the text. */
  public String buildTranslationPrompt(String text, String targetLanguage) {
    return translationTemplate.render(Map.of("text", text, "targetLanguage", targetLanguage));
  }

  /** Builds a prompt that answers the question from the context. */
  public String buildQuestionAnswerPrompt(String context, String question) {
    return questionAnswerTemplate.render(Map.of("context", context, "question", question));
  }

  /** Returns the default system prompt with any custom instructions appended. */
  public String buildCustomSystemPrompt(String customInstructions) {
    if (customInstructions == null || customInstructions.isEmpty()) {
      return defaultSystemPrompt;
    }
    return defaultSystemPrompt + "\n\n" + customInstructions;
  }

  /** Loads the system prompt of an agent. */
  public String loadAgentSystemPrompt(String agentKey) {
    String body = ClasspathPromptLoader.load("agent/" + agentKey + ".st");
    return ClasspathPromptLoader.joinSections(body, sharedStyle);
  }
}
