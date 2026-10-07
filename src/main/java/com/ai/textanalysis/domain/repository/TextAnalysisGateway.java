package com.ai.textanalysis.domain.repository;

import com.ai.textanalysis.domain.model.AnalysisText;
import com.ai.textanalysis.domain.model.LanguageHint;
import com.ai.textanalysis.domain.model.TextAnalysis;

/** Repository that turns input text into a structured {@code TextAnalysis} via an AI model. */
public interface TextAnalysisGateway {
  /** Analyzes the text. */
  TextAnalysis analyze(AnalysisText text, LanguageHint hint);
}
