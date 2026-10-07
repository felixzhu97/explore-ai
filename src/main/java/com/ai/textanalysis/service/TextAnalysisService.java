package com.ai.textanalysis.service;

import com.ai.textanalysis.domain.model.AnalysisText;
import com.ai.textanalysis.domain.model.LanguageHint;
import com.ai.textanalysis.domain.model.TextAnalysis;
import com.ai.textanalysis.domain.repository.TextAnalysisGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Entry point for structured text analysis: summary, sentiment, key points, and entities. */
@Service
@RequiredArgsConstructor
public class TextAnalysisService {

  private final TextAnalysisGateway textAnalysisGateway;

  /** Analyzes the text and asks the model to respond in the given language. */
  public TextAnalysis analyzeTextWithLanguage(String text, String language) {
    return textAnalysisGateway.analyze(AnalysisText.of(text), LanguageHint.of(language));
  }

  /** Analyzes the text. */
  public TextAnalysis analyzeText(String text) {
    return textAnalysisGateway.analyze(AnalysisText.of(text), LanguageHint.none());
  }
}
