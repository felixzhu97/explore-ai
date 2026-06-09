package com.ai.tts.presentation.controller;

import com.ai.tts.application.service.TtsApplicationService;
import com.ai.tts.domain.AudioResult;
import com.ai.tts.domain.HealthResponse;
import com.ai.tts.domain.ProviderInfo;
import com.ai.tts.domain.Voice;
import com.ai.tts.presentation.dto.StreamRequest;
import com.ai.tts.presentation.dto.SynthesizeRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tts")
public class TtsController {

    private static final Logger log = LoggerFactory.getLogger(TtsController.class);

    private final TtsApplicationService ttsService;

    public TtsController(TtsApplicationService ttsService) {
        this.ttsService = ttsService;
    }

    @GetMapping("/")
    public Mono<ResponseEntity<Map<String, Object>>> root() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "service", "TTS Service",
            "version", "0.2.0",
            "architecture", "Clean Architecture",
            "provider", ttsService.getDefaultProvider().name(),
            "docs", "/docs",
            "health", "/tts/health",
            "voices", "/tts/voices",
            "providers", "/tts/providers"
        )));
    }

    @PostMapping("/synthesize")
    public Mono<ResponseEntity<byte[]>> synthesize(@Valid @RequestBody SynthesizeRequest request) {
        log.info("Received synthesize request: text length={}", request.text().length());

        return ttsService.synthesize(
                request.text(),
                request.voice(),
                request.language(),
                request.speed(),
                request.pitch(),
                request.outputFormat(),
                request.provider()
            )
            .map(AudioResult::audioData)
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
            .doOnSuccess(result -> log.info("Synthesize completed: size={} bytes", result.getBody().length))
            .doOnError(e -> log.error("Synthesize failed: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Error in synthesize: {}", e.getMessage(), e);
                return Mono.just(ResponseEntity.internalServerError()
                        .body(new byte[0]));
            });
    }

    @PostMapping("/stream")
    public Mono<ResponseEntity<Flux<byte[]>>> stream(@Valid @RequestBody StreamRequest request) {
        log.info("Received stream request: text length={}", request.text().length());

        Flux<byte[]> audioStream = ttsService.stream(
                request.text(),
                request.voice(),
                request.language(),
                request.speed(),
                request.outputFormat(),
                request.provider()
            );

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
        String providerName = provider != null ? provider : ttsService.getDefaultProvider().name();
        HealthResponse response = healthy
                ? HealthResponse.healthy(providerName)
                : HealthResponse.unhealthy(providerName, "Provider unavailable");
        return ResponseEntity.ok(response);
    }
}
