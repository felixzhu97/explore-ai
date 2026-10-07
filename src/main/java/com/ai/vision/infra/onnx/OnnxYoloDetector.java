package com.ai.vision.infra.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.ai.common.exception.DomainException;
import com.ai.vision.domain.model.Detection;
import com.ai.vision.domain.repository.ObjectDetector;
import com.ai.vision.infra.config.VisionModelProperties;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Object detector running a YOLOv8 ONNX model through ONNX Runtime. */
@Service
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = false)
public class OnnxYoloDetector implements ObjectDetector {

  private final VisionModelProperties properties;
  private final OrtEnvironment environment;
  private final OrtSession session;
  private final boolean available;

  public OnnxYoloDetector(VisionModelProperties properties) {
    this.properties = properties;
    OrtEnvironment loadedEnvironment = null;
    OrtSession loadedSession = null;
    boolean modelAvailable = false;
    Path modelPath = Path.of(properties.getDetect().getOnnxPath());

    try {
      loadedEnvironment = OrtEnvironment.getEnvironment();
      if (Files.exists(modelPath)) {
        loadedSession =
            loadedEnvironment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        modelAvailable = true;
      }
    } catch (OrtException expected) {
    } catch (UnsatisfiedLinkError | NoClassDefFoundError expected) {
    }

    this.environment = loadedEnvironment;
    this.session = loadedSession;
    this.available = modelAvailable;
  }

  @Override
  public List<Detection> detectObjects(BufferedImage image) {
    ensureAvailable();
    int inputSize = properties.getDetect().getInputSize();
    float[] input = YoloImagePreprocessor.preprocessImage(image, inputSize);
    long[] inputShape = {1, 3, inputSize, inputSize};

    try (OnnxTensor inputTensor =
            OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), inputShape);
        OrtSession.Result result = session.run(Map.of("images", inputTensor))) {

      OnnxValue outputValue =
          result
              .get("predictions")
              .orElseThrow(() -> new OrtException("predictions output missing"));
      try (OnnxValue output = outputValue) {
        float[][][] predictions = (float[][][]) ((OnnxTensor) output).getValue();
        return parseDetections(predictions[0], image.getWidth(), image.getHeight(), inputSize);
      }
    } catch (OrtException ex) {
      throw DomainException.createUnavailableError(
          "VISION_PROVIDER_UNAVAILABLE", "Object detection failed: " + ex.getMessage());
    }
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  /** Closes the ONNX session. */
  @PreDestroy
  void closeSession() {
    if (session == null) {
      return;
    }
    try {
      session.close();
    } catch (OrtException expected) {
    }
  }

  private List<Detection> parseDetections(
      float[][] output, int sourceWidth, int sourceHeight, int inputSize) {
    int channels = output.length;
    int numPredictions = output[0].length;
    boolean channelsFirst = channels < numPredictions;
    if (!channelsFirst) {
      throw DomainException.createUnavailableError(
          "VISION_PROVIDER_UNAVAILABLE", "Unexpected YOLO output shape");
    }

    float confidenceThreshold = properties.getDetect().getConfidenceThreshold();
    float nmsThreshold = properties.getDetect().getNmsThreshold();
    int classCount = CocoClassNames.countClasses();
    float scaleX = (float) sourceWidth / inputSize;
    float scaleY = (float) sourceHeight / inputSize;

    List<Detection> candidates = new ArrayList<>();
    for (int i = 0; i < numPredictions; i++) {
      float cx = output[0][i];
      float cy = output[1][i];
      float w = output[2][i];
      float h = output[3][i];

      int bestClass = -1;
      float bestScore = 0f;
      for (int c = 0; c < classCount; c++) {
        float score = output[4 + c][i];
        if (score > bestScore) {
          bestScore = score;
          bestClass = c;
        }
      }

      if (bestScore < confidenceThreshold || bestClass < 0) {
        continue;
      }

      float x = (cx - w / 2f) * scaleX;
      float y = (cy - h / 2f) * scaleY;
      float width = w * scaleX;
      float height = h * scaleY;

      candidates.add(
          new Detection(
              CocoClassNames.getLabel(bestClass),
              bestScore,
              clampCoordinate(x, sourceWidth),
              clampCoordinate(y, sourceHeight),
              clampCoordinate(width, sourceWidth),
              clampCoordinate(height, sourceHeight)));
    }

    return YoloNonMaxSuppression.applySuppression(candidates, nmsThreshold);
  }

  private void ensureAvailable() {
    if (!available) {
      throw DomainException.createUnavailableError(
          "VISION_PROVIDER_UNAVAILABLE",
          "YOLOv8 detector is not available. Provide ONNX model at "
              + properties.getDetect().getOnnxPath());
    }
  }

  private float clampCoordinate(float value, int max) {
    return Math.max(0f, Math.min(value, max));
  }
}
