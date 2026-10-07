package com.ai.pipeline.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import com.ai.pipeline.domain.model.CustomAgent;
import com.ai.testsupport.AbstractDataJpaTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CustomAgentRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parseKey("c:66666666-6666-6666-6666-666666666666");
  private static final OwnerKey OTHER = OwnerKey.parseKey("c:77777777-7777-7777-7777-777777777777");

  @Autowired private CustomAgentRepository repository;

  @Test
  @DisplayName("should reload every field when a custom agent round trips")
  void shouldReloadEveryFieldWhenACustomAgentRoundTrips() {
    CustomAgent agent = save(OWNER, "researcher", "Research Agent", List.of("draft", "edit"));
    flushAndClear();

    CustomAgent reloaded = repository.findByIdAndOwnerKey(agent.getId(), OWNER).orElseThrow();

    assertThat(reloaded.getAgentType()).isEqualTo("researcher");
    assertThat(reloaded.getName()).isEqualTo("Research Agent");
    assertThat(reloaded.getTools()).containsExactly("draft", "edit");
    assertThat(reloaded.isEnabled()).isTrue();
    assertThat(repository.findByIdAndOwnerKey(agent.getId(), OTHER)).isEmpty();
  }

  @Test
  @DisplayName("should list the owner's agents by name and the enabled ones separately")
  void shouldListTheOwnersAgentsByNameAndTheEnabledOnesSeparately() {
    save(OWNER, "beta", "Beta Agent", List.of());
    save(OWNER, "alpha", "Alpha Agent", List.of()).disable();
    save(OTHER, "gamma", "Gamma Agent", List.of());
    flushAndClear();

    assertThat(repository.findAllByOwnerKeyOrderByNameAsc(OWNER))
        .extracting(CustomAgent::getName)
        .containsExactly("Alpha Agent", "Beta Agent");
    assertThat(repository.findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OWNER))
        .extracting(CustomAgent::getName)
        .containsExactly("Beta Agent");
  }

  @Test
  @DisplayName("should match a taken type key only within the owner and outside the excluded id")
  void shouldMatchATakenAgentTypeOnlyWithinTheOwnerAndOutsideTheExcludedId() {
    final CustomAgent agent = save(OWNER, "writer", "Writer", List.of());
    flushAndClear();

    assertThat(repository.existsByOwnerKeyAndAgentTypeIgnoringId(OWNER, "writer", null)).isTrue();
    assertThat(repository.existsByOwnerKeyAndAgentTypeIgnoringId(OTHER, "writer", null)).isFalse();
    assertThat(repository.existsByOwnerKeyAndAgentTypeIgnoringId(OWNER, "writer", agent.getId()))
        .isFalse();
  }

  private CustomAgent save(OwnerKey owner, String agentType, String name, List<String> tools) {
    return repository.save(
        CustomAgent.createAgent(owner.getValue(), agentType, name, "Description", "Prompt", tools));
  }
}
