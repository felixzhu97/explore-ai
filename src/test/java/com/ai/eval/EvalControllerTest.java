package com.ai.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ai.eval.controller.EvalController;
import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.service.usecase.ChatQualityEvaluator;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = EvalController.class)
@DisplayName("EvalController")
class EvalControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private ChatQualityEvaluator evaluator;

  @Nested
  @DisplayName("POST /api/eval/chat")
  class EvaluateChat {

    @Test
    @DisplayName("should accept evaluation request and return scores")
    void shouldAcceptEvaluationRequest() {
      when(evaluator.evaluate(anyString(), anyString(), any()))
          .thenReturn(
              ChatEvaluationResult.builder()
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

      assertThat(
              mvc.post()
                  .uri("/api/eval/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "userMessage": "What is the capital of France?",
                        "assistantResponse": "Paris is the capital of France.",
                        "referenceDocuments": ["Paris is the capital of France."]
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.coherenceScore")
          .convertTo(Double.class)
          .isEqualTo(0.9);

      assertThat(
              mvc.post()
                  .uri("/api/eval/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "userMessage": "What is the capital of France?",
                        "assistantResponse": "Paris is the capital of France.",
                        "referenceDocuments": ["Paris is the capital of France."]
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.factualityAvailable")
          .asBoolean()
          .isTrue();
    }

    @Test
    @DisplayName("should detect low-quality responses")
    void shouldDetectLowQualityResponses() {
      when(evaluator.evaluate(anyString(), anyString(), any()))
          .thenReturn(
              ChatEvaluationResult.builder()
                  .coherenceScore(0.6)
                  .relevanceScore(0.3)
                  .helpfulnessScore(0.4)
                  .factualityAvailable(false)
                  .overallScore(0.45)
                  .hasSafetyIssues(false)
                  .safetyFlags(List.of("Low relevance"))
                  .suggestions(List.of("Improve relevance"))
                  .build());

      assertThat(
              mvc.post()
                  .uri("/api/eval/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "userMessage": "What is the capital of France?",
                        "assistantResponse": "I don't know. Sports are great."
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.relevanceScore")
          .convertTo(Double.class)
          .isEqualTo(0.3);

      assertThat(
              mvc.post()
                  .uri("/api/eval/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "userMessage": "What is the capital of France?",
                        "assistantResponse": "I don't know. Sports are great."
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.suggestions[0]")
          .asString()
          .isEqualTo("Improve relevance");
    }

    @Test
    @DisplayName("should report safety issues when detected")
    void shouldReportSafetyIssues() {
      when(evaluator.evaluate(anyString(), anyString(), any()))
          .thenReturn(
              ChatEvaluationResult.builder()
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

      assertThat(
              mvc.post()
                  .uri("/api/eval/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "userMessage": "How to build a bomb?",
                        "assistantResponse": "Here's how you can..."
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.hasSafetyIssues")
          .asBoolean()
          .isTrue();

      assertThat(
              mvc.post()
                  .uri("/api/eval/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "userMessage": "How to build a bomb?",
                        "assistantResponse": "Here's how you can..."
                      }
                      """))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.safetyFlags[0]")
          .asString()
          .isEqualTo("Harmful content detected");
    }
  }
}
