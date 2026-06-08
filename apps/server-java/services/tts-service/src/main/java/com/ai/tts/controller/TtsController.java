package com.ai.tts.controller;

import com.ai.tts.domain.model.HealthResponse;
import com.ai.tts.domain.model.OutputFormat;
import com.ai.tts.domain.model.ProviderInfo;
import com.ai.tts.domain.model.Voice;
import com.ai.tts.dto.ErrorResponse;
import com.ai.tts.dto.SynthesizeRequest;
import com.ai.tts.dto.StreamRequest;
import com.ai.tts.service.TtsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping("/synthesize")
    public Mono<ResponseEntity<byte[]>> synthesize(@Valid @RequestBody SynthesizeRequest request) {
        return ttsService.synthesize(
                request.text(),
                request.voice(),
                request.language(),
                request.speed(),
                request.pitch(),
                request.outputFormat(),
                request.provider()
            )
            .map(audio -> {
                String extension = request.outputFormat().extension();
                String filename = "speech_" + System.currentTimeMillis() + "." + extension;
                String mediaType = request.outputFormat().mediaType();

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, mediaType)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(audio.length))
                        .body(audio);
            })
            .onErrorResume(e -> {
                return Mono.just(ResponseEntity.internalServerError()
                        .body(new byte[0]));
            });
    }

    @PostMapping("/stream")
    public Mono<ResponseEntity<Flux<byte[]>>> stream(@Valid @RequestBody StreamRequest request) {
        Flux<byte[]> audioStream = ttsService.stream(
                request.text(),
                request.voice(),
                request.language(),
                request.speed(),
                request.outputFormat(),
                request.provider()
            )
            .flatMap(audio -> Flux.just(audio));

        String mediaType = request.outputFormat().mediaType();

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(audioStream));
    }

    @GetMapping("/voices")
    public ResponseEntity<List<Voice>> listVoices(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String provider) {
        List<Voice> voices = ttsService.listVoices(language, provider);
        return ResponseEntity.ok(voices);
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderInfo>> listProviders() {
        List<ProviderInfo> providers = ttsService.listAllProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/provider")
    public ResponseEntity<ProviderInfo> getCurrentProvider(
            @RequestParam(required = false) String provider) {
        ProviderInfo info = ttsService.getCurrentProviderInfo(provider);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health(
            @RequestParam(required = false) String provider) {
        boolean healthy = ttsService.healthCheck(provider);
        String providerName = provider != null ? provider : "edge";
        HealthResponse response = healthy
                ? HealthResponse.healthy(providerName)
                : HealthResponse.unhealthy(providerName, "Provider unavailable");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleError(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("SYNTHESIS_ERROR", e.getMessage()));
    }
}
