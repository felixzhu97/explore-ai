package com.ai.chat.controller;

import static com.ai.testsupport.MvcStreamTestSupport.STREAM_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.controller.dto.ProviderStatus;
import com.ai.chat.service.ChatService;
import com.ai.chat.service.TextProviderCatalog;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.skill.service.SkillService;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

@SliceWebMvcTest(controllers = ChatStreamController.class)
@DisplayName("ChatStreamController")
class ChatStreamControllerTest extends AbstractOwnerScopedControllerTest {

  @MockitoBean private ChatService chatService;

  @MockitoBean private TextProviderCatalog providerCatalog;

  @MockitoBean private SkillService skillService;

  @Test
  @DisplayName("should return providers")
  void shouldReturnProvidersWhenListProvidersCalled() {
    when(providerCatalog.listProviders())
        .thenReturn(
            List.of(
                new com.ai.chat.controller.dto.ProviderInfoResponse(
                    "openai", "DeepSeek", List.of("deepseek-v4-flash"), ProviderStatus.AVAILABLE)));

    assertThat(mvc.get().uri("/api/chat/providers"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$")
        .asArray()
        .hasSize(1);

    assertThat(mvc.get().uri("/api/chat/providers"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].name")
        .asString()
        .isEqualTo("openai");

    assertThat(mvc.get().uri("/api/chat/providers"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].displayName")
        .asString()
        .isEqualTo("DeepSeek");
  }

  @Test
  @DisplayName("should return models for provider")
  void shouldReturnModelsWhenListModelsCalled() {
    when(providerCatalog.listModels("openai"))
        .thenReturn(
            List.of(
                new com.ai.chat.controller.dto.ModelInfoResponse(
                    "deepseek-v4-flash", "openai", "DeepSeek chat model")));

    assertThat(mvc.get().uri("/api/chat/models").param("provider", "openai"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.provider")
        .asString()
        .isEqualTo("openai");

    assertThat(mvc.get().uri("/api/chat/models").param("provider", "openai"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.count")
        .convertTo(Integer.class)
        .isEqualTo(1);

    assertThat(mvc.get().uri("/api/chat/models").param("provider", "openai"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.models[0].name")
        .asString()
        .isEqualTo("deepseek-v4-flash");
  }

  @Nested
  @DisplayName("POST /api/chat/stream")
  class ChatStream {

    @Test
    @DisplayName("should use session stream when sessionId provided")
    void shouldUseSessionStreamWhenSessionIdProvided() {
      when(chatService.streamChatWithSession(
              "22222222-2222-2222-2222-222222222222",
              "Hello",
              TextChatOptions.createOptions("openai", "deepseek-v4-flash", false),
              ownerKey()))
          .thenReturn(Flux.just("Hi", " there"));

      assertThat(
              mvc.post()
                  .uri("/api/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "messages": [{"role": "user", "content": "Hello"}],
                        "sessionId": "22222222-2222-2222-2222-222222222222",
                        "provider": "openai",
                        "model": "deepseek-v4-flash",
                        "toolsEnabled": false
                      }
                      """)
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey()))
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("Hi")
          .contains("there");
      verify(chatService)
          .streamChatWithSession(
              "22222222-2222-2222-2222-222222222222",
              "Hello",
              TextChatOptions.createOptions("openai", "deepseek-v4-flash", false),
              ownerKey());
    }

    @Test
    @DisplayName("should reject request when messages are empty")
    void shouldRejectRequestWhenMessagesAreEmpty() {
      assertThat(
              mvc.post()
                  .uri("/api/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"messages": []}
                      """))
          .hasStatus(400);
    }

    @Test
    @DisplayName("should use stateless stream when sessionId missing")
    void shouldUseStatelessStreamWhenSessionIdMissing() {
      when(chatService.streamChat(any(), any(TextChatOptions.class), eq(ownerKey())))
          .thenReturn(Flux.just("token"));

      assertThat(
              mvc.post()
                  .uri("/api/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "messages": [{"role": "user", "content": "Hello"}],
                        "toolsEnabled": false
                      }
                      """)
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("token");
    }

    @Test
    @DisplayName("should attach skill system prompt when skillIds provided")
    void shouldAttachSkillSystemPromptWhenSkillIdsProvided() {
      String skillId = "11111111-2222-3333-4444-555555555555";
      when(skillService.buildSkillsPrompt(ownerKey(), List.of(skillId)))
          .thenReturn(java.util.Optional.of("## Active Skills\n### Brief Style\nBe concise."));
      when(chatService.streamChat(any(), any(TextChatOptions.class), eq(ownerKey())))
          .thenReturn(Flux.just("ok"));

      assertThat(
              mvc.post()
                  .uri("/api/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "messages": [{"role": "user", "content": "Hello"}],
                        "provider": "openai",
                        "model": "deepseek-v4-flash",
                        "toolsEnabled": false,
                        "skillIds": ["%s"]
                      }
                      """
                          .formatted(skillId))
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey()))
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk();

      verify(chatService)
          .streamChat(
              any(),
              org.mockito.ArgumentMatchers.argThat(
                  options ->
                      options.skillSystemPrompt() != null
                          && options.skillSystemPrompt().contains("## Active Skills")
                          && options.skillSystemPrompt().contains("Brief Style")),
              eq(ownerKey()));
    }
  }
}
