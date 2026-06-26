package com.ai.rag.domain.util;

/**
 * Utility class for vector similarity calculations.
 * Provides common similarity metrics for embedding vectors.
 */
public final class VectorSimilarity {

    private VectorSimilarity() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Calculates cosine similarity between two vectors.
     *
     * @param a First vector
     * @param b Second vector
     * @return Cosine similarity score between -1 and 1, or 0.0 if vectors are invalid
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator > 0 ? dotProduct / denominator : 0.0;
    }
}
