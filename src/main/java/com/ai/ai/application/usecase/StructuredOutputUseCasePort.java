package com.ai.ai.application.usecase;

import com.ai.ai.web.dto.TextAnalysisResult;

public interface StructuredOutputUseCasePort {
    TextAnalysisResult analyzeText(String text);
    TextAnalysisResult analyzeTextWithLanguage(String text, String language);
}
