package com.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG (Retrieval-Augmented Generation) configuration properties.
 * Binds configuration from application.yml under 'rag' prefix.
 */
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Chunk chunk = new Chunk();
    private Retrieval retrieval = new Retrieval();

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }

    public static class Chunk {
        private int size = 500;
        private int overlap = 50;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getOverlap() {
            return overlap;
        }

        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }
    }

    public static class Retrieval {
        private int topK = 5;
        private double scoreThreshold = 0.5;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getScoreThreshold() {
            return scoreThreshold;
        }

        public void setScoreThreshold(double scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }
    }
}
