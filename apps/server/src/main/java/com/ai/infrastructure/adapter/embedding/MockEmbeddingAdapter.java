package com.ai.infrastructure.adapter.embedding;

import com.ai.application.port.EmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Mock embedding adapter for testing without API calls.
 * Returns random embeddings when enabled via configuration.
 */
@Component
@ConditionalOnProperty(name = "rag.mock.embeddings", havingValue = "true", matchIfMissing = false)
public class MockEmbeddingAdapter implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(MockEmbeddingAdapter.class);
    private static final int DIMENSIONS = 1536;
    private final Random random = new Random();

    @Override
    public float[] embed(String text) {
        log.debug("Mock embedding for text (length={})", text.length());
        float[] embedding = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            embedding[i] = random.nextFloat() * 2 - 1; // Random values between -1 and 1
        }
        return embedding;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(this::embed).collect(Collectors.toList());
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }
}
