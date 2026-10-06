package com.ai.pipeline.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PipelineTemplate")
class PipelineTemplateTest {

  private static final String OWNER = "c:11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("should keep worker agent types in order when created")
  void shouldKeepWorkerAgentTypesInOrderWhenCreated() {
    PipelineTemplate template = create(List.of(" Research ", "analyst"));

    assertThat(template.getAgentTypes()).containsExactly("research", "analyst");
  }

  @Test
  @DisplayName("should reject a supervisor node when created")
  void shouldRejectASupervisorNodeWhenCreated() {
    assertThatThrownBy(() -> create(List.of("supervisor", "analyst")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should reject a supervisor node when updated")
  void shouldRejectASupervisorNodeWhenUpdated() {
    PipelineTemplate template = create(List.of("research"));

    assertThatThrownBy(
            () -> template.update("Daily", null, List.of("Supervisor"), null, "Summarize news"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(template.getAgentTypes()).containsExactly("research");
  }

  private static PipelineTemplate create(List<String> agentTypes) {
    return PipelineTemplate.create(OWNER, "Daily", null, agentTypes, null, "Summarize news", null);
  }
}
