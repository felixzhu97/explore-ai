package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OcrResponse(
    String task,
    String model,
    List<TextBlock> results,
    String fullText,
    double processingTimeMs
) {
    public record TextBlock(
        String text,
        float confidence,
        List<List<Float>> bbox
    ) {}

    public static OcrResponse of(String model, List<TextBlock> results, double processingTimeMs) {
        String fullText = results.stream()
            .map(TextBlock::text)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
        return new OcrResponse("extract_text", model, results, fullText, processingTimeMs);
    }
}
