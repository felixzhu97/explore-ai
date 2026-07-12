package com.ai.rag.domain.repository;

import java.util.List;

public interface TextEmbeddingRepository {

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);

    int getDimensions();
}
