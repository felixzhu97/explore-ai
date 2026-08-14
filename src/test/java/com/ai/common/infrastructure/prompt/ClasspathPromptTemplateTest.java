package com.ai.common.infrastructure.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClasspathPromptTemplate")
class ClasspathPromptTemplateTest {

  @Test
  @DisplayName("should render placeholder when single variable")
  void shouldRenderPlaceholderWhenSingleVariable() {
    String rendered = ClasspathPromptTemplate.render("Hello {name}", Map.of("name", "world"));

    assertThat(rendered).isEqualTo("Hello world");
  }

  @Test
  @DisplayName("should render rag user template when all variables provided")
  void shouldRenderRagUserTemplateWhenAllVariablesProvided() {
    String rendered =
        ClasspathPromptTemplate.loadAndRender(
            "rag/user-en.st",
            Map.of(
                "style",
                "Keep replies minimal.",
                "context",
                "AI is Artificial Intelligence",
                "question",
                "What is AI?"));

    assertThat(rendered).contains("AI is Artificial Intelligence");
    assertThat(rendered).contains("What is AI?");
    assertThat(rendered).contains("Keep replies minimal.");
  }

  @Test
  @DisplayName("should load static fragment without rendering when json braces present")
  void shouldLoadStaticFragmentWithoutRenderingWhenJsonBracesPresent() {
    String fragment = ClasspathPromptTemplate.load("chat/a2ui-chart.st");

    assertThat(fragment).contains("\"version\": \"v0.9\"");
    assertThatCode(() -> ClasspathPromptTemplate.render("Task: {text}", Map.of("text", "sample")))
        .doesNotThrowAnyException();
  }
}
