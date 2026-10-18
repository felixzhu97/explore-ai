package com.ai.vision.service;

import com.ai.common.controller.dto.HealthStatus;
import com.ai.common.domain.event.AiInvocationCompletedEvent;
import com.ai.common.domain.event.AiInvocationFailedEvent;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.vision.controller.dto.CaptionResponse;
import com.ai.vision.controller.dto.DetectResponse;
import com.ai.vision.controller.dto.DetectionResponse;
import com.ai.vision.controller.dto.OcrResponse;
import com.ai.vision.controller.dto.VisionHealthResponse;
import com.ai.vision.controller.dto.VisionProvidersResponse;
import com.ai.vision.domain.model.Detection;
import com.ai.vision.domain.repository.ImageCaptioner;
import com.ai.vision.domain.repository.ObjectDetector;
import com.ai.vision.domain.repository.OcrEngine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Runs captioning, OCR, and object detection on uploaded images with timing metrics. */
@Service
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = false)
public class VisionAnalysisService {

  private static final String VISION = "vision";
  private static final long NANOS_PER_MILLI = 1_000_000L;

  private final ImageCaptioner captioner;
  private final ObjectDetector detector;
  private final OcrEngine ocrEngine;
  private final Timer captionTimer;
  private final Timer detectTimer;
  private final Timer ocrTimer;
  private final ApplicationEventPublisher eventPublisher;

  public VisionAnalysisService(
      ImageCaptioner captioner,
      ObjectDetector detector,
      OcrEngine ocrEngine,
      MeterRegistry meterRegistry,
      ApplicationEventPublisher eventPublisher) {
    this.captioner = captioner;
    this.detector = detector;
    this.ocrEngine = ocrEngine;
    this.captionTimer = meterRegistry.timer("vision.caption.duration");
    this.detectTimer = meterRegistry.timer("vision.detect.duration");
    this.ocrTimer = meterRegistry.timer("vision.ocr.duration");
    this.eventPublisher = eventPublisher;
  }

  /** Reports each vision provider's status, overall UP only when all three are available. */
  public VisionHealthResponse getHealth() {
    VisionProvidersResponse providers =
        new VisionProvidersResponse(
            HealthStatus.createStatus(captioner.isAvailable()),
            HealthStatus.createStatus(detector.isAvailable()),
            HealthStatus.createStatus(ocrEngine.isAvailable()));
    boolean allUp = captioner.isAvailable() && detector.isAvailable() && ocrEngine.isAvailable();
    return new VisionHealthResponse(allUp ? HealthStatus.UP : HealthStatus.DEGRADED, providers);
  }

  /** Captions the uploaded image, recording latency and the invocation outcome. */
  public CaptionResponse captionImage(MultipartFile file, OwnerKey owner) throws IOException {
    BufferedImage image = toImage(file);
    long startedAt = System.nanoTime();
    try {
      var caption = captioner.captionImage(image);
      long processingTimeMs = elapsedMillis(startedAt);
      captionTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
      publishCompleted("vision.caption", processingTimeMs, owner);
      return new CaptionResponse(caption.getText(), processingTimeMs);
    } catch (RuntimeException ex) {
      publishFailed("vision.caption", elapsedMillis(startedAt), owner, ex);
      throw ex;
    }
  }

  /** Detects objects in the uploaded image, recording latency and the invocation outcome. */
  public DetectResponse detectObjects(MultipartFile file, OwnerKey owner) throws IOException {
    BufferedImage image = toImage(file);
    long startedAt = System.nanoTime();
    try {
      List<DetectionResponse> detections =
          detector.detectObjects(image).stream().map(this::toDto).toList();
      long processingTimeMs = elapsedMillis(startedAt);
      detectTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
      publishCompleted("vision.detect", processingTimeMs, owner);
      return new DetectResponse(detections, processingTimeMs);
    } catch (RuntimeException ex) {
      publishFailed("vision.detect", elapsedMillis(startedAt), owner, ex);
      throw ex;
    }
  }

  /** Extracts text from the uploaded image, recording latency and the invocation outcome. */
  public OcrResponse recognizeText(MultipartFile file, OwnerKey owner) throws IOException {
    BufferedImage image = toImage(file);
    long startedAt = System.nanoTime();
    try {
      var extraction = ocrEngine.extractText(image);
      long processingTimeMs = elapsedMillis(startedAt);
      ocrTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
      publishCompleted("vision.ocr", processingTimeMs, owner);
      return new OcrResponse(extraction.getText(), processingTimeMs);
    } catch (RuntimeException ex) {
      publishFailed("vision.ocr", elapsedMillis(startedAt), owner, ex);
      throw ex;
    }
  }

  private void publishCompleted(String operation, long latencyMs, OwnerKey owner) {
    eventPublisher.publishEvent(
        new AiInvocationCompletedEvent(
            VISION, operation, latencyMs, owner, null, null, null, null, null));
  }

  private void publishFailed(String operation, long latencyMs, OwnerKey owner, Throwable error) {
    eventPublisher.publishEvent(
        new AiInvocationFailedEvent(
            VISION,
            operation,
            latencyMs,
            owner,
            null,
            null,
            null,
            null,
            null,
            error.getClass().getSimpleName(),
            error.getMessage()));
  }

  private static long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / NANOS_PER_MILLI;
  }

  private DetectionResponse toDto(Detection detection) {
    return new DetectionResponse(
        detection.getClassName(),
        detection.getConfidence(),
        List.of(
            detection.getLeft(), detection.getTop(), detection.getWidth(), detection.getHeight()));
  }

  private BufferedImage toImage(MultipartFile file) throws IOException {
    try (var inputStream = file.getInputStream()) {
      BufferedImage image = ImageIO.read(inputStream);
      if (image == null) {
        throw DomainException.createInvalidError(
            "INVALID_FILE", "Unsupported or corrupt image file");
      }
      return image;
    }
  }
}
