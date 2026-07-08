package com.ai.analysis.application.usecase;

import com.ai.analysis.domain.model.TextAnalysis;
import com.ai.analysis.domain.repository.StructuredAnalysisRepository;
import com.ai.analysis.domain.vo.AnalysisText;
import com.ai.analysis.domain.vo.LanguageHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnalysisFacade {

    private static final Logger log = LoggerFactory.getLogger(AnalysisFacade.class);

    private final StructuredAnalysisRepository structuredAnalysisRepository;

    public AnalysisFacade(StructuredAnalysisRepository structuredAnalysisRepository) {
        this.structuredAnalysisRepository = structuredAnalysisRepository;
    }

    public TextAnalysis analyzeText(String text) {
        log.info("AnalysisFacade.analyzeText: {}", truncate(text));
        return structuredAnalysisRepository.analyze(AnalysisText.of(text), LanguageHint.none());
    }

    public TextAnalysis analyzeTextWithLanguage(String text, String language) {
        log.info("AnalysisFacade.analyzeTextWithLanguage: {} lang={}", truncate(text), language);
        return structuredAnalysisRepository.analyze(
                AnalysisText.of(text), LanguageHint.of(language));
    }

    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
}
