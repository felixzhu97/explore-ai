package com.ai.vision.infrastructure.adapter;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.ai.vision.domain.exception.VisionProviderUnavailableException;
import com.ai.vision.domain.model.Detection;
import com.ai.vision.domain.repository.ObjectDetector;
import com.ai.vision.infrastructure.config.VisionModelProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.vision", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OnnxYoloDetector implements ObjectDetector {

    private static final Logger log = LoggerFactory.getLogger(OnnxYoloDetector.class);

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
                loadedSession = loadedEnvironment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
                modelAvailable = true;
                log.info("YOLOv8 detector loaded from {}", modelPath.toAbsolutePath());
            } else {
                log.warn("YOLOv8 model not found at {}", modelPath.toAbsolutePath());
            }
        } catch (OrtException ex) {
            log.warn("Failed to load YOLOv8 model at {}: {}", modelPath, ex.getMessage());
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            log.warn("ONNX Runtime native library unavailable: {}", ex.getMessage());
        }

        this.environment = loadedEnvironment;
        this.session = loadedSession;
        this.available = modelAvailable;
    }

    @Override
    public List<Detection> detect(BufferedImage image) {
        ensureAvailable();
        int inputSize = properties.getDetect().getInputSize();
        float[] input = YoloImagePreprocessor.preprocess(image, inputSize);
        long[] inputShape = {1, 3, inputSize, inputSize};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), inputShape);
                OrtSession.Result result = session.run(Map.of("images", inputTensor))) {

            OnnxValue outputValue = result.get("predictions")
                    .orElseThrow(() -> new OrtException("predictions output missing"));
            try (OnnxValue output = outputValue) {
                float[][][] predictions = (float[][][]) ((OnnxTensor) output).getValue();
                return parseDetections(predictions[0], image.getWidth(), image.getHeight(), inputSize);
            }
        } catch (OrtException ex) {
            throw new VisionProviderUnavailableException("detect", "Object detection failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @PreDestroy
    void closeSession() {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (OrtException ex) {
            log.debug("Failed to close YOLO session: {}", ex.getMessage());
        }
    }

    private List<Detection> parseDetections(float[][] output, int sourceWidth, int sourceHeight, int inputSize) {
        int channels = output.length;
        int numPredictions = output[0].length;
        boolean channelsFirst = channels < numPredictions;
        if (!channelsFirst) {
            throw new VisionProviderUnavailableException("detect", "Unexpected YOLO output shape");
        }

        float confidenceThreshold = properties.getDetect().getConfidenceThreshold();
        float nmsThreshold = properties.getDetect().getNmsThreshold();
        int classCount = CocoClassNames.classCount();
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

            candidates.add(new Detection(
                    CocoClassNames.label(bestClass),
                    bestScore,
                    clamp(x, sourceWidth),
                    clamp(y, sourceHeight),
                    clamp(width, sourceWidth),
                    clamp(height, sourceHeight)));
        }

        return YoloNonMaxSuppression.apply(candidates, nmsThreshold);
    }

    private void ensureAvailable() {
        if (!available) {
            throw new VisionProviderUnavailableException(
                    "detect",
                    "YOLOv8 detector is not available. Provide ONNX model at "
                            + properties.getDetect().getOnnxPath());
        }
    }

    private float clamp(float value, int max) {
        return Math.max(0f, Math.min(value, max));
    }
}
