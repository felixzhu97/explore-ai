package com.ai.vision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.chat.infra.prompt.LocalizedRagPromptBuilder;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.StreamTokenEvent;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.rag.domain.model.SourceCitation;
import com.ai.rag.service.RagApplicationService;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisionChatService")
class VisionChatServiceTest {

  private static final String OWNER = "c:owner";

  @Mock private RagApplicationService ragApplicationService;

  @Mock private ChatClientProvider chatClientProvider;

  @Mock private LocalizedRagPromptBuilder localizedRagPromptBuilder;

  @Mock private ChatClient chatClient;

  @Mock private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.StreamResponseSpec streamResponseSpec;

  private VisionChatService visionChatService;

  @BeforeEach
  void setUp() {
    visionChatService =
        new VisionChatService(
            "qwen3.5:35b",
            ragApplicationService,
            chatClientProvider,
            localizedRagPromptBuilder,
            new ObjectMapper());
  }

  @Test
  @DisplayName("should emit token stream and sources when images provided")
  void shouldEmitTokenStreamAndSourcesWhenImagesProvided() {
    when(ragApplicationService.retrieveContext(anyString(), any(), any(Integer.class), eq(OWNER)))
        .thenReturn(
            new RagApplicationService.RetrievalResult(
                "context chunk",
                List.of(new SourceCitation("source text", 0.9, Map.of())),
                "question"));
    when(localizedRagPromptBuilder.build(anyString(), anyString())).thenReturn("prompt");
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
    when(requestSpec.stream()).thenReturn(streamResponseSpec);
    when(streamResponseSpec.content()).thenReturn(Flux.just("Hello ", "world"));

    StepVerifier.create(
            visionChatService.streamChatWithImages(
                "What is in the image?", null, List.of("iVBORw0KGgo="), 5, OWNER))
        .assertNext(event -> assertThat(event.data()).isEqualTo(StreamTokenEvent.toJson("Hello ")))
        .assertNext(event -> assertThat(event.data()).isEqualTo(StreamTokenEvent.toJson("world")))
        .assertNext(
            event -> {
              assertThat(event.event()).isEqualTo("sources");
              assertThat(event.data()).contains("source text");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should emit error event when stream fails")
  void shouldEmitErrorEventWhenStreamFails() {
    when(ragApplicationService.retrieveContext(anyString(), any(), any(Integer.class), eq(OWNER)))
        .thenReturn(new RagApplicationService.RetrievalResult("context", List.of(), "question"));
    when(localizedRagPromptBuilder.build(anyString(), anyString())).thenReturn("prompt");
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
    when(requestSpec.stream()).thenReturn(streamResponseSpec);
    when(streamResponseSpec.content()).thenReturn(Flux.error(new RuntimeException("model down")));

    StepVerifier.create(
            visionChatService.streamChatWithImages(
                "question", null, List.of("iVBORw0KGgo="), 5, OWNER))
        .assertNext(
            event -> {
              assertThat(event.event()).isEqualTo("error");
              assertThat(event.data()).contains("model down");
            })
        .verifyComplete();
  }
}
