package com.ai.audio.infra.tts;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechRepository;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/** Loopback TTS to explore-ml media-gen (Qwen3-TTS by default). */
@Repository
@ConditionalOnProperty(name = "app.ai.tts.provider", havingValue = "media-gen")
public class MediaGenTextToSpeechRepository implements TextToSpeechRepository {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  /** Documentation. */
  public MediaGenTextToSpeechRepository(
      @Value("${app.ai.tts.media-gen-base-url:${MEDIA_GENERATION_API_URL:http://localhost:8003}}")
          String baseUrl,
      @Value("${app.ai.tts.media-gen.connect-timeout:5s}") Duration connectTimeout,
      @Value("${app.ai.tts.media-gen.read-timeout:120s}") Duration readTimeout,
      ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    this.restClient =
        RestClient.builder().baseUrl(trimSlash(baseUrl)).requestFactory(factory).build();
  }

  @Override
  public SynthesizedAudio synthesize(SpeechText text, VoiceSelection voiceSelection, Double speed) {
    Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("text", text.value());
    if (voiceSelection != null
        && voiceSelection.voice() != null
        && !voiceSelection.voice().isBlank()) {
      body.put("voice", voiceSelection.voice());
    }
    String json =
        restClient
            .post()
            .uri("/api/v1/voices:synthesize")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);
    try {
      JsonNode node = objectMapper.readTree(json == null ? "{}" : json);
      String audioUrl = node.path("audio_url").asText("");
      if (audioUrl.isBlank()) {
        return SynthesizedAudio.empty();
      }
      byte[] audio =
          RestClient.create().get().uri(URI.create(audioUrl)).retrieve().body(byte[].class);
      String mediaType = audioUrl.endsWith(".wav") ? "audio/wav" : "audio/mpeg";
      return SynthesizedAudio.create(audio, mediaType);
    } catch (Exception e) {
      return SynthesizedAudio.empty();
    }
  }

  private static String trimSlash(String baseUrl) {
    String base = baseUrl == null ? "http://localhost:8003" : baseUrl.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base;
  }
}
