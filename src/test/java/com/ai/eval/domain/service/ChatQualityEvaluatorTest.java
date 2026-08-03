package com.ai.eval.application.usecase;

import com.ai.eval.domain.model.ChatEvaluationResult;
import com.ai.eval.domain.model.LlmEvaluationResponse;
import com.ai.eval.domain.model.OfficialGateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatQualityEvaluator")
class ChatQualityEvaluatorTest {

    @Mock
    private OfficialSpringAiEvaluators officialEvaluators;

    @Mock
    private ChatClient evaluationChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ChatQualityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ChatQualityEvaluator(officialEvaluators, evaluationChatClient);
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
            .relevancyPass(true)
            .factualityPass(true)
            .evaluatorFeedback(List.of("relevancy: PASS"))
            .build();

        assertThat(result.coherenceScore()).isEqualTo(0.9);
        assertThat(result.relevancyPass()).isTrue();
        assertThat(result.factualityPass()).isTrue();
        assertThat(result.evaluatorFeedback()).containsExactly("relevancy: PASS");
    }

    @Test
    @DisplayName("should skip factuality when referenceDocuments is empty")
    void should_skipFactuality_when_noReferenceDocuments() {
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
            .thenReturn(new OfficialGateResult(true, null, false, 1.0, null, List.of("relevancy: PASS"), true));
        stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "What is the capital of France?",
            "Paris is the capital of France.",
            List.of()
        );

        assertThat(result.factualityAvailable()).isFalse();
        assertThat(result.factualityScore()).isNull();
        assertThat(result.relevancyPass()).isTrue();
        assertThat(result.factualityPass()).isNull();
        verify(officialEvaluators).evaluate(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("should evaluate factuality when referenceDocuments provided")
    void should_evaluateFactuality_when_referenceDocumentsProvided() {
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
            .thenReturn(new OfficialGateResult(true, true, true, 1.0, 1.0, List.of("relevancy: PASS", "factuality: PASS"), true));
        stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "What is the capital of France?",
            "Paris is the capital of France.",
            List.of("France is a country in Europe. Its capital is Paris.")
        );

        assertThat(result.factualityAvailable()).isTrue();
        assertThat(result.factualityScore()).isEqualTo(1.0);
        assertThat(result.factualityPass()).isTrue();
        assertThat(result.evaluatorFeedback()).isNotEmpty();
    }

    @Test
    @DisplayName("should use fallback when LLM judge returns null")
    void should_useFallback_when_llmJudgeReturnsNull() {
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
            .thenReturn(new OfficialGateResult(true, null, false, 1.0, null, List.of("relevancy: PASS"), true));
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
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
            .thenReturn(new OfficialGateResult(true, null, false, 1.0, null, List.of("relevancy: PASS"), true));
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
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
            .thenReturn(new OfficialGateResult(true, true, true, 1.0, 1.0, List.of(), true));
        stubLlmJudge(new LlmEvaluationResponse(0.8, 0.8, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate(
            "Question",
            "Answer",
            List.of("Reference context")
        );

        assertThat(result.overallScore()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("should suggest relevance fix when relevancy fails")
    void should_suggestRelevanceFix_when_relevancyFails() {
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
            .thenReturn(new OfficialGateResult(false, null, false, 0.0, null, List.of("relevancy: FAIL"), false));
        stubLlmJudge(new LlmEvaluationResponse(0.9, 0.9, false, "", ""));

        ChatEvaluationResult result = evaluator.evaluate("Q", "unrelated", List.of());

        assertThat(result.relevancyPass()).isFalse();
        assertThat(result.suggestions()).contains("Response does not fully address the user's question");
    }

    private void stubLlmJudge(LlmEvaluationResponse response) {
        lenient().when(evaluationChatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        lenient().when(requestSpec.messages(any(Message.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(LlmEvaluationResponse.class), any())).thenReturn(response);
    }
}
