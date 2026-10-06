package com.ai.textanalysis.domain.repository;

import com.ai.textanalysis.domain.model.TextAnalysis;
import com.ai.textanalysis.domain.vo.AnalysisText;
import com.ai.textanalysis.domain.vo.LanguageHint;

/** Repository that turns input text into a structured {@code TextAnalysis} via an AI model. */
public interface TextAnalysisGateway {
  /** Analyzes the text. */
  TextAnalysis analyze(AnalysisText text, LanguageHint hint);
}
