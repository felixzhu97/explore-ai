package com.ai.vision.infrastructure.config;

import com.ai.vision.domain.port.ImageCaptioner;
import com.ai.vision.domain.port.ObjectDetector;
import com.ai.vision.domain.port.OcrEngine;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class VisionHealthIndicator implements HealthIndicator {

    private final ImageCaptioner captioner;
    private final ObjectDetector detector;
    private final OcrEngine ocrEngine;

    public VisionHealthIndicator(
            ImageCaptioner captioner,
            ObjectDetector detector,
            OcrEngine ocrEngine) {
        this.captioner = captioner;
        this.detector = detector;
        this.ocrEngine = ocrEngine;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        builder.withDetail("caption", captioner.isAvailable() ? "UP" : "DOWN");
        builder.withDetail("detect", detector.isAvailable() ? "UP" : "DOWN");
        builder.withDetail("ocr", ocrEngine.isAvailable() ? "UP" : "DOWN");

        if (!captioner.isAvailable() || !detector.isAvailable() || !ocrEngine.isAvailable()) {
            builder.status("DEGRADED");
        }
        return builder.build();
    }
}
