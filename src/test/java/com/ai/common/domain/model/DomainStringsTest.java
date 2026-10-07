package com.ai.common.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

  @Test
  @DisplayName("should trim the name when normalizing for a uniqueness check")
  void shouldTrimTheNameWhenNormalizingForAUniquenessCheck() {
    assertEquals("Daily brief", DomainStrings.normalizeName("  Daily brief "));
    assertEquals(null, DomainStrings.normalizeName(null));
  }

  @Test
  @DisplayName("should number copy names after the original name")
  void shouldNumberCopyNamesAfterTheOriginalName() {
    List<String> candidates = DomainStrings.copyNameCandidates(" Digest ", 120).limit(3).toList();

    assertEquals(List.of("Digest", "Digest (2)", "Digest (3)"), candidates);
  }

  @Test
  @DisplayName("should keep copy names within the limit when the base name is long")
  void shouldKeepCopyNamesWithinTheLimitWhenTheBaseNameIsLong() {
    String base = "x".repeat(120);

    assertTrue(DomainStrings.copyNameCandidates(base, 120).allMatch(name -> name.length() <= 120));
    assertTrue(DomainStrings.copyName(base, "abcdef12", 120).endsWith(" (abcdef12)"));
    assertEquals(120, DomainStrings.copyName(base, "abcdef12", 120).length());
  }
}
