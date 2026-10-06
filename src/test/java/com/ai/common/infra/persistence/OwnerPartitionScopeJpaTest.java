package com.ai.common.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.automation.infra.persistence.JpaAutomationRunRepository;
import com.ai.automation.infra.persistence.SpringDataAutomationRunRepository;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.infra.persistence.JpaSkillRepository;
import com.ai.skill.infra.persistence.SpringDataSkillRepository;
import com.ai.testsupport.AbstractDataJpaTest;
import com.ai.testsupport.JpaTestPackages;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@EntityScan(
    basePackages = {"com.ai.skill.domain", "com.ai.automation.domain", JpaTestPackages.COMMON})
@EnableJpaRepositories(
    basePackageClasses = {SpringDataSkillRepository.class, SpringDataAutomationRunRepository.class})
@Import({OwnerPartitionScope.class, JpaSkillRepository.class, JpaAutomationRunRepository.class})
class OwnerPartitionScopeJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_A = "c:aaaaaaaa-0000-0000-0000-000000000001";
  private static final String OWNER_B = "c:bbbbbbbb-0000-0000-0000-000000000002";

  @Autowired private TestEntityManager em;
  @Autowired private OwnerPartitionScope ownerPartition;
  @Autowired private SpringDataSkillRepository skills;
  @Autowired private JpaSkillRepository jpaSkillRepository;
  @Autowired private JpaAutomationRunRepository jpaRunRepository;

  private Skill persistSkill(String ownerKey, String name) {
    Skill skill = Skill.create(ownerKey, name, "Description", "Instructions", List.of());
    skills.saveAndFlush(skill);
    return skill;
  }

  @Test
  @DisplayName("should hide another owner's row when loading by id inside the scope")
  void shouldHideAnotherOwnersRowWhenLoadingByIdInsideTheScope() {
    Skill skill = persistSkill(OWNER_A, "Private");
    em.clear();

    assertThat(ownerPartition.apply(OwnerKey.parse(OWNER_B), () -> skills.findById(skill.getId())))
        .isEmpty();
    assertThat(ownerPartition.apply(OwnerKey.parse(OWNER_A), () -> skills.findById(skill.getId())))
        .isPresent();
  }

  @Test
  @DisplayName("should hide a cached instance of another owner when finding one")
  void shouldHideCachedInstanceOfAnotherOwnerWhenFindingOne() {
    Skill skill = persistSkill(OWNER_A, "Cached");

    assertThat(jpaSkillRepository.findByIdAndOwnerKey(skill.getId(), OWNER_B)).isEmpty();
    assertThat(jpaSkillRepository.findByIdAndOwnerKey(skill.getId(), OWNER_A)).isPresent();
  }

  @Test
  @DisplayName("should list and match names only within the owner partition")
  void shouldListAndMatchNamesOnlyWithinTheOwnerPartition() {
    persistSkill(OWNER_A, "Shared Name");
    persistSkill(OWNER_B, "Other");
    em.clear();

    assertThat(jpaSkillRepository.findAllByOwnerKey(OWNER_A))
        .extracting(Skill::getName)
        .containsExactly("Shared Name");
    assertThat(jpaSkillRepository.existsByOwnerKeyAndNameIgnoringId(OWNER_B, "Shared Name", null))
        .isFalse();
    assertThat(jpaSkillRepository.existsByOwnerKeyAndNameIgnoringId(OWNER_A, "Shared Name", null))
        .isTrue();
  }

  @Test
  @DisplayName("should keep another owner's row when deleting by id")
  void shouldKeepAnotherOwnersRowWhenDeletingById() {
    Skill skill = persistSkill(OWNER_A, "Kept");
    em.clear();

    jpaSkillRepository.deleteByIdAndOwnerKey(skill.getId(), OWNER_B);
    em.flush();
    em.clear();

    assertThat(skills.findById(skill.getId())).isPresent();
  }

  @Test
  @DisplayName("should scope automation runs through the owner keyed run base")
  void shouldScopeAutomationRunsThroughTheOwnerKeyedRunBase() {
    ScheduleId scheduleId = ScheduleId.generate();
    jpaRunRepository.save(AutomationRun.start(scheduleId, OWNER_A));
    em.clear();

    assertThat(jpaRunRepository.findByScheduleIdAndOwnerKey(scheduleId, OWNER_B, 10)).isEmpty();
    assertThat(jpaRunRepository.findByScheduleIdAndOwnerKey(scheduleId, OWNER_A, 10)).hasSize(1);
  }

  @Test
  @DisplayName("should see every owner again when the scope ends")
  void shouldSeeEveryOwnerAgainWhenTheScopeEnds() {
    persistSkill(OWNER_A, "First");
    persistSkill(OWNER_B, "Second");
    em.clear();

    ownerPartition.apply(OwnerKey.parse(OWNER_A), skills::findAll);

    assertThat(skills.findAll()).hasSize(2);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("should reject the scope when no transaction is active")
  void shouldRejectTheScopeWhenNoTransactionIsActive() {
    assertThatThrownBy(() -> ownerPartition.apply(OwnerKey.parse(OWNER_A), skills::findAll))
        .isInstanceOf(IllegalStateException.class);
  }
}
