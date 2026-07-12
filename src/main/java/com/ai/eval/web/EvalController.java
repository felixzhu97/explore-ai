package com.ai.eval.web;

import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.application.usecase.ChatQualityEvaluator;
import com.ai.eval.web.dto.EvaluationRequest;
import com.ai.eval.web.dto.EvaluationResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for chat evaluation endpoints.
 */
@RestController
@RequestMapping("/api/eval")
public class EvalController {

    private static final Logger log = LoggerFactory.getLogger(EvalController.class);

    private final ChatQualityEvaluator evaluator;

    public EvalController(ChatQualityEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @PostMapping("/chat")
    public ResponseEntity<EvaluationResponse> evaluateChat(@Valid @RequestBody EvaluationRequest request) {
        log.info("Evaluating chat: userMessage={}", truncate(request.userMessage()));

        ChatEvaluationResult result = evaluator.evaluate(
            request.userMessage(),
            request.assistantResponse(),
            request.referenceDocuments()
        );

        return ResponseEntity.ok(EvaluationResponse.from(result));
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
