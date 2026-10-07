package com.ai.textanalysis.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Sentiment")
class SentimentTest {

  @Test
  @DisplayName("should parse known sentiment strings")
  void shouldParseKnownSentimentStrings() {
    assertThat(Sentiment.parseSentiment("positive")).isEqualTo(Sentiment.POSITIVE);
    assertThat(Sentiment.parseSentiment("NEGATIVE")).isEqualTo(Sentiment.NEGATIVE);
  }

  @Test
  @DisplayName("should default to neutral for unknown values")
  void shouldDefaultToNeutralForUnknownValues() {
    assertThat(Sentiment.parseSentiment("unknown")).isEqualTo(Sentiment.NEUTRAL);
    assertThat(Sentiment.parseSentiment(null)).isEqualTo(Sentiment.NEUTRAL);
  }

  @Test
  @DisplayName("should expose sentiment predicates")
  void shouldExposeSentimentPredicates() {
    assertThat(Sentiment.NEGATIVE.isNegative()).isTrue();
    assertThat(Sentiment.POSITIVE.isPositive()).isTrue();
  }
}
