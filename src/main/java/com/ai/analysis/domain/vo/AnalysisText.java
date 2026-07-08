package com.ai.analysis.domain.vo;

import com.ai.analysis.domain.exception.InvalidAnalysisTextException;

public record AnalysisText(String value) {

    private static final int MAX_LENGTH = 50_000;

    private static final String ANALYSIS_PROMPT_TEMPLATE = """
            Analyze the following text and provide a structured response.

            Text: %s

            Respond with a JSON object containing:
            - summary: A brief summary of the text (max 50 words)
            - sentiment: One of POSITIVE, NEUTRAL, or NEGATIVE
            - key_points: 3-5 key takeaways from the text
            - entities: List of named entities (people, places, organizations) mentioned
            - language: The detected language of the text

            Be concise and accurate in your analysis.
            """;

    public static AnalysisText of(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidAnalysisTextException("Analysis text must not be blank");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidAnalysisTextException(
                    "Analysis text exceeds maximum length of " + MAX_LENGTH);
        }
        return new AnalysisText(trimmed);
    }

    public String buildAnalysisPrompt(LanguageHint hint) {
        String prompt = ANALYSIS_PROMPT_TEMPLATE.formatted(value);
        if (hint != null && hint.isSpecified()) {
            prompt += "\n\nPlease respond in " + hint.responseLanguage() + ".";
        }
        return prompt;
    }
}
