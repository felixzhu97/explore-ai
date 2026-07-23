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
        @DisplayName("should detect Japanese when kana content exceeds threshold")
        void shouldDetectJapaneseWhenKanaExceedsThreshold() {
            String text = "あいうえおかきくけこさしすせそたちつてと";
            assertThat(service.detect(text)).isEqualTo("ja");
        }
    }
}
