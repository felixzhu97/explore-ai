package com.ai.rag.domain;

public interface RagRetrievalSettings {

    int getTopK();

    double getScoreThreshold();
}
