package com.ai.audio.web;

import com.ai.audio.application.usecase.AudioFacade;
import com.ai.audio.domain.exception.InvalidSpeechTextException;
import com.ai.audio.domain.exception.TtsProviderNotConfiguredException;
import com.ai.audio.web.dto.VoiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Audio/TTS REST Controller.
 */
@RestController
@RequestMapping("/api")
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    private final AudioFacade audioFacade;

    public AudioController(AudioFacade audioFacade) {
        this.audioFacade = audioFacade;
    }

    /**
     * Convert text to speech.
     */
    @PostMapping(value = {"/audio/speak", "/tts/synthesize"}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> speak(@RequestBody TtsRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] audio = audioFacade.synthesize(request.text(), request.voice(), request.speed());

            if (audio == null || audio.length == 0) {
                return ResponseEntity.internalServerError().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"")
                    .body(audio);
        } catch (InvalidSpeechTextException e) {
            return ResponseEntity.badRequest().build();
        } catch (TtsProviderNotConfiguredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Error synthesizing speech", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available TTS voices.
     */
    @GetMapping({"/audio/voices", "/tts/voices"})
    public ResponseEntity<Map<String, Object>> getVoices() {
        List<VoiceResponse> voices =
                audioFacade.getAvailableVoices().stream().map(VoiceResponse::from).toList();
        return ResponseEntity.ok(Map.of("voices", voices));
    }

    /**
     * Get available TTS models.
     */
    @GetMapping("/audio/models")
    public ResponseEntity<Map<String, Object>> getTtsModels() {
        return ResponseEntity.ok(Map.of("models", audioFacade.getAvailableTtsModels()));
    }

    public record TtsRequest(String text, String voice, Double speed, String output_format) {}
}
