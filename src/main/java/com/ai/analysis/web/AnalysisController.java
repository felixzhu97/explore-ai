package com.ai.analysis.web;

import com.ai.analysis.application.usecase.AnalysisFacade;
import com.ai.analysis.domain.exception.InvalidAnalysisTextException;
import com.ai.analysis.web.dto.TextAnalysisRequest;
import com.ai.analysis.web.dto.TextAnalysisResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisFacade analysisFacade;

    public AnalysisController(AnalysisFacade analysisFacade) {
        this.analysisFacade = analysisFacade;
    }

    @PostMapping("/chat/analyze")
    public ResponseEntity<TextAnalysisResult> analyzeText(@Valid @RequestBody TextAnalysisRequest request) {
        try {
            var result = request.language() != null && !request.language().isBlank()
                    ? analysisFacade.analyzeTextWithLanguage(request.text(), request.language())
                    : analysisFacade.analyzeText(request.text());
            return ResponseEntity.ok(TextAnalysisResult.fromDomain(result));
        } catch (InvalidAnalysisTextException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
