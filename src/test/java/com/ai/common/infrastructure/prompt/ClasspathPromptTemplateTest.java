package com.ai.common.infrastructure.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ClasspathPromptTemplate")
class ClasspathPromptTemplateTest {

    @Test
    @DisplayName("should_renderPlaceholder_when_singleVariable")
    void should_renderPlaceholder_when_singleVariable() {
        String rendered = ClasspathPromptTemplate.render("Hello {name}", Map.of("name", "world"));

        assertThat(rendered).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("should_renderRagUserTemplate_when_allVariablesProvided")
    void should_renderRagUserTemplate_when_allVariablesProvided() {
        String rendered = ClasspathPromptTemplate.loadAndRender(
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
    @DisplayName("should_loadStaticFragmentWithoutRendering_when_jsonBracesPresent")
    void should_loadStaticFragmentWithoutRendering_when_jsonBracesPresent() {
        String fragment = ClasspathPromptTemplate.load("chat/a2ui-chart.st");

        assertThat(fragment).contains("\"version\": \"v0.9\"");
        assertThatCode(() -> ClasspathPromptTemplate.render("Task: {text}", Map.of("text", "sample")))
                .doesNotThrowAnyException();
    }
}
