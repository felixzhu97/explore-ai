package com.ai.common.application.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextChatOptions")
class TextChatOptionsTest {

    @Test
    @DisplayName("should_enable_tools_by_default")
    void should_enable_tools_by_default() {
        assertThat(TextChatOptions.defaults().toolsEnabled()).isTrue();
        assertThat(TextChatOptions.of("openai", "gpt-4o").toolsEnabled()).isTrue();
        assertThat(TextChatOptions.of("openai", "gpt-4o", null).toolsEnabled()).isTrue();
    }

    @Test
    @DisplayName("should_disable_tools_when_explicitly_false")
    void should_disable_tools_when_explicitly_false() {
        assertThat(TextChatOptions.of("openai", "gpt-4o", false).toolsEnabled()).isFalse();
        assertThat(TextChatOptions.withoutTools().toolsEnabled()).isFalse();
    }
}
