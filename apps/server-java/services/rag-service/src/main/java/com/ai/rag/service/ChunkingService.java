package com.ai.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for splitting documents into chunks for embedding.
 * Implements smart text chunking with configurable size and overlap.
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    private static final String[] SENTENCE_SEPARATORS = {"\n\n", "\n", ". ", "? ", "! ", "; ", ", "};
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");

    /**
     * Chunk text with default settings.
     *
     * @param text Text to chunk
     * @return List of text chunks
     */
    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    /**
     * Chunk text with custom size and overlap.
     *
     * @param text       Text to chunk
     * @param chunkSize  Target size of each chunk (in characters)
     * @param overlap    Overlap between chunks (in characters)
     * @return List of text chunks
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            String chunk = text.substring(start, end);

            // Try to break at natural boundaries
            if (end < textLength) {
                chunk = breakAtNaturalBoundary(chunk);
            }

            if (!chunk.isBlank()) {
                chunks.add(chunk.trim());
            }

            // Move start position, accounting for overlap
            start = start + chunk.length() - overlap;
            if (start <= 0 || start >= textLength) {
                break;
            }
        }

        log.debug("Chunked text into {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Chunk text by paragraphs first, then by size if paragraphs are too large.
     *
     * @param text Text to chunk
     * @param chunkSize Target size of each chunk
     * @param overlap Overlap between chunks
     * @return List of text chunks
     */
    public List<String> chunkByParagraphs(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Split into paragraphs
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }

            // If adding this paragraph exceeds chunk size, save current and start new
            if (currentChunk.length() + trimmedParagraph.length() + 2 > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                // If single paragraph exceeds chunk size, split it further
                if (trimmedParagraph.length() > chunkSize) {
                    chunks.addAll(chunkText(trimmedParagraph, chunkSize, overlap));
                } else {
                    currentChunk.append(trimmedParagraph);
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(trimmedParagraph);
            }
        }

        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.debug("Chunked text into {} chunks (paragraph-aware)", chunks.size());
        return chunks;
    }

    /**
     * Attempt to break a chunk at a natural sentence/paragraph boundary.
     */
    private String breakAtNaturalBoundary(String chunk) {
        // Find the last occurrence of sentence separators
        int bestBreakPoint = -1;
        int minPosition = chunk.length() / 2; // Don't break in the middle

        for (String separator : SENTENCE_SEPARATORS) {
            int lastIndex = chunk.lastIndexOf(separator);
            if (lastIndex > minPosition) {
                bestBreakPoint = lastIndex + separator.length();
                break;
            }
        }

        if (bestBreakPoint > 0 && bestBreakPoint < chunk.length()) {
            return chunk.substring(0, bestBreakPoint);
        }

        return chunk;
    }

    /**
     * Count approximate tokens (rough estimate: 1 token ≈ 4 characters for English).
     */
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.length() / 4;
    }

    /**
     * Check if text needs chunking.
     */
    public boolean needsChunking(String text, int threshold) {
        return estimateTokens(text) > threshold;
    }
}
