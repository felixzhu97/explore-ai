package com.ai.audio.infra.tts;

import com.ai.audio.domain.model.SpeechText;
import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.model.VoiceSelection;
import com.ai.audio.domain.repository.TextToSpeechGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/** Loopback TTS to explore-ml speech (local Qwen3-TTS by default). */
@Repository
@ConditionalOnProperty(name = "app.ai.tts.provider", havingValue = "speech", matchIfMissing = true)
public class SpeechTextToSpeechGateway implements TextToSpeechGateway {

  /** OpenAI TTS catalog voices — not valid Qwen3-TTS speakers. */
  private static final Set<String> OPENAI_VOICES =
      Set.of("alloy", "echo", "fable", "onyx", "nova", "shimmer");

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  public SpeechTextToSpeechGateway(
      @Value("${app.ai.tts.speech-base-url:${EXPLORE_ML_API_URL:http://localhost:8000}}")
          String baseUrl,
      @Value("${app.ai.tts.speech.connect-timeout:5s}") Duration connectTimeout,
      @Value("${app.ai.tts.speech.read-timeout:120s}") Duration readTimeout,
      ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.baseUrl = trimSlash(baseUrl);
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    this.restClient = RestClient.builder().baseUrl(this.baseUrl).requestFactory(factory).build();
  }

  @Override
  public SynthesizedAudio synthesizeSpeech(
      SpeechText text, VoiceSelection voiceSelection, Double speed) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("text", text.value());
    String qwenSpeaker = resolveQwenSpeaker(voiceSelection);
    if (qwenSpeaker != null) {
      body.put("voice", qwenSpeaker);
    }
    try {
      String json =
          restClient
              .post()
              .uri("/api/v1/voices:synthesize")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);
      JsonNode node = objectMapper.readTree(json == null ? "{}" : json);
      String audioUrl = node.path("audio_url").asText("");
      if (audioUrl.isBlank()) {
        return SynthesizedAudio.createEmptyAudio();
      }
      URI uri = resolveAudioUri(audioUrl);
      byte[] audio = RestClient.create().get().uri(uri).retrieve().body(byte[].class);
      if (audio == null || audio.length == 0) {
        return SynthesizedAudio.createEmptyAudio();
      }
      String mediaType = audioUrl.endsWith(".wav") ? "audio/wav" : "audio/mpeg";
      return SynthesizedAudio.createAudio(audio, mediaType);
    } catch (Exception e) {
      return SynthesizedAudio.createEmptyAudio();
    }
  }

  /** Prefer Qwen speakers; omit OpenAI aliases so speech picks TTS_SPEAKER. */
  static String resolveQwenSpeaker(VoiceSelection voiceSelection) {
    String voice = voiceSelection.voice();
    return OPENAI_VOICES.contains(voice.toLowerCase()) ? null : voice;
  }

  /** Resolves the audio URL returned by the speech service to an absolute URI. */
  URI resolveAudioUri(String audioUrl) {
    if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
      return URI.create(audioUrl);
    }
    if (audioUrl.startsWith("/")) {
      return URI.create(baseUrl + audioUrl);
    }
    return URI.create(baseUrl + "/" + audioUrl);
  }

  private static String trimSlash(String baseUrl) {
    String base = baseUrl.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base;
  }
}
