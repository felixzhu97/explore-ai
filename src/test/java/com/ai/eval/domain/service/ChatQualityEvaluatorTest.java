package com.ai.eval.domain.service;

import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.domain.model.LlmEvaluationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatQualityEvaluator")
class ChatQualityEvaluatorTest {

    @Mock
    private RelevancyEvaluator relevancyEvaluator;

    @Mock
    private FactCheckingEvaluator factCheckingEvaluator;

    @Mock
    private ChatClient evaluationChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ChatQualityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ChatQualityEvaluator(relevancyEvaluator, factCheckingEvaluator, evaluationChatClient);
    }

    @Test
    @DisplayName("LlmEvaluationResponse should hold values correctly")
    void shouldHoldValuesCorrectly() {
        LlmEvaluationResponse response = new LlmEvaluationResponse(
            0.9, 0.85, true, "Harmful content", "Avoid such content"
        );

        assertThat(response.coherenceScore()).isEqualTo(0.9);
        assertThat(response.helpfulnessScore()).isEqualTo(0.85);
        assertThat(response.hasSafetyIssues()).isTrue();
        assertThat(response.safetyConcern()).isEqualTo("Harmful content");
        assertThat(response.suggestion()).isEqualTo("Avoid such content");
    }

    @Test
    @DisplayName("ChatEvaluationResult builder should work correctly with all fields")
    void shouldBuildCorrectlyWithAllFields() {
        ChatEvaluationResult result = ChatEvaluationResult.builder()
            .coherenceScore(0.9)
            .relevanceScore(0.85)
            .helpfulnessScore(0.8)
            .factualityScore(0.95)
            .factualityAvailable(true)
            .overallScore(0.85)
            .hasSafetyIssues(true)
            .safetyFlags(List.of("Test flag"))
            .suggestions(List.of("Test suggestion"))
            .build();

        assertThat(result.coherenceScore()).isEqualTo(0.9);
        assertThat(result.relevanceScore()).isEqualTo(0.85);
        assertThat(result.helpfulnessScore()).isEqualTo(0.8);
        assertThat(result.factualityScore()).isEqualTo(0.95);
        assertThat(result.factualityAvailable()).isTrue();
        assertThat(result.overallScore()).isEqualTo(0.85);
        assertThat(result.hasSafetyIssues()).isTrue();
        assertThat(result.safetyFlags()).hasSize(1);
        assertThat(result.suggestions()).hasSize(1);
    }

    @Test
    @DisplayName("should clamp scores between 0 and 1")
    void shouldClampScoresBetween0And1() {
        ChatEvaluationResult result = ChatEvaluationResult.builder()
            .coherenceScore(1.5)
            .relevanceScore(-0.5)
            .helpfulnessScore(0.8)
            .factualityScore(1.2)
            .overallScore(0.85)
            .build();

        assertThat(result.coherenceScore()).isEqualTo(1.0);
        assertThat(result.relevanceScore()).isEqualTo(0.0);
        assertThat(result.helpfulnessScore()).isEqualTo(0.8);
        assertThat(result.factualityScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should skip factuality when referenceDocuments is empty")
    void should_skipFactuality_when_noReferenceDocuments() {
        stubRelevancyPass();
        stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "What is the capital of France?",
            "Paris is the capital of France.",
            List.of()
        );

        assertThat(result.factualityAvailable()).isFalse();
        assertThat(result.factualityScore()).isNull();
        verify(factCheckingEvaluator, never()).evaluate(any(EvaluationRequest.class));
    }

    @Test
    @DisplayName("should evaluate factuality when referenceDocuments provided")
    void should_evaluateFactuality_when_referenceDocumentsProvided() {
        stubRelevancyPass();
        when(factCheckingEvaluator.evaluate(any(EvaluationRequest.class)))
            .thenReturn(new EvaluationResponse(true, 1.0f, "", Map.of()));
        stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "What is the capital of France?",
            "Paris is the capital of France.",
            List.of("France is a country in Europe. Its capital is Paris.")
        );

        assertThat(result.factualityAvailable()).isTrue();
        assertThat(result.factualityScore()).isEqualTo(1.0);
        verify(factCheckingEvaluator).evaluate(any(EvaluationRequest.class));
    }

    @Test
    @DisplayName("should use fallback when LLM judge returns null")
    void should_useFallback_when_llmJudgeReturnsNull() {
        stubRelevancyPass();
        lenient().when(evaluationChatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        lenient().when(requestSpec.messages(any(Message.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(LlmEvaluationResponse.class), any())).thenReturn(null);

        ChatEvaluationResult result = evaluator.evaluate(
            "Hello",
            "Hi there",
            List.of()
        );

        assertThat(result.coherenceScore()).isZero();
        assertThat(result.helpfulnessScore()).isZero();
        assertThat(result.suggestions()).contains("Evaluation failed to process");
    }

    @Test
    @DisplayName("should use default safety concern when blank")
    void should_useDefaultSafetyConcern_when_blank() {
        stubRelevancyPass();
        stubLlmJudge(new LlmEvaluationResponse(0.8, 0.8, true, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "Hello",
            "Harmful reply",
            List.of()
        );

        assertThat(result.hasSafetyIssues()).isTrue();
        assertThat(result.safetyFlags()).containsExactly("Safety issue detected");
    }

    @Test
    @DisplayName("should include factuality in overall score when available")
    void should_includeFactualityInOverallScore_when_available() {
        stubRelevancyPass();
        when(factCheckingEvaluator.evaluate(any(EvaluationRequest.class)))
            .thenReturn(new EvaluationResponse(true, 1.0f, "", Map.of()));
        stubLlmJudge(new LlmEvaluationResponse(0.8, 0.8, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "Question",
            "Answer",
            List.of("Reference context")
        );

        assertThat(result.overallScore()).isEqualTo(0.9);
    }

    private void stubRelevancyPass() {
        when(relevancyEvaluator.evaluate(any(EvaluationRequest.class)))
            .thenReturn(new EvaluationResponse(true, 1.0f, "", Map.of()));
    }

    private void stubLlmJudge(LlmEvaluationResponse response) {
        lenient().when(evaluationChatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        lenient().when(requestSpec.messages(any(Message.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(LlmEvaluationResponse.class), any())).thenReturn(response);
    }
}
