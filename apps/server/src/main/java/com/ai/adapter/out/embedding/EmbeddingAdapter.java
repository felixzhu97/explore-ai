package com.ai.adapter.out.embedding;

import java.util.List;

public interface EmbeddingAdapter {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    int getDimensions();
}
