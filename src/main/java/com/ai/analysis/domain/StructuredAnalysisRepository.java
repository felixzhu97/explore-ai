package com.ai.analysis.domain;

import com.ai.analysis.domain.TextAnalysis;
import com.ai.analysis.domain.AnalysisText;
import com.ai.analysis.domain.LanguageHint;

public interface StructuredAnalysisRepository {

    TextAnalysis analyze(AnalysisText text, LanguageHint hint);
}
