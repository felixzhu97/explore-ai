package com.ai.rag.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.infrastructure.prompt.LocalizedRagPromptBuilder;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.rag.domain.model.SourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisionChatUseCase")
class VisionChatUseCaseTest {

  @Mock private RagApplicationService ragApplicationService;

  @Mock private ChatClientProvider chatClientProvider;

  @Mock private LocalizedRagPromptBuilder localizedRagPromptBuilder;

  @Mock private ChatClient chatClient;

  @Mock private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.StreamResponseSpec streamResponseSpec;

  private VisionChatUseCase visionChatUseCase;

  @BeforeEach
  void setUp() {
    visionChatUseCase =
        new VisionChatUseCase(
            ragApplicationService,
            chatClientProvider,
            localizedRagPromptBuilder,
            new ObjectMapper());
    ReflectionTestUtils.setField(visionChatUseCase, "visionModel", "qwen3.5:35b");
  }

  @Test
  @DisplayName("should emit token stream and sources when images provided")
  void shouldEmitTokenStreamAndSourcesWhenImagesProvided() {
    when(ragApplicationService.retrieveContext(anyString(), any(), any(Integer.class)))
        .thenReturn(
            new RagApplicationService.RetrievalResult(
                "context chunk",
                List.of(new SourceDocument("source text", 0.9, Map.of())),
                "question"));
    when(localizedRagPromptBuilder.build(anyString(), anyString())).thenReturn("prompt");
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
    when(requestSpec.stream()).thenReturn(streamResponseSpec);
    when(streamResponseSpec.content()).thenReturn(Flux.just("Hello ", "world"));

    StepVerifier.create(
            visionChatUseCase.chatStreamWithImages(
                "What is in the image?", null, List.of("iVBORw0KGgo="), null))
        .assertNext(event -> assertThat(event.data()).isEqualTo("Hello "))
        .assertNext(event -> assertThat(event.data()).isEqualTo("world"))
        .assertNext(
            event -> {
              assertThat(event.event()).isEqualTo("sources");
              assertThat(event.data()).contains("source text");
            })
        .verifyComplete();

    verify(requestSpec).stream();
  }

  @Test
  @DisplayName("should emit error event when stream fails")
  void shouldEmitErrorEventWhenStreamFails() {
    when(ragApplicationService.retrieveContext(anyString(), any(), any(Integer.class)))
        .thenReturn(new RagApplicationService.RetrievalResult("context", List.of(), "question"));
    when(localizedRagPromptBuilder.build(anyString(), anyString())).thenReturn("prompt");
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
    when(requestSpec.stream()).thenReturn(streamResponseSpec);
    when(streamResponseSpec.content()).thenReturn(Flux.error(new RuntimeException("model down")));

    StepVerifier.create(
            visionChatUseCase.chatStreamWithImages("question", null, List.of("iVBORw0KGgo="), null))
        .assertNext(
            event -> {
              assertThat(event.event()).isEqualTo("error");
              assertThat(event.data()).contains("model down");
            })
        .verifyComplete();
  }
}
