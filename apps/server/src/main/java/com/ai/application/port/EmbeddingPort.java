package com.ai.application.port;

import java.util.List;

public interface EmbeddingPort {
    float[] embed(String text);
    List<float[]> embed(List<String> texts);
    int getDimensions();
}
