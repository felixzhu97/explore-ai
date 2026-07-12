package com.ai.analysis.infrastructure.llm;

import com.ai.analysis.domain.vo.AnalysisText;
import com.ai.analysis.domain.vo.LanguageHint;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.exception.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiStructuredAnalysisRepository")
class SpringAiStructuredAnalysisRepositoryTest {

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SpringAiStructuredAnalysisRepository repository;

    @BeforeEach
    void setUp() {
        when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        repository = new SpringAiStructuredAnalysisRepository(chatClientProvider);
    }

    @Test
    @DisplayName("should throw when LLM returns null entity")
    void should_throw_when_llm_returns_null_entity() {
        when(callResponseSpec.entity(any(Class.class))).thenReturn(null);

        assertThatThrownBy(() -> repository.analyze(AnalysisText.of("Sample"), LanguageHint.none()))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("empty structured analysis");
    }
}
