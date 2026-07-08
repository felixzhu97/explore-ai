package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.chat.application.usecase.TextProviderCatalog;
import com.ai.chat.web.dto.ChatStreamRequest;
import com.ai.chat.web.dto.ModelInfoResponse;
import com.ai.chat.web.dto.ProviderInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextControllerTest {

    @Mock
    private ChatUseCase chatUseCase;

    @Mock
    private TextProviderCatalog providerCatalog;

    private TextController controller;

    @BeforeEach
    void setUp() {
        controller = new TextController(chatUseCase, providerCatalog);
    }

    @Test
    void should_return_providers_when_listProviders_called() {
        when(providerCatalog.listProviders()).thenReturn(List.of(
                new ProviderInfoResponse("openai", "DeepSeek", List.of("deepseek-v4-flash"), "available")
        ));

        var providers = controller.listProviders();

        assertThat(providers).hasSize(1);
        assertThat(providers.getFirst().name()).isEqualTo("openai");
        assertThat(providers.getFirst().displayName()).isEqualTo("DeepSeek");
    }

    @Test
    void should_return_models_when_listModels_called() {
        when(providerCatalog.listModels("openai")).thenReturn(List.of(
                new ModelInfoResponse("deepseek-v4-flash", "openai", "DeepSeek chat model")
        ));

        var response = controller.listModels("openai");

        assertThat(response.provider()).isEqualTo("openai");
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.models().getFirst().name()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void should_use_session_stream_when_session_id_provided() {
        when(chatUseCase.chatStreamWithSession("session-1", "Hello"))
                .thenReturn(Flux.just("Hi", " there"));

        Flux<String> result = controller.chatStream(new ChatStreamRequest(
                List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
                "session-1",
                "openai",
                "deepseek-v4-flash"
        ));

        StepVerifier.create(result)
                .expectNext("Hi", " there")
                .verifyComplete();
        verify(chatUseCase).chatStreamWithSession("session-1", "Hello");
    }

    @Test
    void should_use_stateless_stream_when_session_id_missing() {
        when(chatUseCase.chatStream(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Flux.just("token"));

        Flux<String> result = controller.chatStream(new ChatStreamRequest(
                List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
                null,
                null,
                null
        ));

        StepVerifier.create(result)
                .expectNext("token")
                .verifyComplete();
    }
}
