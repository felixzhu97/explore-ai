package com.ai.skill.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.skill.domain.model.Skill;
import com.ai.testsupport.AbstractDataJpaTest;
import com.ai.testsupport.JpaTestPackages;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.skill.domain", JpaTestPackages.COMMON})
@EnableJpaRepositories(basePackageClasses = SpringDataSkillRepository.class)
@Import({JpaSkillRepository.class, OwnerPartitionScope.class})
class SkillJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:22222222-2222-2222-2222-222222222222";

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataSkillRepository repository;
  @Autowired private JpaSkillRepository adapter;

  @Test
  @DisplayName("should persist and reload skill when round tripping")
  void shouldPersistAndReloadSkillWhenRoundTripping() {
    Skill skill =
        Skill.create(OWNER_KEY, "Summarize", "Short summary skill", "Summarize text", List.of());

    repository.saveAndFlush(skill);
    em.clear();

    Optional<Skill> reloaded = repository.findById(skill.getId());

    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo("Summarize");
    assertThat(reloaded.get().getInstructions()).isEqualTo("Summarize text");
    assertThat(reloaded.get().isEnabled()).isTrue();
  }

  @Test
  @DisplayName("should store allowed tools as json when persisting skill")
  void shouldStoreAllowedToolsAsJsonWhenPersistingSkill() {
    Skill skill =
        Skill.create(
            OWNER_KEY,
            "Tools",
            "Uses tools",
            "Use allowed tools",
            List.of("web_search", "calculator"));

    repository.saveAndFlush(skill);
    em.clear();

    Skill reloaded = repository.findById(skill.getId()).orElseThrow();

    assertThat(reloaded.getAllowedTools()).containsExactly("web_search", "calculator");
  }

  @Test
  @DisplayName("should find skill by id and owner key when scoped lookup")
  void shouldFindSkillByIdAndOwnerKeyWhenScopedLookup() {
    Skill skill = Skill.create(OWNER_KEY, "Scoped", "desc", "instructions", List.of());
    repository.saveAndFlush(skill);
    em.clear();

    Optional<Skill> found = adapter.findByIdAndOwnerKey(skill.getId(), OWNER_KEY);

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Scoped");
  }

  @Test
  @DisplayName("should list skills by owner ordered by name ascending")
  void shouldListSkillsByOwnerOrderedByNameAscending() {
    Skill beta = Skill.create(OWNER_KEY, "Beta", "desc", "instructions", List.of());
    Skill alpha = Skill.create(OWNER_KEY, "Alpha", "desc", "instructions", List.of());
    repository.saveAndFlush(beta);
    repository.saveAndFlush(alpha);
    em.clear();

    List<Skill> skills = adapter.findAllByOwnerKey(OWNER_KEY);

    assertThat(skills).extracting(Skill::getName).containsExactly("Alpha", "Beta");
  }

  @Test
  @DisplayName("should report name exists for owner when checking duplicate")
  void shouldReportNameExistsForOwnerWhenCheckingDuplicate() {
    Skill skill = Skill.create(OWNER_KEY, "Unique", "desc", "instructions", List.of());
    repository.saveAndFlush(skill);
    em.clear();

    assertThat(adapter.existsByOwnerKeyAndNameIgnoringId(OWNER_KEY, "Unique", null)).isTrue();
    assertThat(adapter.existsByOwnerKeyAndNameIgnoringId(OWNER_KEY, "Missing", null)).isFalse();
  }
}
