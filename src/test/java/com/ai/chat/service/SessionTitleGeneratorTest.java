package com.ai.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTitleGenerator")
class SessionTitleGeneratorTest {

  @Mock private ChatClientProvider chatClientProvider;

  @Mock private ChatClient chatClient;

  @Mock(answer = Answers.RETURNS_SELF)
  private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  private SessionTitleGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new SessionTitleGenerator(chatClientProvider);
  }

  @Test
  @DisplayName("should return the llm title when the model answers")
  void shouldReturnTheLlmTitleWhenTheModelAnswers() {
    givenModelTitle("Kubernetes 部署指南");

    String title = generator.generate("如何部署 K8s？", "你可以使用 kubectl apply...").getValue();

    assertThat(title).isEqualTo("Kubernetes 部署指南");
  }

  @Test
  @DisplayName("should fall back to the truncated user message when the llm fails")
  void shouldFallBackToTheTruncatedUserMessageWhenTheLlmFails() {
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenThrow(new RuntimeException("LLM unavailable"));

    String title = generator.generate("这是一个非常长的用户消息".repeat(5), "reply").getValue();

    assertThat(title).hasSize(50);
  }

  @Test
  @DisplayName("should fall back to the user message when the llm returns a blank title")
  void shouldFallBackToTheUserMessageWhenTheLlmReturnsABlankTitle() {
    givenModelTitle("   ");

    String title = generator.generate("Hello world", "Hi there").getValue();

    assertThat(title).isEqualTo("Hello world");
  }

  @Test
  @DisplayName("should not call the llm when the user message is blank")
  void shouldNotCallTheLlmWhenTheUserMessageIsBlank() {
    String title = generator.generate("   ", "reply").getValue();

    assertThat(title).isEqualTo("New Chat");
    verifyNoInteractions(chatClientProvider);
  }

  @Test
  @DisplayName("should not call the llm when the assistant reply is blank")
  void shouldNotCallTheLlmWhenTheAssistantReplyIsBlank() {
    String title = generator.generate("Hello world", "   ").getValue();

    assertThat(title).isEqualTo("Hello world");
    verifyNoInteractions(chatClientProvider);
  }

  private void givenModelTitle(String title) {
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.entity(eq(SessionTitleGenerator.SessionTitleResponse.class), any()))
        .thenReturn(new SessionTitleGenerator.SessionTitleResponse(title));
  }
}
