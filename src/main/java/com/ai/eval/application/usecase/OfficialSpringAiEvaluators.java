package com.ai.eval.application.usecase;

import com.ai.eval.domain.model.OfficialGateResult;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around Spring AI official evaluators for Evaluation Testing.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/testing.html">Evaluation Testing</a>
 */
@Service
public class OfficialSpringAiEvaluators {

    private final RelevancyEvaluator relevancyEvaluator;
    private final FactCheckingEvaluator factCheckingEvaluator;

    public OfficialSpringAiEvaluators(
            RelevancyEvaluator relevancyEvaluator,
            FactCheckingEvaluator factCheckingEvaluator) {
        this.relevancyEvaluator = relevancyEvaluator;
        this.factCheckingEvaluator = factCheckingEvaluator;
    }

    public OfficialGateResult evaluate(String userText, String responseContent, List<String> contextTexts) {
        List<Document> documents = toDocuments(contextTexts);
        boolean hasContext = !documents.isEmpty();

        EvaluationResponse relevancy = relevancyEvaluator.evaluate(
                new EvaluationRequest(userText, documents, responseContent));
        boolean relevancyPass = relevancy.isPass();
        double relevanceScore = scoreFrom(relevancy);

        List<String> feedback = new ArrayList<>();
        appendFeedback(feedback, "relevancy", relevancy);

        Boolean factualityPass = null;
        Double factualityScore = null;
        if (hasContext) {
            EvaluationResponse factuality = factCheckingEvaluator.evaluate(
                    new EvaluationRequest(userText, documents, responseContent));
            factualityPass = factuality.isPass();
            factualityScore = scoreFrom(factuality);
            appendFeedback(feedback, "factuality", factuality);
        }

        boolean passed = relevancyPass && (!hasContext || Boolean.TRUE.equals(factualityPass));
        return new OfficialGateResult(
                relevancyPass,
                factualityPass,
                hasContext,
                relevanceScore,
                factualityScore,
                feedback,
                passed);
    }

    private static double scoreFrom(EvaluationResponse response) {
        if (response.getScore() > 0f) {
            return Math.max(0d, Math.min(1d, response.getScore()));
        }
        return response.isPass() ? 1.0 : 0.0;
    }

    private static void appendFeedback(List<String> feedback, String label, EvaluationResponse response) {
        String text = response.getFeedback();
        if (text != null && !text.isBlank()) {
            feedback.add(label + ": " + text.strip());
        } else {
            feedback.add(label + ": " + (response.isPass() ? "PASS" : "FAIL"));
        }
    }

    private static List<Document> toDocuments(List<String> contextTexts) {
        if (contextTexts == null || contextTexts.isEmpty()) {
            return List.of();
        }
        return contextTexts.stream()
                .filter(text -> text != null && !text.isBlank())
                .map(text -> Document.builder().text(text).build())
                .toList();
    }
}
