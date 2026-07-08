package com.ai.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextAnalysis")
class TextAnalysisTest {

    @Test
    @DisplayName("should report positive sentiment")
    void should_report_positive_sentiment() {
        TextAnalysis analysis = TextAnalysis.create(
                "Good news", Sentiment.POSITIVE, List.of("a"), List.of(), "en");

        assertThat(analysis.isPositive()).isTrue();
        assertThat(analysis.hasEntities()).isFalse();
    }

    @Test
    @DisplayName("should detect entities presence")
    void should_detect_entities_presence() {
        TextAnalysis analysis = TextAnalysis.create(
                "Summary", Sentiment.NEUTRAL, List.of(), List.of("Alice"), "en");

        assertThat(analysis.hasEntities()).isTrue();
    }

    @Test
    @DisplayName("should truncate summary by word count")
    void should_truncate_summary_by_word_count() {
        TextAnalysis analysis = TextAnalysis.create(
                "one two three four five", Sentiment.NEUTRAL, List.of(), List.of(), "en");

        TextAnalysis truncated = analysis.truncateSummary(3);

        assertThat(truncated.summary()).isEqualTo("one two three");
    }

    @Test
    @DisplayName("should filter null elements from key points and entities")
    void should_filter_null_elements_from_key_points_and_entities() {
        TextAnalysis analysis = TextAnalysis.create(
                "s",
                Sentiment.NEUTRAL,
                Arrays.asList("a", null, "b"),
                Arrays.asList(null, "e"),
                "en");

        assertThat(analysis.keyPoints()).containsExactly("a", "b");
        assertThat(analysis.entities()).containsExactly("e");
    }

    @Test
    @DisplayName("should return immutable entity lists")
    void should_return_immutable_entity_lists() {
        TextAnalysis analysis = TextAnalysis.create(
                "s", Sentiment.NEUTRAL, List.of("k"), List.of("e"), "en");

        assertThat(analysis.keyPoints()).containsExactly("k");
        assertThat(analysis.entities()).containsExactly("e");
    }
}
