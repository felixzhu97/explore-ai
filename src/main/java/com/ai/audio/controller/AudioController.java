package com.ai.audio.controller;

import com.ai.audio.controller.dto.TextToSpeechRequest;
import com.ai.audio.controller.dto.TtsModelsResponse;
import com.ai.audio.controller.dto.VoiceResponse;
import com.ai.audio.controller.dto.VoicesResponse;
import com.ai.audio.service.AudioService;
import com.ai.common.controller.GlobalExceptionHandler;
import com.ai.common.exception.DomainException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Audio/TTS REST Controller. */
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

  private final AudioService audioService;

  /** Get available TTS voices. */
  @GetMapping("/voices")
  public ResponseEntity<VoicesResponse> getVoices() {
    List<VoiceResponse> voices =
        audioService.getAvailableVoices().stream().map(VoiceResponse::from).toList();
    return ResponseEntity.ok(new VoicesResponse(voices));
  }

  /** Get available TTS models. */
  @GetMapping("/models")
  public ResponseEntity<TtsModelsResponse> getTtsModels() {
    return ResponseEntity.ok(new TtsModelsResponse(audioService.getAvailableTtsModels()));
  }

  /** Convert text to speech. */
  @PostMapping(value = "/speech", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> speak(@RequestBody TextToSpeechRequest request) {
    if (request.text() == null || request.text().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    try {
      var audio = audioService.synthesizeAudio(request.text(), request.voice(), request.speed());

      if (audio.isEmpty()) {
        return ResponseEntity.internalServerError().build();
      }

      String mediaType = audio.getMediaType();
      String filename = mediaType.contains("wav") ? "speech.wav" : "speech.mp3";
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(mediaType))
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .body(audio.copyBytes());
    } catch (DomainException e) {
      return ResponseEntity.status(GlobalExceptionHandler.statusOf(e.kind())).build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
