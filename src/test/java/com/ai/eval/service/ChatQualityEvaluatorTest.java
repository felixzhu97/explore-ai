package com.ai.eval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.domain.model.OfficialGateResult;
import com.ai.eval.service.ChatQualityEvaluator.LlmEvaluationResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatQualityEvaluator")
class ChatQualityEvaluatorTest {

  @Mock private OfficialSpringAiEvaluators officialEvaluators;

  @Mock private ChatClient evaluationChatClient;

  @Mock(answer = Answers.RETURNS_SELF)
  private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  private ChatQualityEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new ChatQualityEvaluator(officialEvaluators, evaluationChatClient);
  }

  @Test
  @DisplayName("should skip factuality when referenceDocuments is empty")
  void shouldSkipFactualityWhenNoReferenceDocuments() {
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(
            new OfficialGateResult(true, null, false, 1.0, null, List.of("relevancy: PASS"), true));
    stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

    ChatEvaluationResult result =
        evaluator.evaluate(
            "What is the capital of France?", "Paris is the capital of France.", List.of());

    assertThat(result.factualityAvailable()).isFalse();
    assertThat(result.factualityScore()).isNull();
    assertThat(result.relevancyPassed()).isTrue();
    assertThat(result.factualityPassed()).isNull();
  }

  @Test
  @DisplayName("should evaluate factuality when referenceDocuments provided")
  void shouldEvaluateFactualityWhenReferenceDocumentsProvided() {
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(
            new OfficialGateResult(
                true, true, true, 1.0, 1.0, List.of("relevancy: PASS", "factuality: PASS"), true));
    stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

    ChatEvaluationResult result =
        evaluator.evaluate(
            "What is the capital of France?",
            "Paris is the capital of France.",
            List.of("France is a country in Europe. Its capital is Paris."));

    assertThat(result.factualityAvailable()).isTrue();
    assertThat(result.factualityScore()).isEqualTo(1.0);
    assertThat(result.factualityPassed()).isTrue();
    assertThat(result.evaluatorFeedback()).isNotEmpty();
  }

  @Test
  @DisplayName("should use fallback when LLM judge returns null")
  void shouldUseFallbackWhenLlmJudgeReturnsNull() {
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(
            new OfficialGateResult(true, null, false, 1.0, null, List.of("relevancy: PASS"), true));
    stubLlmJudge(null);

    ChatEvaluationResult result = evaluator.evaluate("Hello", "Hi there", List.of());

    assertThat(result.coherenceScore()).isZero();
    assertThat(result.helpfulnessScore()).isZero();
    assertThat(result.suggestions()).contains("Evaluation failed to process");
  }

  @Test
  @DisplayName("should use default safety concern when blank")
  void shouldUseDefaultSafetyConcernWhenBlank() {
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(
            new OfficialGateResult(true, null, false, 1.0, null, List.of("relevancy: PASS"), true));
    stubLlmJudge(new LlmEvaluationResponse(0.8, 0.8, true, "", ""));

    ChatEvaluationResult result = evaluator.evaluate("Hello", "Harmful reply", List.of());

    assertThat(result.hasSafetyIssues()).isTrue();
    assertThat(result.safetyFlags()).containsExactly("Safety issue detected");
  }

  @Test
  @DisplayName("should include factuality in overall score when available")
  void shouldIncludeFactualityInOverallScoreWhenAvailable() {
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(new OfficialGateResult(true, true, true, 1.0, 1.0, List.of(), true));
    stubLlmJudge(new LlmEvaluationResponse(0.8, 0.8, false, "", ""));

    ChatEvaluationResult result =
        evaluator.evaluate("Question", "Answer", List.of("Reference context"));

    assertThat(result.overallScore()).isEqualTo(0.9);
  }

  @Test
  @DisplayName("should suggest relevance fix when relevancy fails")
  void shouldSuggestRelevanceFixWhenRelevancyFails() {
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(
            new OfficialGateResult(
                false, null, false, 0.0, null, List.of("relevancy: FAIL"), false));
    stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

    ChatEvaluationResult result = evaluator.evaluate("Q", "unrelated", List.of());

    assertThat(result.relevancyPassed()).isFalse();
    assertThat(result.suggestions())
        .contains("Response does not fully address the user's question");
  }

  private void stubLlmJudge(LlmEvaluationResponse response) {
    when(evaluationChatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.entity(eq(LlmEvaluationResponse.class), any())).thenReturn(response);
  }
}
