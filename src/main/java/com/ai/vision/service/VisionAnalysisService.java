package com.ai.vision.service;

import com.ai.common.controller.dto.HealthStatus;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.service.AiInvocationRecorder;
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

  private final ImageCaptioner captioner;
  private final ObjectDetector detector;
  private final OcrEngine ocrEngine;
  private final Timer captionTimer;
  private final Timer detectTimer;
  private final Timer ocrTimer;
  private final AiInvocationRecorder invocationRecorder;

  public VisionAnalysisService(
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
      var result = captioner.captionImage(image);
      long processingTimeMs = Latency.measureSince(startedAt).getMillis();
      captionTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
      invocationRecorder.recordSuccess(
          AiCapability.VISION,
          "vision.caption",
          Latency.createFromMillis(processingTimeMs),
          owner,
          null,
          null,
          null);
      return new CaptionResponse(result.getText(), processingTimeMs);
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiCapability.VISION,
          "vision.caption",
          Latency.measureSince(startedAt),
          owner,
          null,
          null,
          null,
          ex);
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
      long processingTimeMs = Latency.measureSince(startedAt).getMillis();
      detectTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
      invocationRecorder.recordSuccess(
          AiCapability.VISION,
          "vision.detect",
          Latency.createFromMillis(processingTimeMs),
          owner,
          null,
          null,
          null);
      return new DetectResponse(detections, processingTimeMs);
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiCapability.VISION,
          "vision.detect",
          Latency.measureSince(startedAt),
          owner,
          null,
          null,
          null,
          ex);
      throw ex;
    }
  }

  /** Extracts text from the uploaded image, recording latency and the invocation outcome. */
  public OcrResponse recognizeText(MultipartFile file, OwnerKey owner) throws IOException {
    BufferedImage image = toImage(file);
    long startedAt = System.nanoTime();
    try {
      var result = ocrEngine.extractText(image);
      long processingTimeMs = Latency.measureSince(startedAt).getMillis();
      ocrTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
      invocationRecorder.recordSuccess(
          AiCapability.VISION,
          "vision.ocr",
          Latency.createFromMillis(processingTimeMs),
          owner,
          null,
          null,
          null);
      return new OcrResponse(result.getText(), processingTimeMs);
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiCapability.VISION,
          "vision.ocr",
          Latency.measureSince(startedAt),
          owner,
          null,
          null,
          null,
          ex);
      throw ex;
    }
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
