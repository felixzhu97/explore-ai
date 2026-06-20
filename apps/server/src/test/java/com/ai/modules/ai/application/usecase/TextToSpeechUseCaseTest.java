package com.ai.modules.ai.application.usecase;

import com.ai.modules.ai.application.usecase.TextToSpeechUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextToSpeechUseCase Tests")
class TextToSpeechUseCaseTest {

    @Mock
    private TextToSpeechModel textToSpeechModel;

    private TextToSpeechUseCase textToSpeechUseCase;

    @BeforeEach
    void setUp() {
        textToSpeechUseCase = new TextToSpeechUseCase(textToSpeechModel);
    }

    @Nested
    @DisplayName("synthesize()")
    class Synthesize {

        @Test
        @DisplayName("should return audio bytes when synthesis succeeds")
        void shouldReturnAudioBytesWhenSynthesisSucceeds() {
            // Arrange
            String text = "Hello, world!";
            byte[] expectedAudio = "fake-audio-data".getBytes();

            TextToSpeechResponse mockResponse = mock(TextToSpeechResponse.class);
            org.springframework.ai.audio.tts.Speech mockSpeech = mock(org.springframework.ai.audio.tts.Speech.class);

            when(mockSpeech.getOutput()).thenReturn(expectedAudio);
            when(mockResponse.getResults()).thenReturn(List.of(mockSpeech));
            when(textToSpeechModel.call(any(TextToSpeechPrompt.class))).thenReturn(mockResponse);

            // Act
            byte[] audio = textToSpeechUseCase.synthesize(text);

            // Assert
            assertThat(audio).isEqualTo(expectedAudio);
            verify(textToSpeechModel).call(any(TextToSpeechPrompt.class));
        }

        @Test
        @DisplayName("should return null when response is null")
        void shouldReturnNullWhenResponseIsNull() {
            // Arrange
            String text = "Hello";
            when(textToSpeechModel.call(any(TextToSpeechPrompt.class))).thenReturn(null);

            // Act
            byte[] audio = textToSpeechUseCase.synthesize(text);

            // Assert
            assertThat(audio).isNull();
        }

        @Test
        @DisplayName("should return null when response results is null")
        void shouldReturnNullWhenResponseResultsIsNull() {
            // Arrange
            String text = "Hello";
            TextToSpeechResponse mockResponse = mock(TextToSpeechResponse.class);
            when(mockResponse.getResults()).thenReturn(null);
            when(textToSpeechModel.call(any(TextToSpeechPrompt.class))).thenReturn(mockResponse);

            // Act
            byte[] audio = textToSpeechUseCase.synthesize(text);

            // Assert
            assertThat(audio).isNull();
        }

        @Test
        @DisplayName("should return null when response results is empty")
        void shouldReturnNullWhenResponseResultsIsEmpty() {
            // Arrange
            String text = "Hello";
            TextToSpeechResponse mockResponse = mock(TextToSpeechResponse.class);
            when(mockResponse.getResults()).thenReturn(List.of());
            when(textToSpeechModel.call(any(TextToSpeechPrompt.class))).thenReturn(mockResponse);

            // Act
            byte[] audio = textToSpeechUseCase.synthesize(text);

            // Assert
            assertThat(audio).isNull();
        }
    }

    @Nested
    @DisplayName("stream()")
    class Stream {

        @Test
        @DisplayName("should return Flux of audio bytes")
        void shouldReturnFluxOfAudioBytes() {
            // Arrange
            String text = "Hello, streaming!";
            byte[] chunk1 = "chunk1".getBytes();
            byte[] chunk2 = "chunk2".getBytes();

            when(textToSpeechModel.stream(text)).thenReturn(Flux.just(chunk1, chunk2));

            // Act
            Flux<byte[]> result = textToSpeechUseCase.stream(text);

            // Assert
            StepVerifier.create(result)
                    .expectNext(chunk1)
                    .expectNext(chunk2)
                    .verifyComplete();

            verify(textToSpeechModel).stream(text);
        }

        @Test
        @DisplayName("should return empty Flux when no audio data")
        void shouldReturnEmptyFluxWhenNoAudioData() {
            // Arrange
            String text = "Silent text";
            when(textToSpeechModel.stream(text)).thenReturn(Flux.empty());

            // Act
            Flux<byte[]> result = textToSpeechUseCase.stream(text);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getAvailableVoices()")
    class GetAvailableVoices {

        @Test
        @DisplayName("should return list of available voices")
        void shouldReturnListOfAvailableVoices() {
            // Act
            List<String> voices = textToSpeechUseCase.getAvailableVoices();

            // Assert
            assertThat(voices)
                    .hasSize(6)
                    .contains("alloy", "echo", "fable", "onyx", "nova", "shimmer");
        }
    }

    @Nested
    @DisplayName("getAvailableModels()")
    class GetAvailableModels {

        @Test
        @DisplayName("should return list of available models")
        void shouldReturnListOfAvailableModels() {
            // Act
            List<String> models = textToSpeechUseCase.getAvailableModels();

            // Assert
            assertThat(models)
                    .hasSize(4)
                    .contains("gpt-4o-mini-tts", "gpt-4o-tts", "tts-1", "tts-1-hd");
        }
    }
}
