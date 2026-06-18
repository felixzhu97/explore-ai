package com.ai.adapter.out.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "rag.mock.embeddings", havingValue = "true", matchIfMissing = false)
public class MockEmbeddingAdapter implements EmbeddingAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockEmbeddingAdapter.class);
    private static final int DIMENSIONS = 1536;
    private final Random random = new Random();

    public float[] embed(String text) {
        log.debug("Mock embedding for text (length={})", text.length());
        float[] embedding = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            embedding[i] = random.nextFloat() * 2 - 1;
        }
        return embedding;
    }

    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).collect(Collectors.toList());
    }

    public int getDimensions() {
        return DIMENSIONS;
    }
}
