package com.ai.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextToSpeechService Tests")
class TextToSpeechServiceTest {

    @Mock
    private org.springframework.ai.audio.tts.TextToSpeechModel textToSpeechModel;

    private TextToSpeechService service;

    @BeforeEach
    void setUp() {
        service = new TextToSpeechService(textToSpeechModel);
    }

    @Nested
    @DisplayName("getAvailableVoices")
    class GetAvailableVoicesTests {

        @Test
        @DisplayName("should return available voices")
        void shouldReturnAvailableVoices() {
            List<String> voices = service.getAvailableVoices();

            assertThat(voices).contains("alloy", "echo", "fable", "onyx", "nova", "shimmer");
        }
    }

    @Nested
    @DisplayName("getAvailableModels")
    class GetAvailableModelsTests {

        @Test
        @DisplayName("should return available models")
        void shouldReturnAvailableModels() {
            List<String> models = service.getAvailableModels();

            assertThat(models).contains("gpt-4o-mini-tts", "gpt-4o-tts", "tts-1", "tts-1-hd");
        }
    }
}
