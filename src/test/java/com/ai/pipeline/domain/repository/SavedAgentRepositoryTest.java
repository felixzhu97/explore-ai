package com.ai.pipeline.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.testsupport.AbstractDataJpaTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SavedAgentRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parse("c:66666666-6666-6666-6666-666666666666");
  private static final OwnerKey OTHER = OwnerKey.parse("c:77777777-7777-7777-7777-777777777777");

  @Autowired private SavedAgentRepository repository;

  @Test
  @DisplayName("should reload every field when a saved agent round trips")
  void shouldReloadEveryFieldWhenASavedAgentRoundTrips() {
    SavedAgent agent = save(OWNER, "researcher", "Research Agent", List.of("draft", "edit"));
    flushAndClear();

    SavedAgent reloaded = repository.findByIdAndOwnerKey(agent.getId(), OWNER).orElseThrow();

    assertThat(reloaded.getTypeKey()).isEqualTo("researcher");
    assertThat(reloaded.getName()).isEqualTo("Research Agent");
    assertThat(reloaded.getToolKeys()).containsExactly("draft", "edit");
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
        .extracting(SavedAgent::getName)
        .containsExactly("Alpha Agent", "Beta Agent");
    assertThat(repository.findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OWNER))
        .extracting(SavedAgent::getName)
        .containsExactly("Beta Agent");
  }

  @Test
  @DisplayName("should match a taken type key only within the owner and outside the excluded id")
  void shouldMatchATakenTypeKeyOnlyWithinTheOwnerAndOutsideTheExcludedId() {
    final SavedAgent agent = save(OWNER, "writer", "Writer", List.of());
    flushAndClear();

    assertThat(repository.existsByOwnerKeyAndTypeKeyIgnoringId(OWNER, "writer", null)).isTrue();
    assertThat(repository.existsByOwnerKeyAndTypeKeyIgnoringId(OTHER, "writer", null)).isFalse();
    assertThat(repository.existsByOwnerKeyAndTypeKeyIgnoringId(OWNER, "writer", agent.getId()))
        .isFalse();
  }

  private SavedAgent save(OwnerKey owner, String typeKey, String name, List<String> toolKeys) {
    return repository.save(
        SavedAgent.create(owner.value(), typeKey, name, "Description", "Prompt", toolKeys));
  }
}
