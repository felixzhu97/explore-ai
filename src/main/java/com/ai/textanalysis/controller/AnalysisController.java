package com.ai.textanalysis.controller;

import com.ai.textanalysis.controller.dto.TextAnalysisRequest;
import com.ai.textanalysis.controller.dto.TextAnalysisResult;
import com.ai.textanalysis.domain.exception.InvalidAnalysisTextException;
import com.ai.textanalysis.service.TextAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {

  private final TextAnalysisService textAnalysisService;

  public AnalysisController(TextAnalysisService textAnalysisService) {
    this.textAnalysisService = textAnalysisService;
  }

  @PostMapping("/chat/analyze")
  public ResponseEntity<TextAnalysisResult> analyzeText(
      @Valid @RequestBody TextAnalysisRequest request) {
    try {
      var result =
          request.language() != null && !request.language().isBlank()
              ? textAnalysisService.analyzeTextWithLanguage(request.text(), request.language())
              : textAnalysisService.analyzeText(request.text());
      return ResponseEntity.ok(TextAnalysisResult.fromDomain(result));
    } catch (InvalidAnalysisTextException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
