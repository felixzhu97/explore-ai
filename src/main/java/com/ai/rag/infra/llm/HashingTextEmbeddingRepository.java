package com.ai.rag.infra.llm;

import com.ai.rag.domain.repository.TextEmbeddingRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Deterministic local embedding used when no remote EmbeddingModel is configured (e.g. missing
 * OPENAI_API_KEY on Render). Quality is poor; keeps RAG wiring bootable.
 */
@Component
@ConditionalOnMissingBean(TextEmbeddingRepository.class)
public class HashingTextEmbeddingRepository implements TextEmbeddingRepository {

  private static final Logger log = LoggerFactory.getLogger(HashingTextEmbeddingRepository.class);

  private final int dimensions;

  /** Documentation. */
  public HashingTextEmbeddingRepository(
      @Value("${app.rag.embedding.dimensions:1024}") int dimensions) {
    this.dimensions = dimensions;
    log.warn(
        "Using HashingTextEmbeddingRepository (dimensions={}); configure"
            + " app.rag.embedding.provider=openai with OPENAI_API_KEY for real embeddings",
        dimensions);
  }

  @Override
  public float[] embed(String text) {
    return hashToVector(text == null ? "" : text);
  }

  @Override
  public List<float[]> embedBatch(List<String> texts) {
    List<float[]> out = new ArrayList<>(texts.size());
    for (String text : texts) {
      out.add(embed(text));
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
