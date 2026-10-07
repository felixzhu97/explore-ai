package com.ai.chat.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SessionTitle")
class SessionTitleTest {

  @Test
  @DisplayName("should fall back to the default title when the text is blank")
  void shouldFallBackToTheDefaultTitleWhenTheTextIsBlank() {
    assertThat(SessionTitle.of(null).isDefault()).isTrue();
    assertThat(SessionTitle.of("  ").value()).isEqualTo("New Chat");
    assertThat(SessionTitle.fromFirstMessage("\n").isDefault()).isTrue();
  }

  @Test
  @DisplayName("should cut a typed title to one hundred characters")
  void shouldCutATypedTitleToOneHundredCharacters() {
    assertThat(SessionTitle.of("A".repeat(150)).value()).hasSize(SessionTitle.MAX_LENGTH);
  }

  @Test
  @DisplayName("should keep a title from the first message on one short line")
  void shouldKeepATitleFromTheFirstMessageOnOneShortLine() {
    SessionTitle title = SessionTitle.fromFirstMessage("How do I\n  deploy " + "K8s ".repeat(20));

    assertThat(title.value()).startsWith("How do I deploy K8s").doesNotContain("\n");
    assertThat(title.value().length()).isLessThanOrEqualTo(SessionTitle.MAX_DERIVED_LENGTH);
  }

  @Test
  @DisplayName("should strip wrapping quotes when the model writes the title")
  void shouldStripWrappingQuotesWhenTheModelWritesTheTitle() {
    assertThat(SessionTitle.generated(" \"Kubernetes 部署指南\" ").value())
        .isEqualTo("Kubernetes 部署指南");
    assertThat(SessionTitle.generated("\"\"").isDefault()).isTrue();
  }
}
