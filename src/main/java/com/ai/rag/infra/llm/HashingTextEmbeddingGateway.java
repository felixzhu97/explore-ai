package com.ai.rag.infra.llm;

import com.ai.rag.domain.repository.TextEmbeddingGateway;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic local embedding for cloud-minimal when no remote embedding API key is set. Quality
 * is limited; set {@code app.rag.embedding.provider=openai} with {@code OPENAI_API_KEY} for real
 * vectors.
 */
@Component
@ConditionalOnProperty(name = "app.rag.embedding.provider", havingValue = "hash")
public class HashingTextEmbeddingGateway implements TextEmbeddingGateway {

  private final int dimensions;

  public HashingTextEmbeddingGateway(
      @Value("${app.rag.embedding.dimensions:1024}") int dimensions) {
    this.dimensions = dimensions;
  }

  @Override
  public float[] embedText(String text) {
    return hashToVector(text == null ? "" : text);
  }

  @Override
  public List<float[]> embedBatch(List<String> texts) {
    List<float[]> out = new ArrayList<>(texts.size());
    for (String text : texts) {
      out.add(embedText(text));
    }
    return out;
  }

  @Override
  public int getDimensions() {
    return dimensions;
  }

  private float[] hashToVector(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] seed = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      float[] vector = new float[dimensions];
      for (int i = 0; i < dimensions; i++) {
        int b = seed[i % seed.length] & 0xff;
        vector[i] = (b / 127.5f) - 1.0f;
      }
      float norm = 0f;
      for (float v : vector) {
        norm += v * v;
      }
      norm = (float) Math.sqrt(norm);
      if (norm > 0f) {
        for (int i = 0; i < vector.length; i++) {
          vector[i] /= norm;
        }
      }
      return vector;
    } catch (NoSuchAlgorithmException e) {
      return new float[dimensions];
    }
  }
}
