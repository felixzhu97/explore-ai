package com.ai.eval.service.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.eval.domain.model.OfficialGateResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfficialSpringAiEvaluators")
class OfficialSpringAiEvaluatorsTest {

  @Mock private RelevancyEvaluator relevancyEvaluator;
  @Mock private FactCheckingEvaluator factCheckingEvaluator;

  @InjectMocks private OfficialSpringAiEvaluators evaluators;

  @Test
  @DisplayName("should pass without factuality when no context")
  void shouldPassWithoutFactualityWhenNoContext() {
    when(relevancyEvaluator.evaluate(any(EvaluationRequest.class)))
        .thenReturn(new EvaluationResponse(true, 1.0f, "ok", Map.of()));

    OfficialGateResult result = evaluators.evaluate("q", "a", List.of());

    assertThat(result.passed()).isTrue();
    assertThat(result.relevancyPass()).isTrue();
    assertThat(result.factualityEvaluated()).isFalse();
    assertThat(result.factualityPass()).isNull();
    assertThat(result.relevanceScore()).isEqualTo(1.0);
    verify(factCheckingEvaluator, never()).evaluate(any());
  }

  @Test
  @DisplayName("should require both passes when context present")
  void shouldRequireBothPassesWhenContextPresent() {
    when(relevancyEvaluator.evaluate(any(EvaluationRequest.class)))
        .thenReturn(new EvaluationResponse(true, 1.0f, "", Map.of()));
    when(factCheckingEvaluator.evaluate(any(EvaluationRequest.class)))
        .thenReturn(new EvaluationResponse(false, 0.0f, "unsupported", Map.of()));

    OfficialGateResult result = evaluators.evaluate("q", "a", List.of("doc text"));

    assertThat(result.passed()).isFalse();
    assertThat(result.factualityEvaluated()).isTrue();
    assertThat(result.factualityPass()).isFalse();
    assertThat(result.feedback()).anyMatch(f -> f.contains("factuality"));
  }
}
