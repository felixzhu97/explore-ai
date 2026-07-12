package com.ai.common.infrastructure.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PromptTemplates Unit Tests
 *
 * Tests for PromptTemplates following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests prompt building and template formatting
 */
@DisplayName("PromptTemplates")
class PromptTemplatesTest {

    private final PromptTemplates templates = new PromptTemplates();

    @Nested
    @DisplayName("getDefaultSystemPrompt")
    class GetDefaultSystemPrompt {

        @Test
        @DisplayName("should return non-null system prompt")
        void shouldReturnNonNullSystemPrompt() {
            // Act
            String prompt = templates.getDefaultSystemPrompt();

            // Assert
            assertThat(prompt).isNotNull();
            assertThat(prompt).isNotBlank();
        }

        @Test
        @DisplayName("should contain helpful AI assistant reference")
        void shouldContainHelpfulAiAssistantReference() {
            // Act
            String prompt = templates.getDefaultSystemPrompt();

            // Assert
            assertThat(prompt.toLowerCase()).contains("helpful");
            assertThat(prompt.toLowerCase()).contains("assistant");
        }

        @Test
        @DisplayName("should include GFM markdown formatting instructions")
        void shouldIncludeGfmMarkdownFormattingInstructions() {
            String prompt = templates.getDefaultSystemPrompt();

            assertThat(prompt.toLowerCase()).contains("github flavored markdown");
            assertThat(prompt).contains("# through ###");
            assertThat(prompt).contains("- ");
        }
    }

    @Nested
    @DisplayName("getRagSystemPrompt")
    class GetRagSystemPrompt {

        @Test
        @DisplayName("should return non-null RAG prompt")
        void shouldReturnNonNullRagPrompt() {
            // Act
            String prompt = templates.getRagSystemPrompt();

            // Assert
            assertThat(prompt).isNotNull();
            assertThat(prompt).isNotBlank();
        }

        @Test
        @DisplayName("should mention knowledge base")
        void shouldMentionKnowledgeBase() {
            // Act
            String prompt = templates.getRagSystemPrompt();

            // Assert
            assertThat(prompt.toLowerCase()).contains("knowledge base");
        }

        @Test
        @DisplayName("should mention context usage")
        void shouldMentionContextUsage() {
            // Act
            String prompt = templates.getRagSystemPrompt();

            // Assert
            assertThat(prompt.toLowerCase()).contains("context");
        }

        @Test
        @DisplayName("should mention source citation")
        void shouldMentionSourceCitation() {
            // Act
            String prompt = templates.getRagSystemPrompt();

            // Assert
            assertThat(prompt.toLowerCase()).contains("cite");
        }
    }

    @Nested
    @DisplayName("buildSummarizationPrompt")
    class BuildSummarizationPrompt {

        @Test
        @DisplayName("should include text in prompt")
        void shouldIncludeTextInPrompt() {
            // Arrange
            String text = "This is a test text for summarization.";

            // Act
            String prompt = templates.buildSummarizationPrompt(text);

            // Assert
            assertThat(prompt).contains(text);
        }

        @Test
        @DisplayName("should mention JSON response format")
        void shouldMentionJsonResponseFormat() {
            // Act
            String prompt = templates.buildSummarizationPrompt("Test text");

            // Assert
            assertThat(prompt.toLowerCase()).contains("json");
        }

        @Test
        @DisplayName("should include summary field reference")
        void shouldIncludeSummaryFieldReference() {
            // Act
            String prompt = templates.buildSummarizationPrompt("Test");

            // Assert
            assertThat(prompt).contains("summary");
        }

        @Test
        @DisplayName("should include sentiment field reference")
        void shouldIncludeSentimentFieldReference() {
            // Act
            String prompt = templates.buildSummarizationPrompt("Test");

            // Assert
            assertThat(prompt).contains("sentiment");
        }

        @Test
        @DisplayName("should include key_points field reference")
        void shouldIncludeKeyPointsFieldReference() {
            // Act
            String prompt = templates.buildSummarizationPrompt("Test");

            // Assert
            assertThat(prompt).contains("key_points");
        }

        @Test
        @DisplayName("should include entities field reference")
        void shouldIncludeEntitiesFieldReference() {
            // Act
            String prompt = templates.buildSummarizationPrompt("Test");

            // Assert
            assertThat(prompt).contains("entities");
        }

        @Test
        @DisplayName("should include language field reference")
        void shouldIncludeLanguageFieldReference() {
            // Act
            String prompt = templates.buildSummarizationPrompt("Test");

            // Assert
            assertThat(prompt).contains("language");
        }

        @Test
        @DisplayName("should handle empty text")
        void shouldHandleEmptyText() {
            // Arrange
            String text = "";

            // Act
            String prompt = templates.buildSummarizationPrompt(text);

            // Assert
            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("summary");
        }

        @Test
        @DisplayName("should handle long text")
        void shouldHandleLongText() {
            // Arrange
            String longText = "word ".repeat(1000);

            // Act
            String prompt = templates.buildSummarizationPrompt(longText);

            // Assert
            assertThat(prompt).contains(longText);
        }

