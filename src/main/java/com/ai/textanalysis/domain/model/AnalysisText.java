package com.ai.textanalysis.domain.model;

import com.ai.common.exception.DomainException;
import lombok.Value;

/** Text to analyse, non-blank and capped in length. */
@Value
public class AnalysisText {
  String value;

  private static final int MAX_LENGTH = 50_000;

  private static final String ANALYSIS_PROMPT_TEMPLATE =
      """
            Analyze the following text and provide a structured response.

            Text: {text}

            Respond with a JSON object containing:
            - summary: A brief summary of the text (max 50 words)
            - sentiment: One of POSITIVE, NEUTRAL, or NEGATIVE
            - keyPoints: 3-5 key takeaways from the text
            - entities: List of named entities (people, places, organizations) mentioned
            - language: The detected language of the text

            Be concise and accurate in your analysis.
            """;

  public AnalysisText(String value) {
    if (value == null || value.isBlank()) {
      throw DomainException.invalid("INVALID_ANALYSIS_TEXT", "Analysis text must not be blank");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw DomainException.invalid(
          "INVALID_ANALYSIS_TEXT", "Analysis text exceeds maximum length of " + MAX_LENGTH);
    }
    this.value = value;
  }

  /** Wraps the text to analyze. */
  public static AnalysisText createText(String text) {
    return new AnalysisText(text);
  }

  /** Builds the structured-analysis prompt, appending a response-language instruction if set. */
  public String buildAnalysisPrompt(LanguageHint hint) {
    String prompt = ANALYSIS_PROMPT_TEMPLATE.replace("{text}", value);
    if (hint != null && hint.isSpecified()) {
      prompt += "\n\nPlease respond in " + hint.getResponseLanguage() + ".";
    }
    return prompt;
  }
}
