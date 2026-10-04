package com.ai.rag.infra.config;

import com.ai.rag.domain.repository.RagRetrievalSettings;
import org.springframework.stereotype.Component;

/** Exposes {@code app.rag.retrieval} top-K and score threshold as retrieval settings. */
@Component
public class PropertiesRagRetrievalSettings implements RagRetrievalSettings {

  private final RagProperties ragProperties;

  public PropertiesRagRetrievalSettings(RagProperties ragProperties) {
    this.ragProperties = ragProperties;
  }

  @Override
  public int getTopK() {
    return ragProperties.getRetrieval().getTopK();
  }

  @Override
  public double getScoreThreshold() {
    return ragProperties.getRetrieval().getScoreThreshold();
  }
}
