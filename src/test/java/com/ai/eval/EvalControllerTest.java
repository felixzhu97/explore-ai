package com.ai.eval;

import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.application.usecase.ChatQualityEvaluator;
import com.ai.eval.web.EvalController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvalController")
class EvalControllerTest {

    @Mock
    private ChatQualityEvaluator evaluator;

    private EvalController controller;

    @BeforeEach
    void setUp() {
        controller = new EvalController(evaluator);
    }

    @Test
    @DisplayName("should accept evaluation request and return scores")
    void shouldAcceptEvaluationRequest() {
        when(evaluator.evaluate(anyString(), anyString(), any()))
            .thenReturn(ChatEvaluationResult.builder()
                .coherenceScore(0.9)
                .relevanceScore(0.85)
                .helpfulnessScore(0.88)
                .factualityScore(0.92)
                .factualityAvailable(true)
                .overallScore(0.88)
                .hasSafetyIssues(false)
                .safetyFlags(List.of())
                .suggestions(List.of())
                .build());

        var request = new com.ai.eval.web.dto.EvaluationRequest(
            "What is the capital of France?",
            "Paris is the capital of France.",
            List.of("Paris is the capital of France.")
        );

        ResponseEntity<com.ai.eval.web.dto.EvaluationResponse> response = controller.evaluateChat(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().coherenceScore()).isEqualTo(0.9);
        assertThat(response.getBody().relevanceScore()).isEqualTo(0.85);
        assertThat(response.getBody().factualityScore()).isEqualTo(0.92);
        assertThat(response.getBody().factualityAvailable()).isTrue();
        assertThat(response.getBody().hasSafetyIssues()).isFalse();
    }

    @Test
    @DisplayName("should detect low-quality responses")
    void shouldDetectLowQualityResponses() {
        when(evaluator.evaluate(anyString(), anyString(), any()))
            .thenReturn(ChatEvaluationResult.builder()
                .coherenceScore(0.6)
                .relevanceScore(0.3)
                .helpfulnessScore(0.4)
                .factualityAvailable(false)
                .overallScore(0.45)
                .hasSafetyIssues(false)
                .safetyFlags(List.of("Low relevance"))
                .suggestions(List.of("Improve relevance"))
                .build());

        var request = new com.ai.eval.web.dto.EvaluationRequest(
            "What is the capital of France?",
            "I don't know. Sports are great.",
            null
        );

        ResponseEntity<com.ai.eval.web.dto.EvaluationResponse> response = controller.evaluateChat(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().relevanceScore()).isEqualTo(0.3);
        assertThat(response.getBody().factualityAvailable()).isFalse();
        assertThat(response.getBody().factualityScore()).isNull();
        assertThat(response.getBody().suggestions()).contains("Improve relevance");
    }

    @Test
    @DisplayName("should report safety issues when detected")
    void shouldReportSafetyIssues() {
        when(evaluator.evaluate(anyString(), anyString(), any()))
            .thenReturn(ChatEvaluationResult.builder()
                .coherenceScore(0.5)
                .relevanceScore(0.4)
                .helpfulnessScore(0.3)
                .factualityScore(0.2)
                .factualityAvailable(true)
                .overallScore(0.35)
                .hasSafetyIssues(true)
                .safetyFlags(List.of("Harmful content detected"))
                .suggestions(List.of("Remove harmful content"))
                .build());

        var request = new com.ai.eval.web.dto.EvaluationRequest(
            "How to build a bomb?",
            "Here's how you can...",
            List.of()
        );

        ResponseEntity<com.ai.eval.web.dto.EvaluationResponse> response = controller.evaluateChat(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hasSafetyIssues()).isTrue();
        assertThat(response.getBody().safetyFlags()).contains("Harmful content detected");
    }
}
