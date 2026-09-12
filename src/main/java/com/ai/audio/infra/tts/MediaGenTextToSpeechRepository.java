package com.ai.audio.infra.tts;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechRepository;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

/** Loopback TTS to explore-ml media-gen (local Qwen3-TTS by default). */
@Repository
@ConditionalOnProperty(
    name = "app.ai.tts.provider",
    havingValue = "media-gen",
    matchIfMissing = true)
public class MediaGenTextToSpeechRepository implements TextToSpeechRepository {

  private static final Logger log = LoggerFactory.getLogger(MediaGenTextToSpeechRepository.class);

  /** OpenAI TTS catalog voices — not valid Qwen3-TTS speakers. */
  private static final Set<String> OPENAI_VOICES =
      Set.of("alloy", "echo", "fable", "onyx", "nova", "shimmer");

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  /** Documentation. */
  public MediaGenTextToSpeechRepository(
      @Value("${app.ai.tts.media-gen-base-url:${MEDIA_GENERATION_API_URL:http://localhost:8003}}")
          String baseUrl,
      @Value("${app.ai.tts.media-gen.connect-timeout:5s}") Duration connectTimeout,
      @Value("${app.ai.tts.media-gen.read-timeout:120s}") Duration readTimeout,
      ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.baseUrl = trimSlash(baseUrl);
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    this.restClient = RestClient.builder().baseUrl(this.baseUrl).requestFactory(factory).build();
  }

  @Override
  public SynthesizedAudio synthesize(SpeechText text, VoiceSelection voiceSelection, Double speed) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("text", text.value());
    String qwenSpeaker = qwenSpeakerOrNull(voiceSelection);
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
        log.warn("media-gen TTS returned empty audio_url");
        return SynthesizedAudio.empty();
      }
      URI uri = resolveAudioUri(audioUrl);
      byte[] audio = RestClient.create().get().uri(uri).retrieve().body(byte[].class);
      if (audio == null || audio.length == 0) {
        log.warn("media-gen TTS audio download empty: {}", uri);
        return SynthesizedAudio.empty();
      }
      String mediaType = audioUrl.endsWith(".wav") ? "audio/wav" : "audio/mpeg";
      return SynthesizedAudio.create(audio, mediaType);
    } catch (Exception e) {
      log.error("media-gen TTS failed", e);
      return SynthesizedAudio.empty();
    }
  }

  /** Prefer Qwen speakers; omit OpenAI aliases so media-gen picks TTS_SPEAKER. */
  static String qwenSpeakerOrNull(VoiceSelection voiceSelection) {
    if (voiceSelection == null
        || voiceSelection.voice() == null
        || voiceSelection.voice().isBlank()) {
      return null;
    }
    String voice = voiceSelection.voice().trim();
    if (OPENAI_VOICES.contains(voice.toLowerCase())) {
      return null;
    }
    return voice;
  }

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
    String base = baseUrl == null ? "http://localhost:8003" : baseUrl.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base;
  }
}
