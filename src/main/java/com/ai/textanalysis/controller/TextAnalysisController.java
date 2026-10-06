package com.ai.textanalysis.controller;

import com.ai.textanalysis.controller.dto.TextAnalysisRequest;
import com.ai.textanalysis.controller.dto.TextAnalysisResponse;
import com.ai.textanalysis.domain.exception.InvalidAnalysisTextException;
import com.ai.textanalysis.service.TextAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/text-analysis")
@RequiredArgsConstructor
public class TextAnalysisController {

  private final TextAnalysisService textAnalysisService;

  /** Analyzes the text. */
  @PostMapping
  public ResponseEntity<TextAnalysisResponse> analyzeText(
      @Valid @RequestBody TextAnalysisRequest request) {
    try {
      var result =
          request.language() != null && !request.language().isBlank()
              ? textAnalysisService.analyzeTextWithLanguage(request.text(), request.language())
              : textAnalysisService.analyzeText(request.text());
      return ResponseEntity.ok(TextAnalysisResponse.fromDomain(result));
    } catch (InvalidAnalysisTextException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
