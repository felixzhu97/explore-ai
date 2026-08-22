package com.ai.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.service.usecase.ChatUseCase;
import com.ai.chat.service.usecase.TextProviderCatalog;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reactor.core.publisher.Flux;

@SliceWebMvcTest(controllers = TextController.class)
@DisplayName("TextController")
class TextControllerTest {

  private static final String CLIENT_ID = "c:11111111-1111-1111-1111-111111111111";

  @Autowired private MockMvcTester mvc;

  @MockitoBean private ChatUseCase chatUseCase;

  @MockitoBean private TextProviderCatalog providerCatalog;

  @MockitoBean private SkillRepository skillRepository;

  @MockitoBean private OwnerContext ownerContext;

  @BeforeEach
  void setUp() {
    lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
  }

  @Nested
  @DisplayName("GET /api/text/providers")
  class ListProviders {

    @Test
    @DisplayName("should return providers")
    void shouldReturnProvidersWhenListProvidersCalled() {
      when(providerCatalog.listProviders())
          .thenReturn(
              List.of(
                  new com.ai.chat.controller.dto.ProviderInfoResponse(
                      "openai", "DeepSeek", List.of("deepseek-v4-flash"), "available")));

      assertThat(mvc.get().uri("/api/text/providers"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$")
          .asArray()
          .hasSize(1);

      assertThat(mvc.get().uri("/api/text/providers"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[0].name")
          .asString()
          .isEqualTo("openai");

      assertThat(mvc.get().uri("/api/text/providers"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$[0].displayName")
          .asString()
          .isEqualTo("DeepSeek");
    }
  }

  @Nested
  @DisplayName("GET /api/text/models")
  class ListModels {

    @Test
    @DisplayName("should return models for provider")
    void shouldReturnModelsWhenListModelsCalled() {
      when(providerCatalog.listModels("openai"))
          .thenReturn(
              List.of(
                  new com.ai.chat.controller.dto.ModelInfoResponse(
                      "deepseek-v4-flash", "openai", "DeepSeek chat model")));

      assertThat(mvc.get().uri("/api/text/models").param("provider", "openai"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.provider")
          .asString()
          .isEqualTo("openai");

      assertThat(mvc.get().uri("/api/text/models").param("provider", "openai"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.count")
          .convertTo(Integer.class)
          .isEqualTo(1);

      assertThat(mvc.get().uri("/api/text/models").param("provider", "openai"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.models[0].name")
          .asString()
          .isEqualTo("deepseek-v4-flash");
    }
  }

  @Nested
  @DisplayName("POST /api/text/chat/stream")
  class ChatStream {

    @Test
    @DisplayName("should use session stream when sessionId provided")
    void shouldUseSessionStreamWhenSessionIdProvided() {
      when(chatUseCase.chatStreamWithSession(
              "22222222-2222-2222-2222-222222222222",
              "Hello",
              TextChatOptions.of("openai", "deepseek-v4-flash", false),
              CLIENT_ID))
          .thenReturn(Flux.just("Hi", " there"));

      assertThat(
              mvc.post()
                  .uri("/api/text/chat/stream")
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
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID))
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("Hi")
          .contains("there");
      verify(chatUseCase)
          .chatStreamWithSession(
              "22222222-2222-2222-2222-222222222222",
              "Hello",
              TextChatOptions.of("openai", "deepseek-v4-flash", false),
              CLIENT_ID);
    }

    @Test
    @DisplayName("should use stateless stream when sessionId missing")
    void shouldUseStatelessStreamWhenSessionIdMissing() {
      when(chatUseCase.chatStream(any(), any(TextChatOptions.class)))
          .thenReturn(Flux.just("token"));

      assertThat(
              mvc.post()
                  .uri("/api/text/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "messages": [{"role": "user", "content": "Hello"}],
                        "toolsEnabled": false
                      }
                      """)
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("token");
    }

    @Test
    @DisplayName("should attach skill system prompt when skillIds provided")
    void shouldAttachSkillSystemPromptWhenSkillIdsProvided() {
      com.ai.skill.domain.vo.SkillId skillId = com.ai.skill.domain.vo.SkillId.generate();
      com.ai.skill.domain.model.Skill skill =
          com.ai.skill.domain.model.Skill.restore(
              skillId,
              CLIENT_ID,
              "Brief Style",
              "Short answers.",
              "Be concise.",
              List.of(),
              true,
              java.time.Instant.now(),
              java.time.Instant.now());
      when(skillRepository.findEnabledByClientIdAndIds(eq(CLIENT_ID), any()))
          .thenReturn(List.of(skill));
      when(chatUseCase.chatStream(any(), any(TextChatOptions.class))).thenReturn(Flux.just("ok"));

      assertThat(
              mvc.post()
                  .uri("/api/text/chat/stream")
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
                          .formatted(skillId.value()))
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID))
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk();

      verify(chatUseCase)
          .chatStream(
              any(),
              org.mockito.ArgumentMatchers.argThat(
                  options ->
                      options.skillSystemPrompt() != null
                          && options.skillSystemPrompt().contains("## Active Skills")
                          && options.skillSystemPrompt().contains("Brief Style")));
    }
  }
}
