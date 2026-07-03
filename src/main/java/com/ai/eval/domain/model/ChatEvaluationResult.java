package com.ai.eval.domain.model;

import java.util.List;

/**
 * Chat evaluation result containing quality scores and safety analysis.
 */
public record ChatEvaluationResult(
    double coherenceScore,
    double relevanceScore,
    double helpfulnessScore,
    double factualityScore,
    double overallScore,
    boolean hasSafetyIssues,
    List<String> safetyFlags,
    List<String> suggestions
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double coherenceScore;
        private double relevanceScore;
        private double helpfulnessScore;
        private double factualityScore;
        private double overallScore;
        private boolean hasSafetyIssues;
        private List<String> safetyFlags = List.of();
        private List<String> suggestions = List.of();

        public Builder coherenceScore(double score) {
            this.coherenceScore = Math.max(0, Math.min(1, score));
            return this;
        }

        public Builder relevanceScore(double score) {
            this.relevanceScore = Math.max(0, Math.min(1, score));
            return this;
        }

        public Builder helpfulnessScore(double score) {
            this.helpfulnessScore = Math.max(0, Math.min(1, score));
            return this;
        }

        public Builder factualityScore(double score) {
            this.factualityScore = Math.max(0, Math.min(1, score));
            return this;
        }

        public Builder overallScore(double score) {
            this.overallScore = Math.max(0, Math.min(1, score));
            return this;
        }

        public Builder hasSafetyIssues(boolean hasIssues) {
            this.hasSafetyIssues = hasIssues;
            return this;
        }

        public Builder safetyFlags(List<String> flags) {
            this.safetyFlags = List.copyOf(flags);
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.suggestions = List.copyOf(suggestions);
            return this;
        }

        public ChatEvaluationResult build() {
            return new ChatEvaluationResult(
                coherenceScore,
                relevanceScore,
                helpfulnessScore,
                factualityScore,
                overallScore,
                hasSafetyIssues,
                safetyFlags,
                suggestions
            );
        }
    }
}
