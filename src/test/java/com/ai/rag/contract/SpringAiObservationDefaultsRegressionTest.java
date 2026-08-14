package com.ai.rag.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@DisplayName("AI-239 Spring AI observation defaults regression")
class SpringAiObservationDefaultsRegressionTest {

  @Test
  @DisplayName("should disable prompt and completion plaintext when default application yml")
  void shouldDisablePromptAndCompletionPlaintextWhenDefaultApplicationYml() throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
      assertThat(in).isNotNull();
      @SuppressWarnings("unchecked")
      Map<String, Object> root =
          new Yaml().load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
      @SuppressWarnings("unchecked")
      Map<String, Object> spring = (Map<String, Object>) root.get("spring");
      @SuppressWarnings("unchecked")
      Map<String, Object> ai = (Map<String, Object>) spring.get("ai");
      @SuppressWarnings("unchecked")
      Map<String, Object> chat = (Map<String, Object>) ai.get("chat");
      @SuppressWarnings("unchecked")
      Map<String, Object> clientObservations =
          (Map<String, Object>) ((Map<String, Object>) chat.get("client")).get("observations");
      @SuppressWarnings("unchecked")
      Map<String, Object> chatObservations = (Map<String, Object>) chat.get("observations");
      assertThat(clientObservations.get("log-prompt")).isEqualTo(false);
      assertThat(chatObservations.get("log-prompt")).isEqualTo(false);
      assertThat(chatObservations.get("include-completion")).isEqualTo(false);
    }
  }
}
