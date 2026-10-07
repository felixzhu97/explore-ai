package com.ai.chat.infra.prompt;

import com.ai.chat.domain.model.DetectedLanguage;
import com.ai.common.infra.prompt.ClasspathPromptTemplate;
import com.ai.common.infra.prompt.PromptTemplates;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds localized RAG/Vision user prompts from classpath templates, injecting the shared minimal
 * style fragment.
 */
@Component
@RequiredArgsConstructor
public class LocalizedRagPromptBuilder {

  private final PromptTemplates promptTemplates;

  /** Builds the RAG prompt in the question's language. */
  public String buildPrompt(String question, String context) {
    String languageCode = DetectedLanguage.createLanguage(question).getCode();
    return buildPrompt(question, context, languageCode);
  }

  /** Renders the user prompt for the language, or its no-context message when context is blank. */
  public String buildPrompt(String question, String context, String languageCode) {
    if (context == null || context.isBlank()) {
      return getNoContextMessage(languageCode);
    }
    String template = getUserTemplate(languageCode);
    return ClasspathPromptTemplate.renderTemplate(
        template,
        Map.of(
            "style",
            promptTemplates.getSharedStyleInstructions(),
            "context",
            context,
            "question",
            question));
  }

  private String getUserTemplate(String languageCode) {
    return switch (languageCode) {
      case "zh" -> ClasspathPromptTemplate.loadTemplate("rag/user-zh.st");
      case "ja" -> ClasspathPromptTemplate.loadTemplate("rag/user-ja.st");
      default -> ClasspathPromptTemplate.loadTemplate("rag/user-en.st");
    };
  }

  private String getNoContextMessage(String languageCode) {
    return switch (languageCode) {
      case "zh" -> ClasspathPromptTemplate.loadTemplate("rag/no-context-zh.st");
      case "ja" -> ClasspathPromptTemplate.loadTemplate("rag/no-context-ja.st");
      default -> ClasspathPromptTemplate.loadTemplate("rag/no-context-en.st");
    };
  }
}
