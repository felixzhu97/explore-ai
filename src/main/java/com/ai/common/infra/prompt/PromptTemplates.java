package com.ai.common.infra.prompt;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Catalog of composed prompts loaded from {@code classpath:prompts/**}. Shared style/GFM fragments
 * are the single source for chat, RAG system, and agents.
 */
public class PromptTemplates {

  private static final Logger log = LoggerFactory.getLogger(PromptTemplates.class);

  private final String sharedStyle;
  private final String defaultSystemPrompt;
  private final String ragSystemPrompt;
  private final PromptTemplate summarizationTemplate;
  private final PromptTemplate translationTemplate;
  private final PromptTemplate questionAnswerTemplate;
  private final String afterToolsReminder;

  /** Documentation. */
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

  public String getSharedStyleInstructions() {
    return sharedStyle;
  }

  public String getAfterToolsReminder() {
    return afterToolsReminder;
  }

  public String getDefaultSystemPrompt() {
    return defaultSystemPrompt;
  }

  public String getRagSystemPrompt() {
    return ragSystemPrompt;
  }

  /** Documentation. */
  public String buildSummarizationPrompt(String text) {
    log.debug("Building summarization prompt for text of length: {}", text.length());
    return summarizationTemplate.render(Map.of("text", text));
  }

  /** Documentation. */
  public String buildTranslationPrompt(String text, String targetLanguage) {
    log.debug("Building translation prompt to {}", targetLanguage);
    return translationTemplate.render(Map.of("text", text, "targetLanguage", targetLanguage));
  }

  /** Documentation. */
  public String buildQuestionAnswerPrompt(String context, String question) {
    log.debug("Building Q&A prompt with context length: {}", context.length());
    return questionAnswerTemplate.render(Map.of("context", context, "question", question));
  }

  /** Documentation. */
  public String buildCustomSystemPrompt(String customInstructions) {
    if (customInstructions == null || customInstructions.isEmpty()) {
      return defaultSystemPrompt;
    }
    return defaultSystemPrompt + "\n\n" + customInstructions;
  }

  /** Documentation. */
  public String loadAgentSystemPrompt(String agentKey) {
    String body = ClasspathPromptLoader.load("agent/" + agentKey + ".st");
    return ClasspathPromptLoader.joinSections(body, sharedStyle);
  }
}
