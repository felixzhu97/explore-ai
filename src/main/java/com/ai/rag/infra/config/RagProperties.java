package com.ai.rag.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG (Retrieval-Augmented Generation) configuration properties. Binds configuration from
 * application.yml under 'app.rag' prefix.
 */
@Component
@ConfigurationProperties(prefix = "app.rag")
@Getter
@Setter
public class RagProperties {

  private Chunk chunk = new Chunk();
  private Retrieval retrieval = new Retrieval();

  /** Document chunking settings: token chunk size. */
  @Getter
  @Setter
  public static class Chunk {
    /**
     * Target chunk size in tokens ({@link
     * org.springframework.ai.transformer.splitter.TokenTextSplitter}).
     */
    private int size = 500;
  }

  /** Retrieval settings: number of chunks to return and minimum similarity score. */
  @Getter
  @Setter
  public static class Retrieval {
    private int topK = 5;
    private double scoreThreshold = 0.5;
  }
}
