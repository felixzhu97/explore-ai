package com.ai.common.application.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextChatOptions")
class TextChatOptionsTest {

    @Test
    @DisplayName("should enable tools by default")
    void shouldEnableToolsByDefault() {
        assertThat(TextChatOptions.defaults().toolsEnabled()).isTrue();
        assertThat(TextChatOptions.of("openai", "gpt-4o").toolsEnabled()).isTrue();
        assertThat(TextChatOptions.of("openai", "gpt-4o", null).toolsEnabled()).isTrue();
    }

    @Test
    @DisplayName("should preserve null skill system prompt when not provided")
    void shouldPreserveNullSkillSystemPromptWhenNotProvided() {
        assertThat(TextChatOptions.of("openai", "gpt-4o").skillSystemPrompt()).isNull();
        assertThat(TextChatOptions.of("openai", "gpt-4o", false, "prompt").skillSystemPrompt())
                .isEqualTo("prompt");
    }
}
