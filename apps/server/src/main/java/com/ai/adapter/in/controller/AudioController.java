package com.ai.adapter.in.controller;

import com.ai.domain.service.TextToSpeechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * REST Controller for Text-to-Speech operations.
 */
@RestController
@RequestMapping("/api/audio")
@Tag(name = "Audio/TTS", description = "Text-to-Speech synthesis")
public class AudioController {

    private final TextToSpeechService textToSpeechService;

    public AudioController(TextToSpeechService textToSpeechService) {
        this.textToSpeechService = textToSpeechService;
    }

    @PostMapping(value = "/speak", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Convert text to speech and return audio file")
    public ResponseEntity<byte[]> speak(@RequestBody TtsRequest request) {
        try {
            byte[] audio = textToSpeechService.synthesize(request.text());

            if (audio == null || audio.length == 0) {
                return ResponseEntity.internalServerError().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"")
                    .body(audio);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Stream text to speech in real-time")
    public ResponseEntity<Flux<byte[]>> stream(@RequestParam String text) {
        try {
            Flux<byte[]> audioStream = textToSpeechService.stream(text);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                    .body(audioStream);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/voices")
    @Operation(summary = "Get available voices for TTS")
    public ResponseEntity<Map<String, Object>> getVoices() {
        return ResponseEntity.ok(Map.of(
                "voices", textToSpeechService.getAvailableVoices()
        ));
    }

    @GetMapping("/models")
    @Operation(summary = "Get available TTS models")
    public ResponseEntity<Map<String, Object>> getModels() {
        return ResponseEntity.ok(Map.of(
                "models", textToSpeechService.getAvailableModels()
        ));
    }

    public record TtsRequest(String text) {
    }
}
