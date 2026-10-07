package com.ai.eval.infra.golden;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.model.GoldenEvalCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

@DisplayName("ClasspathGoldenSuiteLoader")
class ClasspathGoldenSuiteLoaderTest {

  private final ClasspathGoldenSuiteLoader loader =
      new ClasspathGoldenSuiteLoader(new ObjectMapper());

  @Test
  @DisplayName("should parse string input and ideal when openai evals shape")
  void shouldParseStringInputAndIdealWhenOpenaiEvalsShape() throws Exception {
    String jsonl =
        "{\"id\":\"c1\",\"input\":\"What is Explore AI?\","
            + "\"ideal\":\"A demo platform.\","
            + "\"metadata\":{\"category\":\"CHAT\",\"tools_enabled\":false}}\n";
    List<GoldenEvalCase> cases = loader.readResource(resource(jsonl));

    assertThat(cases).hasSize(1);
    GoldenEvalCase evalCase = cases.getFirst();
    assertThat(evalCase.getId()).isEqualTo("c1");
    assertThat(evalCase.getCategory()).isEqualTo(GoldenEvalCategory.CHAT);
    assertThat(evalCase.getUserText()).isEqualTo("What is Explore AI?");
    assertThat(evalCase.getIdeal()).containsExactly("A demo platform.");
    assertThat(evalCase.isToolsEnabled()).isFalse();
  }

  @Test
  @DisplayName("should parse chat format input and ideal array when present")
  void shouldParseChatFormatInputAndIdealArrayWhenPresent() throws Exception {
    String jsonl =
        "{\"id\":\"r1\",\"input\":[{\"role\":\"user\",\"content\":\"Which modules?\"}],"
            + "\"ideal\":[\"Chat\",\"RAG\"],"
            + "\"metadata\":{\"category\":\"RAG\",\"fixture_keys\":[\"overview\"],"
            + "\"contexts\":[\"Chat and RAG\"]}}\n";
    List<GoldenEvalCase> cases = loader.readResource(resource(jsonl));

    assertThat(cases).hasSize(1);
    GoldenEvalCase evalCase = cases.getFirst();
    assertThat(evalCase.getCategory()).isEqualTo(GoldenEvalCategory.RAG);
    assertThat(evalCase.getUserText()).isEqualTo("Which modules?");
    assertThat(evalCase.getIdeal()).containsExactly("Chat", "RAG");
    assertThat(evalCase.getFixtureKeys()).containsExactly("overview");
    assertThat(evalCase.getContexts()).containsExactly("Chat and RAG");
  }

  @Test
  @DisplayName("should reject line when ideal missing")
  void shouldRejectLineWhenIdealMissing() {
    String jsonl = "{\"id\":\"bad\",\"input\":\"hi\"}\n";
    assertThatThrownBy(() -> loader.readResource(resource(jsonl)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ideal");
  }

  private static ByteArrayResource resource(String jsonl) {
    return new ByteArrayResource(jsonl.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return "sample.jsonl";
      }
    };
  }
}
