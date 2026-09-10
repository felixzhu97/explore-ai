package com.ai.audio.infra.tts;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.audio.domain.vo.VoiceSelection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeechTextToSpeechRepositoryTest {

  private final SpeechTextToSpeechRepository repository =
      new SpeechTextToSpeechRepository(
          "http://localhost:8004", Duration.ofSeconds(1), Duration.ofSeconds(1), new ObjectMapper());

  @Nested
  @DisplayName("qwenSpeakerOrNull()")
  class QwenSpeakerOrNull {

    @Test
    @DisplayName("should omit OpenAI catalog voices for Qwen TTS")
    void shouldOmitOpenAiCatalogVoicesForQwenTts() {
      assertThat(SpeechTextToSpeechRepository.qwenSpeakerOrNull(VoiceSelection.of("alloy", null)))
          .isNull();
    }

    @Test
    @DisplayName("should keep custom Qwen speaker name")
    void shouldKeepCustomQwenSpeakerName() {
      // VoiceSelection validates against OpenAI catalog; use raw record for Qwen speakers.
      assertThat(
              SpeechTextToSpeechRepository.qwenSpeakerOrNull(
                  new VoiceSelection("vivian", "gpt-4o-mini-tts")))
          .isEqualTo("vivian");
    }
  }

  @Nested
  @DisplayName("resolveAudioUri()")
  class ResolveAudioUri {

    @Test
    @DisplayName("should keep absolute speech audio urls")
    void shouldKeepAbsoluteSpeechAudioUrls() {
      URI uri = repository.resolveAudioUri("http://localhost:8004/output/voice/job.wav");
      assertThat(uri.toString()).isEqualTo("http://localhost:8004/output/voice/job.wav");
    }

    @Test
    @DisplayName("should resolve relative audio paths against speech base")
    void shouldResolveRelativeAudioPathsAgainstSpeechBase() {
      URI uri = repository.resolveAudioUri("/output/voice/job.wav");
      assertThat(uri.toString()).isEqualTo("http://localhost:8004/output/voice/job.wav");
    }
  }
}
