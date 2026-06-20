package com.ai.modules.rag.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Domain service for text chunking operations.
 * Uses Recursive Character Text Splitting strategy.
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private static final String[] DEFAULT_SEPARATORS = {
        "\n\n",   // Paragraph break (highest priority)
        "\n",     // Line break
        ". ",     // Sentence (with period)
        "! ",     // Exclamation
        "? ",     // Question
        "; ",     // Semicolon
        ", ",     // Comma (lowest priority)
        " "       // Single space
    };

    private static final Pattern[] DEFAULT_SEPARATOR_PATTERNS;

    static {
        DEFAULT_SEPARATOR_PATTERNS = new Pattern[DEFAULT_SEPARATORS.length];
        for (int i = 0; i < DEFAULT_SEPARATORS.length; i++) {
            DEFAULT_SEPARATOR_PATTERNS[i] = Pattern.compile(Pattern.quote(DEFAULT_SEPARATORS[i]));
        }
    }

    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingService(
            @Value("${rag.chunk.size:500}") int chunkSize,
            @Value("${rag.chunk.overlap:50}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * Chunks text using Recursive Character Text Splitting.
     * 
     * Strategy: Try to split on the largest separator first (paragraph), then progressively
     * smaller ones until chunks are within size limits. This preserves semantic boundaries
     * while ensuring reasonable chunk sizes for embedding.
     *
     * @param text The text to chunk
     * @return List of text chunks
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Attempted to chunk null or blank text");
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        Pattern firstSeparator = DEFAULT_SEPARATOR_PATTERNS[0];
        String[] segments = firstSeparator.split(text);

        for (String segment : segments) {
            if (segment.length() <= chunkSize) {
                chunks.add(segment.trim());
            } else {
                chunks.addAll(recursiveChunk(segment, 1));
            }
        }

        log.info("Split text into {} chunks", chunks.size());
        return chunks.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> recursiveChunk(String text, int separatorIndex) {
        List<String> result = new ArrayList<>();

        if (separatorIndex >= DEFAULT_SEPARATORS.length) {
            // Fallback: force split by character chunks
            return splitBySize(text);
        }

        Pattern separator = DEFAULT_SEPARATOR_PATTERNS[separatorIndex];
        String[] parts = separator.split(text);

        for (String part : parts) {
            if (part.length() <= chunkSize) {
                result.add(part);
            } else if (separatorIndex + 1 < DEFAULT_SEPARATORS.length) {
                result.addAll(recursiveChunk(part, separatorIndex + 1));
            } else {
                result.addAll(splitBySize(part));
            }
        }

        return result;
    }

    private List<String> splitBySize(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            
            if (end < text.length()) {
                // Try to break at a space or natural boundary
                int breakPoint = findBreakPoint(text, start, end);
                result.add(text.substring(start, breakPoint));
                start = breakPoint;
                
                // Apply overlap
                if (chunkOverlap > 0 && start < text.length()) {
                    int overlapStart = Math.max(start - chunkOverlap, start);
                    start = overlapStart;
                }
            } else {
                result.add(text.substring(start));
                break;
            }
        }

        return result;
    }

    private int findBreakPoint(String text, int start, int end) {
        // Try to find a natural break point near the end
        for (int i = end - 1; i >= start && i > end - 50; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return end;
    }

    private String[] splitBySeparator(String text, String separator) {
        if (separator == null || separator.isEmpty()) {
            return new String[]{text};
        }
        if (" ".equals(separator)) {
            return new String[]{text};
        }
        return Pattern.compile(Pattern.quote(separator)).split(text);
    }

    /**
     * Chunks text with custom separators.
     *
     * @param text The text to chunk
     * @param separators Ordered list of separators (largest to smallest)
     * @return List of text chunks
     */
    public List<String> chunk(String text, String[] separators) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] segments = splitBySeparator(text, separators[0]);

        for (String segment : segments) {
            if (segment.length() <= chunkSize) {
                chunks.add(segment.trim());
            } else {
                chunks.addAll(recursiveChunkWithSeparators(segment, 1, separators));
            }
        }

        return chunks.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> recursiveChunkWithSeparators(String text, int separatorIndex, String[] separators) {
        if (separatorIndex >= separators.length) {
            return splitBySize(text);
        }

        List<String> result = new ArrayList<>();
        String separator = separators[separatorIndex];
        if (separator == null || separator.isEmpty()) {
            result.add(text);
            return result;
        }

        String[] parts = splitBySeparator(text, separator);

        for (String part : parts) {
            if (part.length() <= chunkSize) {
                result.add(part);
            } else if (separatorIndex + 1 < separators.length) {
                result.addAll(recursiveChunkWithSeparators(part, separatorIndex + 1, separators));
            } else {
                result.addAll(splitBySize(part));
            }
        }

        return result;
    }
}
