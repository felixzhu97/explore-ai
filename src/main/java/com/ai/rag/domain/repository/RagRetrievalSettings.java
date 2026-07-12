package com.ai.rag.domain.repository;

public interface RagRetrievalSettings {

    int getTopK();

    double getScoreThreshold();
}
