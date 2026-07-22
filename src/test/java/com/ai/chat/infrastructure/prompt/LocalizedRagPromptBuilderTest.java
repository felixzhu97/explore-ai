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
        @DisplayName("should_returnNoContextMessage_when_contextNull")
        void should_returnNoContextMessage_when_contextNull() {
            String prompt = builder.build("What is AI?", null, "en");

            assertThat(prompt).contains("I don't have relevant documents");
        }

        @Test
        @DisplayName("should_returnNoContextMessage_when_contextBlank")
        void should_returnNoContextMessage_when_contextBlank() {
            String prompt = builder.build("What is AI?", "   ", "en");

            assertThat(prompt).contains("I don't have relevant documents");
        }

        @Test
        @DisplayName("should_buildEnglishPromptWithSharedStyle_when_contextPresent")
        void should_buildEnglishPromptWithSharedStyle_when_contextPresent() {
            String prompt = builder.build("What is AI?", "AI is Artificial Intelligence", "en");

            assertThat(prompt).contains("AI is Artificial Intelligence");
            assertThat(prompt).contains("What is AI?");
            assertThat(prompt).contains("helpful assistant");
            assertThat(prompt).contains("Markdown");
            assertThat(prompt).contains("minimal and high-value");
            assertThat(prompt).contains("No decorative emoji");
        }

        @Test
        @DisplayName("should_buildChinesePrompt_when_languageZh")
        void should_buildChinesePrompt_when_languageZh() {
            String prompt = builder.build("什么是AI?", "AI是人工智能", "zh");

            assertThat(prompt).contains("AI是人工智能");
            assertThat(prompt).contains("什么是AI?");
            assertThat(prompt).contains("中文回答");
            assertThat(prompt).contains("minimal and high-value");
        }

        @Test
        @DisplayName("should_buildJapanesePrompt_when_languageJa")
        void should_buildJapanesePrompt_when_languageJa() {
            String prompt = builder.build("AIとは何ですか？", "AIは人工知能です", "ja");

            assertThat(prompt).contains("AIは人工知能です");
            assertThat(prompt).contains("AIとは何ですか？");
            assertThat(prompt).contains("日本語で回答");
        }

        @Test
        @DisplayName("should_useEnglishTemplate_when_languageUnknown")
        void should_useEnglishTemplate_when_languageUnknown() {
            String prompt = builder.build("Question", "Context", "unknown");

            assertThat(prompt).contains("Context");
            assertThat(prompt).contains("Question");
            assertThat(prompt).contains("helpful assistant");
        }

        @Test
        @DisplayName("should_includeFormattingGuidelines_when_chinesePrompt")
        void should_includeFormattingGuidelines_when_chinesePrompt() {
            String prompt = builder.build("问题", "上下文", "zh");

            assertThat(prompt).contains("**粗体**");
            assertThat(prompt).contains("*斜体*");
            assertThat(prompt).contains("列表");
            assertThat(prompt).contains("## 标题");
            assertThat(prompt).contains("中文回答");
        }

        @Test
        @DisplayName("should_detectLanguage_when_buildWithoutExplicitCode")
        void should_detectLanguage_when_buildWithoutExplicitCode() {
            String prompt = builder.build("你好，请介绍一下文档", "文档内容");

            assertThat(prompt).contains("文档内容");
            assertThat(prompt).contains("中文回答");
        }
    }

    @Nested
    @DisplayName("no context messages")
    class NoContextMessages {

        @Test
        @DisplayName("should_returnChineseMessage_when_zh")
        void should_returnChineseMessage_when_zh() {
            assertThat(builder.build("?", null, "zh")).contains("文档").contains("上传");
        }

        @Test
        @DisplayName("should_returnJapaneseMessage_when_ja")
        void should_returnJapaneseMessage_when_ja() {
            assertThat(builder.build("?", null, "ja")).contains("ドキュメント").contains("アップロード");
        }

        @Test
        @DisplayName("should_returnEnglishMessage_when_en")
        void should_returnEnglishMessage_when_en() {
            assertThat(builder.build("?", null, "en")).contains("documents").contains("upload");
        }
    }
}
