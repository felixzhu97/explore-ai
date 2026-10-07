package com.ai.chat.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DetectedLanguage")
class DetectedLanguageTest {

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("should return default for null or blank text")
  void shouldReturnDefaultForNullOrBlank(String text) {
    assertThat(DetectedLanguage.createLanguage(text).getCode()).isEqualTo("default");
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "\t", "\n"})
  @DisplayName("should return default for whitespace-only text")
  void shouldReturnDefaultForWhitespaceOnly(String text) {
    assertThat(DetectedLanguage.createLanguage(text).getCode()).isEqualTo("default");
  }

  @Test
  @DisplayName("should detect English text")
  void shouldDetectEnglishText() {
    assertThat(DetectedLanguage.createLanguage("Hello, how are you?").getCode()).isEqualTo("en");
  }

  @Test
  @DisplayName("should detect English with some special characters")
  void shouldDetectEnglishWithSpecialChars() {
    assertThat(DetectedLanguage.createLanguage("Hello! How are you? I'm fine.").getCode())
        .isEqualTo("en");
  }

  @Test
  @DisplayName("should detect Chinese text")
  void shouldDetectChineseText() {
    assertThat(DetectedLanguage.createLanguage("你好，这是一段中文文本").getCode()).isEqualTo("zh");
  }

  @Test
  @DisplayName("should detect Japanese text with hiragana")
  void shouldDetectJapaneseWithHiragana() {
    assertThat(DetectedLanguage.createLanguage("これは日本語のテキストです").getCode()).isEqualTo("ja");
  }

  @Test
  @DisplayName("should detect Japanese text with katakana")
  void shouldDetectJapaneseWithKatakana() {
    assertThat(DetectedLanguage.createLanguage("これはカタカナで書かれています").getCode()).isEqualTo("ja");
  }

  @Test
  @DisplayName("should detect based on character distribution")
  void shouldDetectBasedOnCharacterDistribution() {
    String englishText = "Hello world this is a test message for language detection";
    assertThat(DetectedLanguage.createLanguage(englishText).getCode()).isEqualTo("en");

    String chineseText = "中文文本内容测试数据";
    assertThat(DetectedLanguage.createLanguage(chineseText).getCode()).isEqualTo("zh");
  }

  @Test
  @DisplayName("should return en or default for short text")
  void shouldReturnEnOrDefaultForShortText() {
    String shortText = "Hi";
    assertThat(DetectedLanguage.createLanguage(shortText).getCode()).isIn("en", "default");
  }

  @Test
  @DisplayName("should detect Japanese when kana content exceeds threshold")
  void shouldDetectJapaneseWhenKanaExceedsThreshold() {
    String text = "あいうえおかきくけこさしすせそたちつてと";
    assertThat(DetectedLanguage.createLanguage(text).getCode()).isEqualTo("ja");
  }
}
