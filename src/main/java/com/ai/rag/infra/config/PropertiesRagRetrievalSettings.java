package com.ai.rag.infra.config;

import com.ai.rag.domain.repository.RagRetrievalSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes {@code app.rag.retrieval} top-K and score threshold as retrieval settings. */
@Component
@RequiredArgsConstructor
public class PropertiesRagRetrievalSettings implements RagRetrievalSettings {

  private final RagProperties ragProperties;

  @Override
  public int getTopK() {
    return ragProperties.getRetrieval().getTopK();
  }

  @Override
  public double getScoreThreshold() {
    return ragProperties.getRetrieval().getScoreThreshold();
  }
}
