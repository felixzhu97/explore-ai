package com.ai.audio.infra.tts;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.audio.domain.vo.VoiceSelection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MediaGenTextToSpeechRepositoryTest {

  private final MediaGenTextToSpeechRepository repository =
      new MediaGenTextToSpeechRepository(
          "http://localhost:8003",
          Duration.ofSeconds(1),
          Duration.ofSeconds(1),
          new ObjectMapper());

  @Nested
  @DisplayName("qwenSpeakerOrNull()")
  class QwenSpeakerOrNull {

    @Test
    @DisplayName("should omit OpenAI catalog voices for Qwen TTS")
    void shouldOmitOpenAiCatalogVoicesForQwenTts() {
      assertThat(MediaGenTextToSpeechRepository.qwenSpeakerOrNull(VoiceSelection.of("alloy", null)))
          .isNull();
    }

    @Test
    @DisplayName("should keep custom Qwen speaker name")
    void shouldKeepCustomQwenSpeakerName() {
      // VoiceSelection validates against OpenAI catalog; use raw record for Qwen speakers.
      assertThat(
              MediaGenTextToSpeechRepository.qwenSpeakerOrNull(
                  new VoiceSelection("vivian", "gpt-4o-mini-tts")))
          .isEqualTo("vivian");
    }
  }

  @Nested
  @DisplayName("resolveAudioUri()")
  class ResolveAudioUri {

    @Test
    @DisplayName("should keep absolute media-gen audio urls")
    void shouldKeepAbsoluteMediaGenAudioUrls() {
      URI uri = repository.resolveAudioUri("http://localhost:8003/output/voice/job.wav");
      assertThat(uri.toString()).isEqualTo("http://localhost:8003/output/voice/job.wav");
    }

    @Test
    @DisplayName("should resolve relative audio paths against media-gen base")
    void shouldResolveRelativeAudioPathsAgainstMediaGenBase() {
      URI uri = repository.resolveAudioUri("/output/voice/job.wav");
      assertThat(uri.toString()).isEqualTo("http://localhost:8003/output/voice/job.wav");
    }
  }
}
