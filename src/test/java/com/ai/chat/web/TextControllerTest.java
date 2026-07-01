package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.web.dto.ChatStreamRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextController")
class TextControllerTest {

    @Mock
    private ChatUseCase chatUseCase;

    private TextController controller;

    @BeforeEach
    void setUp() {
        controller = new TextController(chatUseCase);
    }

    @Nested
    @DisplayName("POST /api/text/chat/stream")
    class ChatStream {

        @Test
        @DisplayName("should stream with provided session")
        void shouldStreamWithProvidedSession() {
            when(chatUseCase.chatStreamWithSession(anyString(), anyList()))
                    .thenReturn(Flux.just("Hel", "lo"));

            var request = new ChatStreamRequest(
                    List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
                    "session-123",
                    null,
                    null);

            StepVerifier.create(controller.chatStream(request))
                    .expectNext("Hel", "lo")
                    .verifyComplete();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(chatUseCase).chatStreamWithSession(eq("session-123"), messagesCaptor.capture());
            assertThat(messagesCaptor.getValue()).hasSize(1);
            assertThat(messagesCaptor.getValue().get(0).getText()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("should stream with default session when session is blank")
        void shouldStreamWithDefaultSessionWhenSessionIsBlank() {
            when(chatUseCase.chatStreamWithSession(anyList()))
                    .thenReturn(Flux.just("Hi"));

            var request = new ChatStreamRequest(
                    List.of(new ChatStreamRequest.ChatMessageDto("user", "Hello")),
                    " ",
                    null,
                    null);

            StepVerifier.create(controller.chatStream(request))
                    .expectNext("Hi")
                    .verifyComplete();

            verify(chatUseCase).chatStreamWithSession(anyList());
        }
    }
}
