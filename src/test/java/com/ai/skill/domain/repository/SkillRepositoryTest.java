package com.ai.skill.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.model.SkillId;
import com.ai.testsupport.AbstractDataJpaTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SkillRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parse("c:aaaaaaaa-0000-0000-0000-000000000001");
  private static final OwnerKey OTHER = OwnerKey.parse("c:bbbbbbbb-0000-0000-0000-000000000002");

  @Autowired private SkillRepository repository;

  @Test
  @DisplayName("should reload every field when a skill round trips")
  void shouldReloadEveryFieldWhenASkillRoundTrips() {
    Skill skill = save(OWNER, "Tools", List.of("web_search", "calculator"));
    flushAndClear();

    Skill reloaded = repository.findByIdAndOwnerKey(skill.getId(), OWNER).orElseThrow();

    assertThat(reloaded.getName()).isEqualTo("Tools");
    assertThat(reloaded.getDescription()).isEmpty();
    assertThat(reloaded.getInstructions()).isEqualTo("Instructions");
    assertThat(reloaded.getAllowedTools()).containsExactly("web_search", "calculator");
    assertThat(reloaded.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("should hide another owner's skill when finding by id")
  void shouldHideAnotherOwnersSkillWhenFindingById() {
    Skill skill = save(OWNER, "Private", List.of());
    flushAndClear();

    assertThat(repository.findByIdAndOwnerKey(skill.getId(), OTHER)).isEmpty();
  }

  @Test
  @DisplayName("should list only the owner's skills by name")
  void shouldListOnlyTheOwnersSkillsByName() {
    save(OWNER, "Beta", List.of());
    save(OWNER, "Alpha", List.of());
    save(OTHER, "Other", List.of());
    flushAndClear();

    assertThat(repository.findAllByOwnerKeyOrderByNameAsc(OWNER))
        .extracting(Skill::getName)
        .containsExactly("Alpha", "Beta");
  }

  @Test
  @DisplayName("should match a taken name only within the owner and outside the excluded id")
  void shouldMatchATakenNameOnlyWithinTheOwnerAndOutsideTheExcludedId() {
    final Skill skill = save(OWNER, "Shared", List.of());
    flushAndClear();

    assertThat(repository.existsByOwnerKeyAndNameIgnoringId(OWNER, "Shared", null)).isTrue();
    assertThat(repository.existsByOwnerKeyAndNameIgnoringId(OTHER, "Shared", null)).isFalse();
    assertThat(repository.existsByOwnerKeyAndNameIgnoringId(OWNER, "Shared", skill.getId()))
        .isFalse();
  }

  @Test
  @DisplayName("should delete the skill only for its owner")
  void shouldDeleteTheSkillOnlyForItsOwner() {
    Skill skill = save(OWNER, "Kept", List.of());
    flushAndClear();

    repository.deleteByIdAndOwnerKey(skill.getId(), OTHER);
    flushAndClear();
    assertThat(repository.findByIdAndOwnerKey(skill.getId(), OWNER)).isPresent();

    repository.deleteByIdAndOwnerKey(skill.getId(), OWNER);
    flushAndClear();
    assertThat(repository.findByIdAndOwnerKey(skill.getId(), OWNER)).isEmpty();
  }

  @Test
  @DisplayName("should return only the owner's enabled skills among the ids")
  void shouldReturnOnlyTheOwnersEnabledSkillsAmongTheIds() {
    Skill enabled = save(OWNER, "Enabled", List.of());
    Skill disabled = save(OWNER, "Disabled", List.of());
    disabled.disable();
    Skill foreign = save(OTHER, "Foreign", List.of());
    flushAndClear();

    List<SkillId> ids = List.of(enabled.getId(), disabled.getId(), foreign.getId());

    assertThat(repository.findEnabledByOwnerKeyAndIds(OWNER, ids))
        .extracting(Skill::getName)
        .containsExactly("Enabled");
    assertThat(repository.findEnabledByOwnerKeyAndIds(OWNER, List.of())).isEmpty();
  }

  private Skill save(OwnerKey owner, String name, List<String> allowedTools) {
    return repository.save(Skill.create(owner.value(), name, null, "Instructions", allowedTools));
  }
}
