package com.ai.rag.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);
    private static final String[] SEPARATORS = {
        "\n\n", "\n", ". ", "! ", "? ", "; ", ", ", " "
    };

    private static final Pattern[] SEPARATOR_PATTERNS;

    static {
        SEPARATOR_PATTERNS = new Pattern[SEPARATORS.length];
        for (int i = 0; i < SEPARATORS.length; i++) {
            SEPARATOR_PATTERNS[i] = Pattern.compile(Pattern.quote(SEPARATORS[i]));
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

    public List<String> chunk(String text) {
        return chunk(text, SEPARATORS);
    }

    public List<String> chunk(String text, String[] separators) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        String[] segments = splitBySeparator(text, separators[0]);

        for (String segment : segments) {
            if (segment.length() <= chunkSize) {
                result.add(segment.trim());
            } else {
                result.addAll(recursiveChunk(segment, 1, separators));
            }
        }

        return result.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> recursiveChunk(String text, int index, String[] separators) {
        if (index >= separators.length) {
            return splitBySize(text);
        }

        List<String> result = new ArrayList<>();
        String[] parts = splitBySeparator(text, separators[index]);

        for (String part : parts) {
            if (part.length() <= chunkSize) {
                result.add(part);
            } else {
                result.addAll(recursiveChunk(part, index + 1, separators));
            }
        }
        return result;
    }

    private String[] splitBySeparator(String text, String separator) {
        if (separator == null || separator.isEmpty() || " ".equals(separator)) {
            return new String[]{text};
        }
        return Pattern.compile(Pattern.quote(separator)).split(text);
    }

    private List<String> splitBySize(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, start, end);
                result.add(text.substring(start, breakPoint));
                start = breakPoint;
                
                if (chunkOverlap > 0 && start < text.length()) {
                    start = Math.max(start - chunkOverlap, start);
                }
            } else {
                result.add(text.substring(start));
                break;
            }
        }
        return result;
    }

    private int findBreakPoint(String text, int start, int end) {
        for (int i = end - 1; i >= start && i > end - 50; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return end;
    }
}
