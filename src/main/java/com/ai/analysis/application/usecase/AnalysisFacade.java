package com.ai.analysis.application.usecase;

import com.ai.analysis.web.dto.TextAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Facade for text analysis operations.
 */
@Service
public class AnalysisFacade {

    private static final Logger log = LoggerFactory.getLogger(AnalysisFacade.class);

    private final SpringAiStructuredOutputUseCase structuredOutputUseCase;

    public AnalysisFacade(SpringAiStructuredOutputUseCase structuredOutputUseCase) {
        this.structuredOutputUseCase = structuredOutputUseCase;
    }

    /**
     * Analyze text and return structured result.
     */
    public TextAnalysisResult analyzeText(String text) {
        log.info("AnalysisFacade.analyzeText: {}", truncate(text));
        return structuredOutputUseCase.analyzeText(text);
    }

    /**
     * Analyze text with specified language.
     */
    public TextAnalysisResult analyzeTextWithLanguage(String text, String language) {
        log.info("AnalysisFacade.analyzeTextWithLanguage: {} lang={}", truncate(text), language);
        return structuredOutputUseCase.analyzeTextWithLanguage(text, language);
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
