package com.ai.media.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller for media module.
 */
@RestController
@RequestMapping("/api/image")
public class MediaHealthController {

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "media-agent",
                "module", "media",
                "timestamp", Instant.now().toString()
        )));
    }
}
