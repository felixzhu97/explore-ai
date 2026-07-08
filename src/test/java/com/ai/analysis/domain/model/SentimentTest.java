package com.ai.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sentiment")
class SentimentTest {

    @Test
    @DisplayName("should parse known sentiment strings")
    void should_parse_known_sentiment_strings() {
        assertThat(Sentiment.fromString("positive")).isEqualTo(Sentiment.POSITIVE);
        assertThat(Sentiment.fromString("NEGATIVE")).isEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    @DisplayName("should default to neutral for unknown values")
    void should_default_to_neutral_for_unknown_values() {
        assertThat(Sentiment.fromString("unknown")).isEqualTo(Sentiment.NEUTRAL);
        assertThat(Sentiment.fromString(null)).isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    @DisplayName("should expose sentiment predicates")
    void should_expose_sentiment_predicates() {
        assertThat(Sentiment.NEGATIVE.isNegative()).isTrue();
        assertThat(Sentiment.POSITIVE.isPositive()).isTrue();
    }
}
