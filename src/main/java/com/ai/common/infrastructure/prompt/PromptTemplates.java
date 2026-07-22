package com.ai.common.infrastructure.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog of composed prompts loaded from {@code classpath:prompts/**}.
 * Shared style/GFM fragments are the single source for chat, RAG system, and agents.
 */
public class PromptTemplates {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplates.class);

    private final String sharedStyle;
    private final String defaultSystemPrompt;
    private final String ragSystemPrompt;
    private final String summarizationTemplate;
    private final String translationTemplate;
    private final String questionAnswerTemplate;
    private final String afterToolsReminder;

    public PromptTemplates() {
        this.sharedStyle = ClasspathPromptLoader.load("shared/style-minimal.st");
        String gfm = ClasspathPromptLoader.load("shared/format-gfm.st");
        String formatting = ClasspathPromptLoader.joinSections(gfm, sharedStyle);

        this.defaultSystemPrompt = ClasspathPromptLoader.joinSections(
                ClasspathPromptLoader.load("chat/system-role.st"),
                ClasspathPromptLoader.load("chat/tools-policy.st"),
                formatting,
                ClasspathPromptLoader.load("chat/a2ui-chart.st"));

        this.ragSystemPrompt = ClasspathPromptLoader.joinSections(
                ClasspathPromptLoader.load("rag/system-role.st"),
                formatting,
                ClasspathPromptLoader.load("chat/a2ui-chart.st"));

        this.summarizationTemplate = ClasspathPromptLoader.load("task/summarization.st");
        this.translationTemplate = ClasspathPromptLoader.load("task/translation.st");
        this.questionAnswerTemplate = ClasspathPromptLoader.load("task/question-answer.st");
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

    public String buildSummarizationPrompt(String text) {
        log.debug("Building summarization prompt for text of length: {}", text.length());
        return ClasspathPromptLoader.fill(summarizationTemplate, "text", text);
    }

    public String buildTranslationPrompt(String text, String targetLanguage) {
        log.debug("Building translation prompt to {}", targetLanguage);
        return ClasspathPromptLoader.fill(
                ClasspathPromptLoader.fill(translationTemplate, "text", text),
                "targetLanguage",
                targetLanguage);
    }

    public String buildQuestionAnswerPrompt(String context, String question) {
        log.debug("Building Q&A prompt with context length: {}", context.length());
        return ClasspathPromptLoader.fill(
                ClasspathPromptLoader.fill(questionAnswerTemplate, "context", context),
                "question",
                question);
    }

    public String buildCustomSystemPrompt(String customInstructions) {
        if (customInstructions == null || customInstructions.isEmpty()) {
            return defaultSystemPrompt;
        }
        return defaultSystemPrompt + "\n\n" + customInstructions;
    }

    public String loadAgentSystemPrompt(String agentKey) {
        String body = ClasspathPromptLoader.load("agent/" + agentKey + ".st");
        return ClasspathPromptLoader.joinSections(body, sharedStyle);
    }
}
