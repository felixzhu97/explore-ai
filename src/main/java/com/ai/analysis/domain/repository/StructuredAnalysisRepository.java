package com.ai.analysis.domain.repository;

import com.ai.analysis.domain.model.TextAnalysis;
import com.ai.analysis.domain.vo.AnalysisText;
import com.ai.analysis.domain.vo.LanguageHint;

/** Documentation. */
public interface StructuredAnalysisRepository {
  /** Documentation. */
  TextAnalysis analyze(AnalysisText text, LanguageHint hint);
}
