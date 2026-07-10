package com.ai.chat.application.usecase;

import com.ai.chat.infrastructure.llm.ChatClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTitleGenerator")
class SessionTitleGeneratorTest {

    @Mock
    private ChatClientFactory chatClientFactory;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SessionTitleGenerator generator;

    @BeforeEach
    void setUp() {
        lenient().when(chatClientFactory.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        generator = new SessionTitleGenerator(chatClientFactory);
    }

    @Test
    @DisplayName("should return LLM generated title when available")
    void shouldReturnLlmGeneratedTitleWhenAvailable() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(SessionTitleGenerator.SessionTitleResponse.class), any()))
                .thenReturn(new SessionTitleGenerator.SessionTitleResponse("Kubernetes 部署指南"));

        String title = generator.generate("如何部署 K8s？", "你可以使用 kubectl apply...");

        assertThat(title).isEqualTo("Kubernetes 部署指南");
    }

    @Test
    @DisplayName("should fallback to truncated user message when LLM fails")
    void shouldFallbackToTruncatedUserMessageWhenLlmFails() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("LLM unavailable"));

        String title = generator.generate("这是一个非常长的用户消息".repeat(5), "reply");

        assertThat(title).hasSize(50);
    }

    @Test
    @DisplayName("should fallback when LLM returns blank")
    void shouldFallbackWhenLlmReturnsBlank() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(SessionTitleGenerator.SessionTitleResponse.class), any(Consumer.class)))
                .thenReturn(new SessionTitleGenerator.SessionTitleResponse("   "));

        String title = generator.generate("Hello world", "Hi there");

        assertThat(title).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("should fallback immediately when user message is blank")
    void shouldFallbackImmediatelyWhenUserMessageIsBlank() {
        String title = generator.generate("   ", "reply");

        assertThat(title).isEqualTo("New Chat");
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("should fallback immediately when assistant reply is blank")
    void shouldFallbackImmediatelyWhenAssistantReplyIsBlank() {
        String title = generator.generate("Hello world", "   ");

        assertThat(title).isEqualTo("Hello world");
        verifyNoInteractions(chatClient);
    }
}
