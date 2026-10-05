package com.ai.textanalysis.service;

import com.ai.common.infra.logging.LogSanitizer;
import com.ai.textanalysis.domain.model.TextAnalysis;
import com.ai.textanalysis.domain.repository.TextAnalysisGateway;
import com.ai.textanalysis.domain.vo.AnalysisText;
import com.ai.textanalysis.domain.vo.LanguageHint;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Entry point for structured text analysis: summary, sentiment, key points, and entities. */
@Service
@RequiredArgsConstructor
public class TextAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(TextAnalysisService.class);

  private final TextAnalysisGateway textAnalysisGateway;

  /** Analyzes the text and asks the model to respond in the given language. */
  public TextAnalysis analyzeTextWithLanguage(String text, String language) {
    log.info(
        "TextAnalysisService.analyzeTextWithLanguage: {} lang={}",
        LogSanitizer.truncate(text),
        language);
    return textAnalysisGateway.analyze(AnalysisText.of(text), LanguageHint.of(language));
  }

  public TextAnalysis analyzeText(String text) {
    log.info("TextAnalysisService.analyzeText: {}", LogSanitizer.truncate(text));
    return textAnalysisGateway.analyze(AnalysisText.of(text), LanguageHint.none());
  }
}
