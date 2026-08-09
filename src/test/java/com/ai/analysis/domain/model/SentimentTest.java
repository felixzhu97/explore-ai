package com.ai.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sentiment")
class SentimentTest {

    @Test
    @DisplayName("should parse known sentiment strings")
    void shouldParseKnownSentimentStrings() {
        assertThat(Sentiment.fromString("positive")).isEqualTo(Sentiment.POSITIVE);
        assertThat(Sentiment.fromString("NEGATIVE")).isEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    @DisplayName("should default to neutral for unknown values")
    void shouldDefaultToNeutralForUnknownValues() {
        assertThat(Sentiment.fromString("unknown")).isEqualTo(Sentiment.NEUTRAL);
        assertThat(Sentiment.fromString(null)).isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    @DisplayName("should expose sentiment predicates")
    void shouldExposeSentimentPredicates() {
        assertThat(Sentiment.NEGATIVE.isNegative()).isTrue();
        assertThat(Sentiment.POSITIVE.isPositive()).isTrue();
    }
}
