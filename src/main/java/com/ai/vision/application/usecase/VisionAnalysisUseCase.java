package com.ai.vision.application.usecase;

import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.domain.model.Detection;
import com.ai.vision.domain.port.ImageCaptioner;
import com.ai.vision.domain.port.ObjectDetector;
import com.ai.vision.domain.port.OcrEngine;
import com.ai.vision.web.dto.CaptionResponse;
import com.ai.vision.web.dto.DetectResponse;
import com.ai.vision.web.dto.DetectionDto;
import com.ai.vision.web.dto.OcrResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VisionAnalysisUseCase {

    private final ImageCaptioner captioner;
    private final ObjectDetector detector;
    private final OcrEngine ocrEngine;
    private final Timer captionTimer;
    private final Timer detectTimer;
    private final Timer ocrTimer;

    public VisionAnalysisUseCase(
            ImageCaptioner captioner,
            ObjectDetector detector,
            OcrEngine ocrEngine,
            MeterRegistry meterRegistry) {
        this.captioner = captioner;
        this.detector = detector;
        this.ocrEngine = ocrEngine;
        this.captionTimer = meterRegistry.timer("vision.caption.duration");
        this.detectTimer = meterRegistry.timer("vision.detect.duration");
        this.ocrTimer = meterRegistry.timer("vision.ocr.duration");
    }

    public CaptionResponse caption(MultipartFile file) throws IOException {
        BufferedImage image = toImage(file);
        long startedAt = System.nanoTime();
        var result = captioner.caption(image);
        long processingTimeMs = elapsedMillis(startedAt);
        captionTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
        return new CaptionResponse(result.text(), processingTimeMs);
    }

    public OcrResponse ocr(MultipartFile file) throws IOException {
        BufferedImage image = toImage(file);
        long startedAt = System.nanoTime();
        var result = ocrEngine.extract(image);
        long processingTimeMs = elapsedMillis(startedAt);
        ocrTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
        return new OcrResponse(result.text(), processingTimeMs);
    }

    public DetectResponse detect(MultipartFile file) throws IOException {
        BufferedImage image = toImage(file);
        long startedAt = System.nanoTime();
        List<DetectionDto> detections = detector.detect(image).stream()
                .map(this::toDto)
                .toList();
        long processingTimeMs = elapsedMillis(startedAt);
        detectTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
        return new DetectResponse(detections, processingTimeMs);
    }

    private DetectionDto toDto(Detection detection) {
        return new DetectionDto(
                detection.className(),
                detection.confidence(),
                List.of(detection.x(), detection.y(), detection.width(), detection.height()));
    }

    private BufferedImage toImage(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new VisionInvalidFileException("Unsupported or corrupt image file");
        }
        return image;
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
