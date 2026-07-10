package com.ai.audio.infrastructure.tts;

import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiTextToSpeechRepository")
class SpringAiTextToSpeechRepositoryTest {

    @Mock
    private TextToSpeechModel textToSpeechModel;

    private SpringAiTextToSpeechRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SpringAiTextToSpeechRepository(textToSpeechModel);
    }

    @Nested
    @DisplayName("synthesize()")
    class Synthesize {

        @Test
        @DisplayName("should return audio bytes when synthesis succeeds")
        void should_return_audio_bytes_when_synthesis_succeeds() {
            byte[] expectedAudio = "fake-audio-data".getBytes();
            TextToSpeechResponse mockResponse = mock(TextToSpeechResponse.class);
            org.springframework.ai.audio.tts.Speech mockSpeech =
                    mock(org.springframework.ai.audio.tts.Speech.class);

            when(mockSpeech.getOutput()).thenReturn(expectedAudio);
            when(mockResponse.getResults()).thenReturn(java.util.List.of(mockSpeech));
            when(textToSpeechModel.call(any(TextToSpeechPrompt.class))).thenReturn(mockResponse);

            var audio = repository.synthesize(
                    SpeechText.of("Hello, world!"), VoiceSelection.of("alloy", null), 1.0);

            assertThat(audio.data()).isEqualTo(expectedAudio);
            verify(textToSpeechModel).call(any(TextToSpeechPrompt.class));
        }

        @Test
        @DisplayName("should return empty audio when response is null")
        void should_return_empty_audio_when_response_is_null() {
            when(textToSpeechModel.call(any(TextToSpeechPrompt.class))).thenReturn(null);

            var audio = repository.synthesize(SpeechText.of("Hello"), VoiceSelection.of("alloy", null), null);

            assertThat(audio.isEmpty()).isTrue();
        }
    }
}
