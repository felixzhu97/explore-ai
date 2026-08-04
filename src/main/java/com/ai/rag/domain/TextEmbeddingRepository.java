package com.ai.rag.domain;

import java.util.List;

public interface TextEmbeddingRepository {

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);

    int getDimensions();
}
