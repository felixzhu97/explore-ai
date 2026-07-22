package com.ai.chat.infrastructure.prompt;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.infrastructure.prompt.ClasspathPromptLoader;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.springframework.stereotype.Component;

/**
 * Builds localized RAG/Vision user prompts from classpath templates,
 * injecting the shared minimal style fragment.
 */
@Component
public class LocalizedRagPromptBuilder {

    private final LanguageDetectionService languageDetectionService;
    private final PromptTemplates promptTemplates;

    public LocalizedRagPromptBuilder(
            LanguageDetectionService languageDetectionService,
            PromptTemplates promptTemplates) {
        this.languageDetectionService = languageDetectionService;
        this.promptTemplates = promptTemplates;
    }

    public String build(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        return build(question, context, languageCode);
    }

    public String build(String question, String context, String languageCode) {
        if (context == null || context.isBlank()) {
            return noContextMessage(languageCode);
        }
        String template = userTemplate(languageCode);
        return ClasspathPromptLoader.fill(
                ClasspathPromptLoader.fill(
                        ClasspathPromptLoader.fill(template, "style", promptTemplates.getSharedStyleInstructions()),
                        "context",
                        context),
                "question",
                question);
    }

    private String userTemplate(String languageCode) {
        return switch (languageCode) {
            case "zh" -> ClasspathPromptLoader.load("rag/user-zh.st");
            case "ja" -> ClasspathPromptLoader.load("rag/user-ja.st");
            default -> ClasspathPromptLoader.load("rag/user-en.st");
        };
    }

    private String noContextMessage(String languageCode) {
        return switch (languageCode) {
            case "zh" -> ClasspathPromptLoader.load("rag/no-context-zh.st");
            case "ja" -> ClasspathPromptLoader.load("rag/no-context-ja.st");
            default -> ClasspathPromptLoader.load("rag/no-context-en.st");
        };
    }
}