        @Test
        @DisplayName("should handle unicode text")
        void shouldHandleUnicodeText() {
            // Arrange
            String unicodeText = "你好世界 Hello مرحبا";

            // Act
            String prompt = templates.buildSummarizationPrompt(unicodeText);

            // Assert
            assertThat(prompt).contains(unicodeText);
        }
    }

    @Nested
    @DisplayName("buildTranslationPrompt")
    class BuildTranslationPrompt {

        @Test
        @DisplayName("should include text in prompt")
        void shouldIncludeTextInPrompt() {
            // Arrange
            String text = "Hello, world!";
            String targetLang = "Chinese";

            // Act
            String prompt = templates.buildTranslationPrompt(text, targetLang);

            // Assert
            assertThat(prompt).contains(text);
        }

        @Test
        @DisplayName("should include target language")
        void shouldIncludeTargetLanguage() {
            // Arrange
            String text = "Hello";
            String targetLang = "French";

            // Act
            String prompt = templates.buildTranslationPrompt(text, targetLang);

            // Assert
            assertThat(prompt).contains(targetLang);
        }

        @Test
        @DisplayName("should request translation only")
        void shouldRequestTranslationOnly() {
            // Act
            String prompt = templates.buildTranslationPrompt("Test", "German");

            // Assert
            assertThat(prompt.toLowerCase()).contains("only");
            assertThat(prompt.toLowerCase()).contains("translation");
        }

        @Test
        @DisplayName("should not include commentary")
        void shouldNotIncludeCommentary() {
            // Act
            String prompt = templates.buildTranslationPrompt("Test", "Spanish");

            // Assert
            assertThat(prompt.toLowerCase()).contains("without");
            assertThat(prompt.toLowerCase()).contains("commentary");
        }
    }

    @Nested
    @DisplayName("buildQuestionAnswerPrompt")
    class BuildQuestionAnswerPrompt {

        @Test
        @DisplayName("should include context in prompt")
        void shouldIncludeContextInPrompt() {
            // Arrange
            String context = "This is the relevant context.";
            String question = "What is this?";

            // Act
            String prompt = templates.buildQuestionAnswerPrompt(context, question);

            // Assert
            assertThat(prompt).contains(context);
        }

        @Test
        @DisplayName("should include question in prompt")
        void shouldIncludeQuestionInPrompt() {
            // Arrange
            String context = "Context here";
            String question = "What is the answer?";

            // Act
            String prompt = templates.buildQuestionAnswerPrompt(context, question);

            // Assert
            assertThat(prompt).contains(question);
        }

        @Test
        @DisplayName("should mention context-based answer")
        void shouldMentionContextBasedAnswer() {
            // Act
            String prompt = templates.buildQuestionAnswerPrompt("Context", "Question");

            // Assert
            assertThat(prompt.toLowerCase()).contains("based on");
        }

        @Test
        @DisplayName("should handle empty context")
        void shouldHandleEmptyContext() {
            // Act
            String prompt = templates.buildQuestionAnswerPrompt("", "Question");

            // Assert
            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("Question");
        }

        @Test
        @DisplayName("should handle long context")
        void shouldHandleLongContext() {
            // Arrange
            String longContext = "Context ".repeat(100);

            // Act
            String prompt = templates.buildQuestionAnswerPrompt(longContext, "Q");

            // Assert
            assertThat(prompt).contains(longContext);
        }
    }

    @Nested
    @DisplayName("buildCustomSystemPrompt")
    class BuildCustomSystemPrompt {

        @Test
        @DisplayName("should include default prompt")
        void shouldIncludeDefaultPrompt() {
            // Arrange
            String customInstructions = "Be more formal.";

            // Act
            String prompt = templates.buildCustomSystemPrompt(customInstructions);

            // Assert
            assertThat(prompt).contains("helpful");
            assertThat(prompt).contains("assistant");
        }

        @Test
        @DisplayName("should include custom instructions")
        void shouldIncludeCustomInstructions() {
            // Arrange
            String customInstructions = "Always respond in JSON format.";

            // Act
            String prompt = templates.buildCustomSystemPrompt(customInstructions);

            // Assert
            assertThat(prompt).contains(customInstructions);
        }

        @Test
        @DisplayName("should separate prompts with newline")
        void shouldSeparatePromptsWithNewline() {
            // Arrange
            String customInstructions = "Be concise.";

            // Act
            String prompt = templates.buildCustomSystemPrompt(customInstructions);

            // Assert
            assertThat(prompt).contains("\n\n");
        }

        @Test
        @DisplayName("should handle empty custom instructions")
        void shouldHandleEmptyCustomInstructions() {
            // Act
            String prompt = templates.buildCustomSystemPrompt("");

            // Assert
            assertThat(prompt).isNotNull();
            assertThat(prompt).isNotBlank();
        }

        @Test
        @DisplayName("should handle long custom instructions")
        void shouldHandleLongCustomInstructions() {
            // Arrange
            String longInstructions = "Instruction ".repeat(100);

            // Act
            String prompt = templates.buildCustomSystemPrompt(longInstructions);

            // Assert
            assertThat(prompt).contains(longInstructions);
        }
    }
}
