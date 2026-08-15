package com.ai.common.infrastructure.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PromptTemplates Unit Tests.
 *
 * <p>Tests for PromptTemplates following TDD principles: - Naming convention:
 * should_expected_result_when_condition - Uses AAA pattern (Arrange-Act-Assert) - Tests prompt
 * building and template formatting
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

    @Test
    @DisplayName(
        "should keep replies minimal and forbid decorative emoji when default system prompt built")
    void shouldKeepRepliesMinimalAndForbidDecorativeEmojiWhenDefaultSystemPromptBuilt() {
      String prompt = templates.getDefaultSystemPrompt();

      assertThat(prompt).contains("minimal and high-value");
      assertThat(prompt).contains("No decorative emoji");
      assertThat(prompt).contains("explicitly asks");
    }

    @Test
    @DisplayName("should expose shared style when catalog loaded")
    void shouldExposeSharedStyleWhenCatalogLoaded() {
      assertThat(templates.getSharedStyleInstructions()).contains("minimal and high-value");
      assertThat(templates.getAfterToolsReminder()).contains("Produce your final answer now");
    }

    @Test
    @DisplayName(
        "should keep a2ui format without tool orchestration when default system prompt built")
    void shouldKeepA2uiFormatWithoutToolOrchestrationWhenDefaultSystemPromptBuilt() {
      String prompt = templates.getDefaultSystemPrompt();

      assertThat(prompt).contains("```a2ui");
      assertThat(prompt).contains("https://explore-ai.local/catalogs/chat-v0.9");
      assertThat(prompt).contains("\"version\": \"v0.9\"");
      assertThat(prompt).contains("Chart");
      assertThat(prompt)
          .contains("Do NOT output executable JavaScript or bare ECharts option JSON");
      assertThat(prompt).contains("do NOT invent or guess");
      assertThat(prompt).contains("柱状图");
      assertThat(prompt).contains("柱状图/条形图→bar");
      assertThat(prompt).contains("散点图→scatter");
      assertThat(prompt).contains("组合图/双轴趋势量→combo");
      assertThat(prompt).contains("矩形树图→treemap");
      assertThat(prompt).contains("桑基图→sankey");
      assertThat(prompt).contains("K线图/蜡烛图→candlestick");
      assertThat(prompt).contains("categories");
      assertThat(prompt).contains("注明来源");
      assertThat(prompt).contains("getWeather");
      assertThat(prompt).contains("not web search");
      assertThat(prompt).doesNotContain("Call searchWeb once");
      assertThat(prompt).doesNotContain("Do NOT call any tool again after searchWeb returns");
      assertThat(prompt).doesNotContain("Preferred chain for live facts");
      assertThat(prompt).doesNotContain("进行在线搜索，最新一季度全球电动汽车市场各品牌份额");
      assertThat(prompt).doesNotContain("Web search: current events, live facts, weather");
    }

    @Test
    @DisplayName(
        "should prefer mermaid fences for structure diagrams when default system prompt built")
    void shouldPreferMermaidFencesForStructureDiagramsWhenDefaultSystemPromptBuilt() {
      String prompt = templates.getDefaultSystemPrompt();

      assertThat(prompt).contains("```mermaid");
      assertThat(prompt).contains("flowchart TD");
      assertThat(prompt).contains("sequenceDiagram");
      assertThat(prompt).contains("流程图");
      assertThat(prompt).contains("This chat renders closed Mermaid fences");
      assertThat(prompt).contains("Do NOT use PlantUML");
      assertThat(prompt).contains("Do NOT tell the user to paste into mermaid.live");
      assertThat(prompt).contains("structure diagram");
      assertThat(templates.getAfterToolsReminder()).contains("```mermaid");
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

    @Test
    @DisplayName("should include A2UI chart instructions like default prompt")
    void shouldIncludeA2uiChartInstructionsLikeDefaultPrompt() {
      String prompt = templates.getRagSystemPrompt();

      assertThat(prompt).contains("```a2ui");
      assertThat(prompt).contains("https://explore-ai.local/catalogs/chat-v0.9");
      assertThat(prompt).contains("createSurface");
    }

    @Test
    @DisplayName("should include Mermaid structure diagram instructions like default prompt")
    void shouldIncludeMermaidStructureDiagramInstructionsLikeDefaultPrompt() {
      String prompt = templates.getRagSystemPrompt();

      assertThat(prompt).contains("```mermaid");
      assertThat(prompt).contains("Do NOT use PlantUML");
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
