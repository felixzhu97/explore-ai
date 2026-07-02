package com.ai.audio.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Adapter for Ollama Whisper streaming transcription.
 * Calls Ollama /api/chat endpoint with audio in images field.
 */
@Component
public class OllamaWhisperAdapter {

    private static final Logger log = LoggerFactory.getLogger(OllamaWhisperAdapter.class);

    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaWhisperAdapter(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.asr.model:whisper-base}") String model,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Transcribe audio chunk and send partial results via WebSocket.
     */
    public void streamAudioChunk(WebSocketSession session, StringBuilder transcript, String payload) {
        try {
            Map<String, String> message = objectMapper.readValue(payload, Map.class);
            String audioData = message.get("data");

            if (audioData == null || audioData.isBlank()) {
                return;
            }

            String partial = transcribeStream(audioData);
            if (partial != null && !partial.isBlank()) {
                transcript.append(partial);
                sendMessage(session, "partial", partial);
            }
        } catch (Exception e) {
            log.error("Error processing audio chunk", e);
        }
    }

    /**
     * Finalize transcription session.
     */
    public void finalizeSession(WebSocketSession session, StringBuilder transcript) {
        String result = transcript.toString();
        if (!result.isBlank()) {
            sendMessage(session, "final", result);
        }
    }

    /**
     * Stream transcription from Ollama Whisper.
     * Audio must be base64-encoded WAV (16kHz mono).
     */
    private String transcribeStream(String base64Audio) {
        try {
            String chatApiUrl = baseUrl + "/api/chat";

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "stream", true,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", "transcribe",
                            "images", List.of(base64Audio)
                    ))
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatApiUrl))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            StringBuilder fullResponse = new StringBuilder();

            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() != 200) {
                log.warn("Ollama returned status: {}", response.statusCode());
                return null;
            }

            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonNode node = objectMapper.readTree(line);
                    if (node.has("message") && node.get("message").has("content")) {
                        String content = node.get("message").get("content").asText();
                        fullResponse.append(content);
                    }
                }
            }

            return fullResponse.toString();
        } catch (Exception e) {
            log.error("Error calling Ollama Whisper", e);
            return null;
        }
    }

    private void sendMessage(WebSocketSession session, String type, String text) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", type, "text", text));
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
        }
    }
}
