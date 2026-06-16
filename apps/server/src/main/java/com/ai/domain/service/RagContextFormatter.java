package com.ai.domain.service;

import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;

import java.util.List;
import java.util.Map;

/**
 * Domain service responsible for formatting RAG context and source documents.
 * Extracts similarity calculation, document title extraction, and context building
 * logic from application use cases into the domain layer.
 */
public class RagContextFormatter {

    public static final String SIMILARITY_FORMAT = "%.1f";
    public static final String UNKNOWN_DOCUMENT = "unknown";
    private static final int MAX_SOURCE_LENGTH = 500;
    private static final String TITLE_KEY = "documentTitle";
    private static final String FILENAME_KEY = "filename";

    public static SourceDocument formatSource(DocumentChunk chunk, int index, float[] queryEmbedding) {
        double similarity = calculateSimilarity(queryEmbedding, chunk.getEmbedding());
        String documentTitle = extractDocumentTitle(chunk.getMetadata());
        String snippet = extractSnippet(chunk.getContent());

        return new SourceDocument(
                index,
                snippet,
                similarity,
                documentTitle,
                chunk.getMetadata()
        );
    }

    public static String buildContextWithSources(List<DocumentChunk> chunks, float[] queryEmbedding) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            int sourceIndex = i + 1;

            SourceDocument source = formatSource(chunk, sourceIndex, queryEmbedding);
            String sourceMarker = formatSourceMarker(source);

            sb.append(sourceMarker)
              .append(chunk.getContent())
              .append("\n\n");
        }

        return sb.toString();
    }

    public static String formatSourceMarker(SourceDocument source) {
        double similarityPercent = source.score() * 100;

        return "[Source " + source.index() + "] (document: " +
               source.documentTitle() +
               ", similarity: " +
               String.format(SIMILARITY_FORMAT, similarityPercent) +
               "%)\n";
    }

    public static double calculateSimilarity(float[] a, float[] b) {
        if (a == null || b == null) {
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

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }

    public static String extractDocumentTitle(Map<String, Object> metadata) {
        if (metadata == null) {
            return UNKNOWN_DOCUMENT;
        }

        Object title = metadata.get(TITLE_KEY);
        if (title != null) {
            return title.toString();
        }

        Object filename = metadata.get(FILENAME_KEY);
        if (filename != null) {
            return filename.toString();
        }

        return UNKNOWN_DOCUMENT;
    }

    private static String extractSnippet(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        int length = Math.min(MAX_SOURCE_LENGTH, content.length());
        return content.substring(0, length);
    }
}
