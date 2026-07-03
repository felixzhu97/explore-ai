package com.ai.eval.domain.service;

import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.domain.model.LlmEvaluationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatQualityEvaluator")
class ChatQualityEvaluatorTest {

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
            .overallScore(0.85)
            .hasSafetyIssues(true)
            .safetyFlags(java.util.List.of("Test flag"))
            .suggestions(java.util.List.of("Test suggestion"))
            .build();

        assertThat(result.coherenceScore()).isEqualTo(0.9);
        assertThat(result.relevanceScore()).isEqualTo(0.85);
        assertThat(result.helpfulnessScore()).isEqualTo(0.8);
        assertThat(result.factualityScore()).isEqualTo(0.95);
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
}
