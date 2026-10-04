package com.ai.rag.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG (Retrieval-Augmented Generation) configuration properties. Binds configuration from
 * application.yml under 'app.rag' prefix.
 */
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

  private Chunk chunk = new Chunk();
  private Retrieval retrieval = new Retrieval();

  public Chunk getChunk() {
    return chunk;
  }

  public void setChunk(Chunk chunk) {
    this.chunk = chunk;
  }

  public Retrieval getRetrieval() {
    return retrieval;
  }

  public void setRetrieval(Retrieval retrieval) {
    this.retrieval = retrieval;
  }

  /** Document chunking settings: token chunk size. */
  public static class Chunk {
    /**
     * Target chunk size in tokens ({@link
     * org.springframework.ai.transformer.splitter.TokenTextSplitter}).
     */
    private int size = 500;

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }
  }

  /** Retrieval settings: number of chunks to return and minimum similarity score. */
  public static class Retrieval {
    private int topK = 5;
    private double scoreThreshold = 0.5;

    public int getTopK() {
      return topK;
    }

    public void setTopK(int topK) {
      this.topK = topK;
    }

    public double getScoreThreshold() {
      return scoreThreshold;
    }

    public void setScoreThreshold(double scoreThreshold) {
      this.scoreThreshold = scoreThreshold;
    }
  }
}
