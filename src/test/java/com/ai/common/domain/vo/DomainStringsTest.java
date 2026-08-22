package com.ai.common.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainStringsTest {

  @Test
  @DisplayName("should truncate description to max length when normalizing")
  void shouldTruncateDescriptionWhenNormalizing() {
    String longText = "x".repeat(600);
    assertEquals(500, DomainStrings.normalizeDescription(longText).length());
  }

  @Test
  @DisplayName("should reject blank name when requiring name")
  void shouldRejectBlankNameWhenRequiringName() {
    assertThrows(IllegalArgumentException.class, () -> DomainStrings.requireName(" "));
  }
}
