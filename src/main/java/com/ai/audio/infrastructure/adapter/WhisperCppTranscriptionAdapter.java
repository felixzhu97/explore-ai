package com.ai.audio.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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
            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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

    private String transcribeChunk(String base64Audio) {
        byte[] wavBytes = Base64.getDecoder().decode(base64Audio);
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

    private void sendMessage(WebSocketSession session, String type, String text) {
        synchronized (session) {
            try {
                String json = objectMapper.writeValueAsString(Map.of("type", type, "text", text));
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Error sending WebSocket message", e);
            }
        }
    }

    record WhisperApiResponse(String text) {}
}
