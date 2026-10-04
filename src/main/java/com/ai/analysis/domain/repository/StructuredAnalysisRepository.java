package com.ai.analysis.domain.repository;

import com.ai.analysis.domain.model.TextAnalysis;
import com.ai.analysis.domain.vo.AnalysisText;
import com.ai.analysis.domain.vo.LanguageHint;

/** Repository that turns input text into a structured {@code TextAnalysis} via an AI model. */
public interface StructuredAnalysisRepository {
  TextAnalysis analyze(AnalysisText text, LanguageHint hint);
}
