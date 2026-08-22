package com.ai.eval.controller;

import com.ai.common.util.LogSanitizer;
import com.ai.eval.controller.dto.EvaluationRequest;
import com.ai.eval.controller.dto.EvaluationResponse;
import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.service.usecase.ChatQualityEvaluator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for chat evaluation endpoints. */
@RestController
@RequestMapping("/api/eval")
public class EvalController {

  private static final Logger log = LoggerFactory.getLogger(EvalController.class);

  private final ChatQualityEvaluator evaluator;

  /** Documentation. */
  public EvalController(ChatQualityEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  /** Documentation. */
  @PostMapping("/chat")
  public ResponseEntity<EvaluationResponse> evaluateChat(
      @Valid @RequestBody EvaluationRequest request) {
    log.info("Evaluating chat: userMessage={}", LogSanitizer.truncate(request.userMessage()));

    ChatEvaluationResult result =
        evaluator.evaluate(
            request.userMessage(), request.assistantResponse(), request.referenceDocuments());

    return ResponseEntity.ok(EvaluationResponse.from(result));
  }
}
