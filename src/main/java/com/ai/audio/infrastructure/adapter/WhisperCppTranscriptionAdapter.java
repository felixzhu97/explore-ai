package com.ai.audio.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Adapter for whisper.cpp streaming transcription via OpenAI-compatible API.
 */
@Component
public class WhisperCppTranscriptionAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhisperCppTranscriptionAdapter.class);

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    @Autowired
    public WhisperCppTranscriptionAdapter(
            @Value("${app.asr.whisper-cpp.base-url:http://localhost:8178}") String baseUrl,
            @Value("${app.asr.whisper-cpp.model:whisper-base}") String model,
            @Value("${app.asr.whisper-cpp.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.asr.whisper-cpp.read-timeout:60s}") Duration readTimeout,
            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory(connectTimeout, readTimeout))
                .build();
    }

    WhisperCppTranscriptionAdapter(RestClient restClient, String model, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    /**
     * Transcribe audio chunk and send partial results via WebSocket.
     */
    public void streamAudioChunk(WebSocketSession session, StringBuilder transcript, String payload) {
        try {
            Map<String, String> message = objectMapper.readValue(payload, new TypeReference<>() {});
            if (!"audio".equals(message.get("type"))) {
                sendMessage(session, "error", "Unsupported message type");
                return;
            }

            String audioData = message.get("data");
            if (audioData == null || audioData.isBlank()) {
                return;
            }

            String partial = transcribeChunk(audioData);
            if (partial == null || partial.isBlank()) {
                sendMessage(session, "error", "Transcription returned empty result");
                return;
            }

            synchronized (transcript) {
                transcript.append(partial);
            }
            sendMessage(session, "partial", partial);
        } catch (RestClientResponseException e) {
            log.warn("whisper.cpp returned status: {}", e.getStatusCode().value());
            sendMessage(session, "error", "Transcription failed: " + e.getStatusText());
        } catch (Exception e) {
            log.error("Error processing audio chunk", e);
            sendMessage(session, "error", "Transcription failed: " + e.getMessage());
        }
    }

    /**
     * Finalize transcription session.
     */
    public void finalizeSession(WebSocketSession session, StringBuilder transcript) {
        String result;
        synchronized (transcript) {
            result = transcript.toString();
        }
        if (!result.isBlank()) {
            sendMessage(session, "final", result);
        }
    }

    public void sendError(WebSocketSession session, String text) {
        sendMessage(session, "error", text);
    }

    private String transcribeChunk(String base64Audio) {
        byte[] wavBytes = decodeAudio(base64Audio);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(wavBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        });
        body.add("model", model);

        WhisperApiResponse response = restClient.post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(WhisperApiResponse.class);

        return response == null ? null : response.text();
    }

    byte[] decodeAudio(String base64Audio) {
        String payload = base64Audio.strip();
        int commaIndex = payload.indexOf(',');
        if (payload.startsWith("data:") && commaIndex > 0) {
            payload = payload.substring(commaIndex + 1);
        }
        return Base64.getMimeDecoder().decode(payload);
    }

    private void sendMessage(WebSocketSession session, String type, String text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", type, "text", text));
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
        }
    }

    private static SimpleClientHttpRequestFactory createRequestFactory(
            Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    record WhisperApiResponse(String text) {}
}
