package com.ai.vision.application;

import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.metrics.domain.AiDomain;
import com.ai.vision.domain.VisionInvalidFileException;
import com.ai.vision.domain.Detection;
import com.ai.vision.domain.ImageCaptioner;
import com.ai.vision.domain.ObjectDetector;
import com.ai.vision.domain.OcrEngine;
import com.ai.vision.web.CaptionResponse;
import com.ai.vision.web.DetectResponse;
import com.ai.vision.web.DetectionDto;
import com.ai.vision.web.OcrResponse;
import com.ai.vision.web.VisionHealthResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "launchdarkly.bootstrap", name = "module-vision", havingValue = "true", matchIfMissing = true)
public class VisionAnalysisUseCase {

    private final ImageCaptioner captioner;
    private final ObjectDetector detector;
    private final OcrEngine ocrEngine;
    private final Timer captionTimer;
    private final Timer detectTimer;
    private final Timer ocrTimer;
    private final AiInvocationRecorder invocationRecorder;

    public VisionAnalysisUseCase(
            ImageCaptioner captioner,
            ObjectDetector detector,
            OcrEngine ocrEngine,
            MeterRegistry meterRegistry,
            AiInvocationRecorder invocationRecorder) {
        this.captioner = captioner;
        this.detector = detector;
        this.ocrEngine = ocrEngine;
        this.captionTimer = meterRegistry.timer("vision.caption.duration");
        this.detectTimer = meterRegistry.timer("vision.detect.duration");
        this.ocrTimer = meterRegistry.timer("vision.ocr.duration");
        this.invocationRecorder = invocationRecorder;
    }

    public CaptionResponse caption(MultipartFile file) throws IOException {
        BufferedImage image = toImage(file);
        long startedAt = System.nanoTime();
        try {
            var result = captioner.caption(image);
            long processingTimeMs = elapsedMillis(startedAt);
            captionTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
            invocationRecorder.recordSuccess(
                    AiDomain.VISION, "vision.caption", processingTimeMs, null, null, null);
            return new CaptionResponse(result.text(), processingTimeMs);
        } catch (RuntimeException ex) {
            invocationRecorder.recordError(
                    AiDomain.VISION, "vision.caption", elapsedMillis(startedAt),
                    null, null, null, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    public OcrResponse ocr(MultipartFile file) throws IOException {
        BufferedImage image = toImage(file);
        long startedAt = System.nanoTime();
        try {
            var result = ocrEngine.extract(image);
            long processingTimeMs = elapsedMillis(startedAt);
            ocrTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
            invocationRecorder.recordSuccess(
                    AiDomain.VISION, "vision.ocr", processingTimeMs, null, null, null);
            return new OcrResponse(result.text(), processingTimeMs);
        } catch (RuntimeException ex) {
            invocationRecorder.recordError(
                    AiDomain.VISION, "vision.ocr", elapsedMillis(startedAt),
                    null, null, null, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    public DetectResponse detect(MultipartFile file) throws IOException {
        BufferedImage image = toImage(file);
        long startedAt = System.nanoTime();
        try {
            List<DetectionDto> detections = detector.detect(image).stream()
                    .map(this::toDto)
                    .toList();
            long processingTimeMs = elapsedMillis(startedAt);
            detectTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
            invocationRecorder.recordSuccess(
                    AiDomain.VISION, "vision.detect", processingTimeMs, null, null, null);
            return new DetectResponse(detections, processingTimeMs);
        } catch (RuntimeException ex) {
            invocationRecorder.recordError(
                    AiDomain.VISION, "vision.detect", elapsedMillis(startedAt),
                    null, null, null, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    public VisionHealthResponse health() {
        Map<String, String> providers = new LinkedHashMap<>();
        providers.put("caption", captioner.isAvailable() ? "UP" : "DOWN");
        providers.put("detect", detector.isAvailable() ? "UP" : "DOWN");
        providers.put("ocr", ocrEngine.isAvailable() ? "UP" : "DOWN");
        boolean allUp = captioner.isAvailable() && detector.isAvailable() && ocrEngine.isAvailable();
        return new VisionHealthResponse(allUp ? "UP" : "DEGRADED", providers);
    }

    private DetectionDto toDto(Detection detection) {
        return new DetectionDto(
                detection.className(),
                detection.confidence(),
                List.of(detection.x(), detection.y(), detection.width(), detection.height()));
    }

    private BufferedImage toImage(MultipartFile file) throws IOException {
        try (var inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new VisionInvalidFileException("Unsupported or corrupt image file");
            }
            return image;
        }
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
