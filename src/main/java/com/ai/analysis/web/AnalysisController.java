package com.ai.analysis.web;

import com.ai.analysis.application.usecase.AnalysisFacade;
import com.ai.analysis.web.dto.TextAnalysisRequest;
import com.ai.analysis.web.dto.TextAnalysisResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Text analysis REST Controller.
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final AnalysisFacade analysisFacade;

    public AnalysisController(AnalysisFacade analysisFacade) {
        this.analysisFacade = analysisFacade;
    }

    /**
     * Analyzes text and returns structured result.
     */
    @PostMapping("/chat/analyze")
    public ResponseEntity<TextAnalysisResult> analyzeText(@Valid @RequestBody TextAnalysisRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        TextAnalysisResult result;
        if (request.language() != null && !request.language().isBlank()) {
            result = analysisFacade.analyzeTextWithLanguage(request.text(), request.language());
        } else {
            result = analysisFacade.analyzeText(request.text());
        }

        return ResponseEntity.ok(result);
    }
}
