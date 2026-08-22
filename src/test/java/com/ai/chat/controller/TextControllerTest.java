package com.ai.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.controller.dto.ChatStreamRequest;
import com.ai.chat.controller.dto.ModelInfoResponse;
import com.ai.chat.controller.dto.ProviderInfoResponse;
import com.ai.chat.service.usecase.ChatUseCase;
import com.ai.chat.service.usecase.TextProviderCatalog;
import com.ai.common.controller.ClientIdentity;
import com.ai.common.service.llm.TextChatOptions;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TextControllerTest {
  private OwnerContext ownerContext;

  private static final String CLIENT_ID = "c:11111111-1111-1111-1111-111111111111";

  @Mock private ChatUseCase chatUseCase;

  @Mock private TextProviderCatalog providerCatalog;

  @Mock private com.ai.skill.domain.repository.SkillRepository skillRepository;

  @Mock private HttpServletRequest httpRequest;

  private TextController controller;

  @BeforeEach
  void setUp() {
    ownerContext = mock(OwnerContext.class);
    lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
    controller = new TextController(chatUseCase, providerCatalog, skillRepository, ownerContext);
    lenient()
        .when(httpRequest.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE))
        .thenReturn(CLIENT_ID);
  }

  @Test
  void shouldReturnProvidersWhenListProvidersCalled() {
    when(providerCatalog.listProviders())
        .thenReturn(
            List.of(
                new ProviderInfoResponse(
                    "openai", "DeepSeek", List.of("deepseek-v4-flash"), "available")));

    var providers = controller.listProviders();

    assertThat(providers).hasSize(1);
    assertThat(providers.getFirst().name()).isEqualTo("openai");
    assertThat(providers.getFirst().displayName()).isEqualTo("DeepSeek");
  }

  @Test
  void shouldReturnModelsWhenListModelsCalled() {
    when(providerCatalog.listModels("openai"))
        .thenReturn(
            List.of(new ModelInfoResponse("deepseek-v4-flash", "openai", "DeepSeek chat model")));

    var response = controller.listModels("openai");

    assertThat(response.provider()).isEqualTo("openai");
    assertThat(response.count()).isEqualTo(1);
    assertThat(response.models().getFirst().name()).isEqualTo("deepseek-v4-flash");
  }

  @Test
  void shouldUseSessionStreamWhenSessionIdProvided() {
    when(chatUseCase.chatStreamWithSession(
            "22222222-2222-2222-2222-222222222222",
            "Hello",
            TextChatOptions.of("openai", "deepseek-v4-flash", false),
            CLIENT_ID))
        .thenReturn(Flux.just("Hi", " there"));

    Flux<String> result =
        controller.chatStream(
            new ChatStreamRequest(
                List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
                "22222222-2222-2222-2222-222222222222",
                "openai",
                "deepseek-v4-flash",
                false,
                null),
            httpRequest);

    StepVerifier.create(result).expectNext("Hi", " there").verifyComplete();
    verify(chatUseCase)
        .chatStreamWithSession(
            "22222222-2222-2222-2222-222222222222",
            "Hello",
            TextChatOptions.of("openai", "deepseek-v4-flash", false),
            CLIENT_ID);
  }

  @Test
  void shouldUseStatelessStreamWhenSessionIdMissing() {
    when(chatUseCase.chatStream(org.mockito.ArgumentMatchers.anyList(), any(TextChatOptions.class)))
        .thenReturn(Flux.just("token"));

    Flux<String> result =
        controller.chatStream(
            new ChatStreamRequest(
                List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
                null,
                null,
                null,
                false,
                null),
            httpRequest);

    StepVerifier.create(result).expectNext("token").verifyComplete();
  }

  @Test
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
    when(skillRepository.findEnabledByClientIdAndIds(
            org.mockito.ArgumentMatchers.eq(CLIENT_ID), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(List.of(skill));
    when(chatUseCase.chatStream(org.mockito.ArgumentMatchers.anyList(), any(TextChatOptions.class)))
        .thenReturn(Flux.just("ok"));

    controller.chatStream(
        new ChatStreamRequest(
            List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
            null,
            "openai",
            "deepseek-v4-flash",
            false,
            List.of(skillId.value())),
        httpRequest);

    verify(chatUseCase)
        .chatStream(
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.argThat(
                options ->
                    options.skillSystemPrompt() != null
                        && options.skillSystemPrompt().contains("## Active Skills")
                        && options.skillSystemPrompt().contains("Brief Style")));
  }
}
