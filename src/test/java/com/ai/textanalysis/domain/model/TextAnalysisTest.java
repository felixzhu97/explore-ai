package com.ai.textanalysis.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TextAnalysis")
class TextAnalysisTest {

  @Test
  @DisplayName("should filter null elements from key points and entities")
  void shouldFilterNullElementsFromKeyPointsAndEntities() {
    TextAnalysis analysis =
        TextAnalysis.createAnalysis(
            "s", Sentiment.NEUTRAL, Arrays.asList("a", null, "b"), Arrays.asList(null, "e"), "en");

    assertThat(analysis.getKeyPoints()).containsExactly("a", "b");
    assertThat(analysis.getEntities()).containsExactly("e");
  }

  @Test
  @DisplayName("should return immutable entity lists")
  void shouldReturnImmutableEntityLists() {
    TextAnalysis analysis =
        TextAnalysis.createAnalysis("s", Sentiment.NEUTRAL, List.of("k"), List.of("e"), "en");

    assertThat(analysis.getKeyPoints()).containsExactly("k");
    assertThat(analysis.getEntities()).containsExactly("e");
  }
}
