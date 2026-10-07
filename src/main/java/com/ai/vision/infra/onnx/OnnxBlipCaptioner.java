package com.ai.vision.infra.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.ai.vision.domain.exception.VisionProviderUnavailableException;
import com.ai.vision.domain.model.CaptionResult;
import com.ai.vision.domain.repository.ImageCaptioner;
import com.ai.vision.infra.config.VisionModelProperties;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/** Image captioner running BLIP vision encoder and text decoder ONNX models. */
@Service
@DependsOn("onnxYoloDetector")
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = false)
public class OnnxBlipCaptioner implements ImageCaptioner {

  private static final long[] PIXEL_SHAPE = {1, 3, 384, 384};
  private static final long DECODER_MIN_BYTES = 600_000_000L;

  private final VisionModelProperties properties;
  private final OrtEnvironment environment;
  private final OrtSession visionSession;
  private final OrtSession decoderSession;
  private final BlipVocabulary vocabulary;
  private final boolean available;

  public OnnxBlipCaptioner(VisionModelProperties properties) {
    this.properties = properties;

    OrtSession loadedVision = null;
    OrtSession loadedDecoder = null;
    BlipVocabulary loadedVocabulary = null;
    OrtEnvironment loadedEnvironment = null;
    boolean modelAvailable = false;

    Path visionPath = Path.of(properties.getCaption().getVisionOnnx());
    Path decoderPath = Path.of(properties.getCaption().getDecoderOnnx());
    Path tokenizerPath = Path.of(properties.getCaption().getTokenizerPath());

    if (Files.exists(visionPath)
        && Files.exists(decoderPath)
        && isCompleteDecoder(decoderPath)
        && Files.isDirectory(tokenizerPath)) {
      try {
        loadedEnvironment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        loadedVision = loadedEnvironment.createSession(visionPath.toString(), options);
        loadedDecoder = loadedEnvironment.createSession(decoderPath.toString(), options);
        loadedVocabulary = BlipVocabulary.load(tokenizerPath);
        modelAvailable = true;
      } catch (IOException | OrtException ex) {
        closeQuietly(loadedVision);
        closeQuietly(loadedDecoder);
        loadedVision = null;
        loadedDecoder = null;
      } catch (UnsatisfiedLinkError | NoClassDefFoundError expected) {
      }
    }

    this.visionSession = loadedVision;
    this.decoderSession = loadedDecoder;
    this.vocabulary = loadedVocabulary;
    this.environment = loadedEnvironment;
    this.available = modelAvailable;
  }

