package com.ai.chat.infrastructure.prompt;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalizedRagPromptBuilder")
class LocalizedRagPromptBuilderTest {

    private final LocalizedRagPromptBuilder builder =
            new LocalizedRagPromptBuilder(new LanguageDetectionService(), new PromptTemplates());

    @Nested
    @DisplayName("build()")
    class Build {

        @Test
        @DisplayName("should return no context message when context null")
        void shouldReturnNoContextMessageWhenContextNull() {
            String prompt = builder.build("What is AI?", null, "en");

            assertThat(prompt).contains("I don't have relevant documents");
        }

        @Test
        @DisplayName("should return no context message when context blank")
        void shouldReturnNoContextMessageWhenContextBlank() {
            String prompt = builder.build("What is AI?", "   ", "en");

            assertThat(prompt).contains("I don't have relevant documents");
        }

        @Test
        @DisplayName("should build english prompt with shared style when context present")
        void shouldBuildEnglishPromptWithSharedStyleWhenContextPresent() {
            String prompt = builder.build("What is AI?", "AI is Artificial Intelligence", "en");

            assertThat(prompt).contains("AI is Artificial Intelligence");
            assertThat(prompt).contains("What is AI?");
            assertThat(prompt).contains("helpful assistant");
            assertThat(prompt).contains("Markdown");
            assertThat(prompt).contains("minimal and high-value");
            assertThat(prompt).contains("No decorative emoji");
        }

        @Test
        @DisplayName("should build chinese prompt when language zh")
        void shouldBuildChinesePromptWhenLanguageZh() {
            String prompt = builder.build("什么是AI?", "AI是人工智能", "zh");

            assertThat(prompt).contains("AI是人工智能");
            assertThat(prompt).contains("什么是AI?");
            assertThat(prompt).contains("中文回答");
            assertThat(prompt).contains("minimal and high-value");
        }

        @Test
        @DisplayName("should build japanese prompt when language ja")
        void shouldBuildJapanesePromptWhenLanguageJa() {
            String prompt = builder.build("AIとは何ですか？", "AIは人工知能です", "ja");

            assertThat(prompt).contains("AIは人工知能です");
            assertThat(prompt).contains("AIとは何ですか？");
            assertThat(prompt).contains("日本語で回答");
        }

        @Test
        @DisplayName("should use english template when language unknown")
        void shouldUseEnglishTemplateWhenLanguageUnknown() {
            String prompt = builder.build("Question", "Context", "unknown");

            assertThat(prompt).contains("Context");
            assertThat(prompt).contains("Question");
            assertThat(prompt).contains("helpful assistant");
        }

        @Test
        @DisplayName("should include formatting guidelines when chinese prompt")
        void shouldIncludeFormattingGuidelinesWhenChinesePrompt() {
            String prompt = builder.build("问题", "上下文", "zh");

            assertThat(prompt).contains("**粗体**");
            assertThat(prompt).contains("*斜体*");
            assertThat(prompt).contains("列表");
            assertThat(prompt).contains("## 标题");
            assertThat(prompt).contains("中文回答");
        }

        @Test
        @DisplayName("should detect language when build without explicit code")
        void shouldDetectLanguageWhenBuildWithoutExplicitCode() {
            String prompt = builder.build("你好，请介绍一下文档", "文档内容");

            assertThat(prompt).contains("文档内容");
            assertThat(prompt).contains("中文回答");
        }
    }

    @Nested
    @DisplayName("no context messages")
    class NoContextMessages {

        @Test
        @DisplayName("should return chinese message when zh")
        void shouldReturnChineseMessageWhenZh() {
            assertThat(builder.build("?", null, "zh")).contains("文档").contains("上传");
        }

        @Test
        @DisplayName("should return japanese message when ja")
        void shouldReturnJapaneseMessageWhenJa() {
            assertThat(builder.build("?", null, "ja")).contains("ドキュメント").contains("アップロード");
        }

        @Test
        @DisplayName("should return english message when en")
        void shouldReturnEnglishMessageWhenEn() {
            assertThat(builder.build("?", null, "en")).contains("documents").contains("upload");
        }
    }
}
