package com.ai.chat.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageDetectionService")
class LanguageDetectionServiceTest {

    private final LanguageDetectionService service = new LanguageDetectionService();

    @Nested
    @DisplayName("detect()")
    class Detect {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return default for null or blank text")
        void shouldReturnDefaultForNullOrBlank(String text) {
            assertThat(service.detect(text)).isEqualTo("default");
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should return default for whitespace-only text")
        void shouldReturnDefaultForWhitespaceOnly(String text) {
            assertThat(service.detect(text)).isEqualTo("default");
        }

        @Test
        @DisplayName("should detect English text")
        void shouldDetectEnglishText() {
            assertThat(service.detect("Hello, how are you?")).isEqualTo("en");
        }

        @Test
        @DisplayName("should detect English with some special characters")
        void shouldDetectEnglishWithSpecialChars() {
            assertThat(service.detect("Hello! How are you? I'm fine.")).isEqualTo("en");
        }

        @Test
        @DisplayName("should detect Chinese text")
        void shouldDetectChineseText() {
            assertThat(service.detect("你好，这是一段中文文本")).isEqualTo("zh");
        }

        @Test
        @DisplayName("should detect Japanese text with hiragana")
        void shouldDetectJapaneseWithHiragana() {
            assertThat(service.detect("これは日本語のテキストです")).isEqualTo("ja");
        }

        @Test
        @DisplayName("should detect Japanese text with katakana")
        void shouldDetectJapaneseWithKatakana() {
            assertThat(service.detect("これはカタカナで書かれています")).isEqualTo("ja");
        }

        @Test
        @DisplayName("should detect based on character distribution")
        void shouldDetectBasedOnCharacterDistribution() {
            String englishText = "Hello world this is a test message for language detection";
            assertThat(service.detect(englishText)).isEqualTo("en");

            String chineseText = "中文文本内容测试数据";
            assertThat(service.detect(chineseText)).isEqualTo("zh");
        }

        @Test
        @DisplayName("should return en or default for short text")
        void shouldReturnEnOrDefaultForShortText() {
            String shortText = "Hi";
            assertThat(service.detect(shortText)).isIn("en", "default");
        }

        @Test
        @DisplayName("should detect English when numbers are present")
        void shouldDetectEnglishWithNumbers() {
            assertThat(service.detect("Order #12345 has been shipped")).isEqualTo("en");
        }

        @Test
        @DisplayName("should handle text with only Chinese characters")
        void shouldHandlePureChineseCharacters() {
            assertThat(service.detect("机器学习")).isEqualTo("zh");
        }

        @Test
        @DisplayName("should detect Japanese when kana content exceeds threshold")
        void shouldDetectJapaneseWhenKanaExceedsThreshold() {
            String text = "あいうえおかきくけこさしすせそたちつてと";
            assertThat(service.detect(text)).isEqualTo("ja");
        }
    }

    @Nested
    @DisplayName("buildPrompt()")
    class BuildPrompt {

        @Test
        @DisplayName("should return no context message for null context")
        void shouldReturnNoContextMessageForNullContext() {
            String prompt = service.buildPrompt("What is AI?", null, "en");

            assertThat(prompt).contains("I don't have relevant documents");
        }

        @Test
        @DisplayName("should return no context message for blank context")
        void shouldReturnNoContextMessageForBlankContext() {
            String prompt = service.buildPrompt("What is AI?", "   ", "en");

            assertThat(prompt).contains("I don't have relevant documents");
        }

        @Test
        @DisplayName("should build English prompt with context")
        void shouldBuildEnglishPromptWithContext() {
            String prompt = service.buildPrompt("What is AI?", "AI is Artificial Intelligence", "en");

            assertThat(prompt).contains("AI is Artificial Intelligence");
            assertThat(prompt).contains("What is AI?");
            assertThat(prompt).contains("helpful assistant");
            assertThat(prompt).contains("Markdown");
        }

        @Test
        @DisplayName("should build Chinese prompt with context")
        void shouldBuildChinesePromptWithContext() {
            String prompt = service.buildPrompt("什么是AI?", "AI是人工智能", "zh");

            assertThat(prompt).contains("AI是人工智能");
            assertThat(prompt).contains("什么是AI?");
            assertThat(prompt).contains("中文回答");
        }

        @Test
        @DisplayName("should build Japanese prompt with context")
        void shouldBuildJapanesePromptWithContext() {
            String prompt = service.buildPrompt("AIとは何ですか？", "AIは人工知能です", "ja");

            assertThat(prompt).contains("AIは人工知能です");
            assertThat(prompt).contains("AIとは何ですか？");
            assertThat(prompt).contains("日本語で回答");
        }

        @Test
        @DisplayName("should use default template for unknown language code")
        void shouldUseDefaultTemplateForUnknownLanguageCode() {
            String prompt = service.buildPrompt("Question", "Context", "unknown");

            assertThat(prompt).contains("Context");
            assertThat(prompt).contains("Question");
            assertThat(prompt).contains("helpful assistant");
        }

        @Test
        @DisplayName("should include formatting guidelines in Chinese prompt")
        void shouldIncludeFormattingGuidelinesInChinesePrompt() {
            String prompt = service.buildPrompt("问题", "上下文", "zh");

            assertThat(prompt).contains("**粗体**");
            assertThat(prompt).contains("*斜体*");
            assertThat(prompt).contains("列表");
            assertThat(prompt).contains("## 标题");
            assertThat(prompt).contains("中文回答");
        }
    }

    @Nested
    @DisplayName("no context messages")
    class NoContextMessages {

        @Test
        @DisplayName("should return Chinese message for zh code")
        void shouldReturnChineseMessageForZhCode() {
            String message = service.buildPrompt("?", null, "zh");

            assertThat(message).contains("文档");
            assertThat(message).contains("上传");
        }

        @Test
        @DisplayName("should return Japanese message for ja code")
        void shouldReturnJapaneseMessageForJaCode() {
            String message = service.buildPrompt("?", null, "ja");

            assertThat(message).contains("ドキュメント");
            assertThat(message).contains("アップロード");
        }

        @Test
        @DisplayName("should return English message for default code")
        void shouldReturnEnglishMessageForDefaultCode() {
            String message = service.buildPrompt("?", null, "en");

            assertThat(message).contains("documents");
            assertThat(message).contains("upload");
        }
    }
}