  @Override
  public CaptionResult captionImage(BufferedImage image) {
    ensureAvailable();
    try {
      float[] pixelValues = BlipImagePreprocessor.preprocess(image);
      EncoderOutput encoderOutput = runVisionEncoder(pixelValues);
      List<Long> tokenIds = generateCaption(encoderOutput);
      String caption = vocabulary.decode(tokenIds);
      if (caption.isBlank()) {
        caption = "Unable to generate caption";
      }
      return new CaptionResult(caption);
    } catch (OrtException ex) {
      throw new VisionProviderUnavailableException(
          "caption", "Caption generation failed: " + ex.getMessage());
    }
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  /** Closes the ONNX sessions. */
  @PreDestroy
  void closeSessions() {
    closeQuietly(visionSession);
    closeQuietly(decoderSession);
  }

  private EncoderOutput runVisionEncoder(float[] pixelValues) throws OrtException {
    try (OnnxTensor pixelTensor =
            OnnxTensor.createTensor(environment, FloatBuffer.wrap(pixelValues), PIXEL_SHAPE);
        OrtSession.Result visionResult = visionSession.run(Map.of("pixel_values", pixelTensor))) {

      OnnxValue hiddenValue =
          visionResult
              .get("encoder_hidden_states")
              .orElseThrow(() -> new OrtException("encoder_hidden_states missing"));
      OnnxValue maskValue =
          visionResult
              .get("encoder_attention_mask")
              .orElseThrow(() -> new OrtException("encoder_attention_mask missing"));

      try (OnnxValue hidden = hiddenValue;
          OnnxValue mask = maskValue) {
        float[][][] encoderHiddenStates = (float[][][]) ((OnnxTensor) hidden).getValue();
        long[] encoderAttentionMask = (long[]) ((OnnxTensor) mask).getValue();
        return new EncoderOutput(encoderHiddenStates, encoderAttentionMask);
      }
    }
  }

  private List<Long> generateCaption(EncoderOutput encoderOutput) throws OrtException {
    List<Long> generated = new ArrayList<>();
    generated.add((long) vocabulary.decoderStartTokenId());

    int maxLength = properties.getCaption().getMaxLength();
    for (int step = 0; step < maxLength; step++) {
      long nextToken = predictNextToken(generated, encoderOutput);
      if (nextToken == vocabulary.eosTokenId()) {
        break;
      }
      generated.add(nextToken);
    }

    if (generated.size() > 1) {
      generated.removeFirst();
    }
    return generated;
  }

  private long predictNextToken(List<Long> generated, EncoderOutput encoderOutput)
      throws OrtException {
    long[] inputShape = {1, generated.size()};
    long[] inputIds = generated.stream().mapToLong(Long::longValue).toArray();
    long[] attentionMask = new long[generated.size()];
    Arrays.fill(attentionMask, 1L);

    try (OnnxTensor inputIdsTensor =
            OnnxTensor.createTensor(environment, LongBuffer.wrap(inputIds), inputShape);
        OnnxTensor attentionMaskTensor =
            OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), inputShape);
        OnnxTensor hiddenStatesTensor =
            OnnxTensor.createTensor(environment, encoderOutput.hiddenStates());
        OnnxTensor encoderAttentionMaskTensor =
            OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(encoderOutput.attentionMask()),
                buildAttentionMaskShape(encoderOutput))) {

      Map<String, OnnxTensor> decoderInputs = new HashMap<>();
      decoderInputs.put("input_ids", inputIdsTensor);
      decoderInputs.put("attention_mask", attentionMaskTensor);
      decoderInputs.put("encoder_hidden_states", hiddenStatesTensor);
      decoderInputs.put("encoder_attention_mask", encoderAttentionMaskTensor);

      try (OrtSession.Result decoderResult = decoderSession.run(decoderInputs)) {
        OnnxValue logitsValue =
            decoderResult
                .get("logits")
                .orElseThrow(() -> new OrtException("logits output missing"));
        try (OnnxValue logits = logitsValue) {
          float[][][] logitsArray = (float[][][]) ((OnnxTensor) logits).getValue();
          float[] lastLogits = logitsArray[0][generated.size() - 1];
          return findMaxIndex(lastLogits);
        }
      }
    }
  }

  private long findMaxIndex(float[] values) {
    long bestIndex = 0;
    float bestValue = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < values.length; i++) {
      if (values[i] > bestValue) {
        bestValue = values[i];
        bestIndex = i;
      }
    }
    return bestIndex;
  }

  private void ensureAvailable() {
    if (!available) {
      throw new VisionProviderUnavailableException(
          "caption",
          "BLIP captioner is not available. Provide ONNX models and tokenizer at "
              + properties.getCaption().getVisionOnnx());
    }
  }

  private boolean isCompleteDecoder(Path decoderPath) {
    try {
      return Files.size(decoderPath) >= DECODER_MIN_BYTES;
    } catch (IOException ex) {
      return false;
    }
  }

  private long[] buildAttentionMaskShape(EncoderOutput encoderOutput) {
    long maskLength = encoderOutput.attentionMask().length;
    if (maskLength == 1) {
      return new long[] {1};
    }
    return new long[] {1, maskLength};
  }

  private void closeQuietly(OrtSession session) {
    if (session == null) {
      return;
    }
    try {
      session.close();
    } catch (OrtException expected) {
    }
  }

  private record EncoderOutput(float[][][] hiddenStates, long[] attentionMask) {}
}
