package com.ai.eval.controller;

import com.ai.eval.controller.dto.EvaluationRequest;
import com.ai.eval.controller.dto.EvaluationResponse;
import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.service.ChatQualityEvaluator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for chat evaluation endpoints. */
@RestController
@RequestMapping("/api/eval")
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-eval",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
public class EvalController {

  private final ChatQualityEvaluator evaluator;

  /** Scores a chat answer. */
  @PostMapping("/chat")
  public ResponseEntity<EvaluationResponse> evaluateChat(
      @Valid @RequestBody EvaluationRequest request) {

    ChatEvaluationResult result =
        evaluator.evaluate(
            request.userMessage(), request.assistantResponse(), request.referenceDocuments());

    return ResponseEntity.ok(EvaluationResponse.from(result));
  }
}
