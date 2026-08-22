package com.ai.automation.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.domain.vo.RunStatus;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(
    basePackages = {"com.ai.automation.domain", "com.ai.base.domain", "com.ai.common.domain"})
@EnableJpaRepositories(
    basePackageClasses = {
      SpringDataAutomationScheduleRepository.class,
      SpringDataAutomationRunRepository.class
    })
class AutomationJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:44444444-4444-4444-4444-444444444444";
  private static final OwnerKey OWNER = OwnerKey.parse(OWNER_KEY);
  private static final String WORKFLOW_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataAutomationScheduleRepository scheduleRepository;
  @Autowired private SpringDataAutomationRunRepository runRepository;

  @Test
  @DisplayName("should persist and reload automation schedule when round tripping")
  void shouldPersistAndReloadAutomationScheduleWhenRoundTripping() {
    Instant nextRun = Instant.now().plusSeconds(3600);
    AutomationSchedule schedule =
        AutomationSchedule.create(
            OWNER_KEY,
            "Daily digest",
            "0 9 * * *",
            "UTC",
            WORKFLOW_ID,
            "user@example.com",
            "Send daily summary",
            nextRun);

    scheduleRepository.saveAndFlush(schedule);
    em.clear();

    AutomationSchedule reloaded = scheduleRepository.findById(schedule.getId()).orElseThrow();

    assertThat(reloaded.getName()).isEqualTo("Daily digest");
    assertThat(reloaded.getClientId()).isEqualTo(OWNER_KEY);
    assertThat(reloaded.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("should persist and reload automation run with schedule id embeddable")
  void shouldPersistAndReloadAutomationRunWithScheduleIdEmbeddable() {
    ScheduleId scheduleId = ScheduleId.generate();
    AutomationRun run = AutomationRun.start(scheduleId, OWNER_KEY);
    run.succeed("result excerpt", EmailDeliveryStatus.SENT);

    runRepository.saveAndFlush(run);
    em.clear();

    AutomationRun reloaded = runRepository.findById(run.getId()).orElseThrow();

    assertThat(reloaded.getScheduleId()).isEqualTo(scheduleId);
    assertThat(reloaded.getStatus()).isEqualTo(RunStatus.SUCCESS);
    assertThat(reloaded.getResultExcerpt()).isEqualTo("result excerpt");
  }

  @Test
  @DisplayName("should store owner key on run via converter when persisting")
  void shouldStoreOwnerKeyOnRunViaConverterWhenPersisting() {
    AutomationRun run = AutomationRun.start(ScheduleId.generate(), OWNER_KEY);

    runRepository.saveAndFlush(run);
    em.clear();

    String rawOwnerKey =
        (String)
            em.getEntityManager()
                .createNativeQuery("SELECT owner_key FROM automation_runs WHERE id = ?")
                .setParameter(1, run.getId().value())
                .getSingleResult();

    assertThat(rawOwnerKey).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should list schedules by owner ordered by created at descending")
  void shouldListSchedulesByOwnerOrderedByCreatedAtDescending() {
    Instant nextRun = Instant.now().plusSeconds(7200);
    AutomationSchedule first =
        AutomationSchedule.create(
            OWNER_KEY,
            "First",
            "0 8 * * *",
            "UTC",
            WORKFLOW_ID,
            "first@example.com",
            "First brief",
            nextRun);
    AutomationSchedule second =
        AutomationSchedule.create(
            OWNER_KEY,
            "Second",
            "0 10 * * *",
            "UTC",
            WORKFLOW_ID,
            "second@example.com",
            "Second brief",
            nextRun.plusSeconds(60));
    scheduleRepository.saveAndFlush(first);
    scheduleRepository.saveAndFlush(second);
    em.clear();

    List<AutomationSchedule> schedules =
        scheduleRepository.findAllByOwnerKeyOrderByCreatedAtDesc(OWNER);

    assertThat(schedules).hasSize(2);
    assertThat(schedules.getFirst().getCreatedAt())
        .isAfterOrEqualTo(schedules.get(1).getCreatedAt());
  }

  @Test
  @DisplayName("should list runs by schedule and owner ordered by started at descending")
  void shouldListRunsByScheduleAndOwnerOrderedByStartedAtDescending() {
    ScheduleId scheduleId = ScheduleId.generate();
    Instant earlier = Instant.parse("2026-01-01T10:00:00Z");
    Instant later = Instant.parse("2026-01-02T10:00:00Z");
    AutomationRun olderRun =
        AutomationRun.restore(
            com.ai.automation.domain.vo.RunId.generate(),
            scheduleId,
            OWNER_KEY,
            earlier,
            earlier.plusSeconds(30),
            RunStatus.SUCCESS,
            null,
            "older",
            EmailDeliveryStatus.SENT);
    AutomationRun newerRun =
        AutomationRun.restore(
            com.ai.automation.domain.vo.RunId.generate(),
            scheduleId,
            OWNER_KEY,
            later,
            later.plusSeconds(30),
            RunStatus.SUCCESS,
            null,
            "newer",
            EmailDeliveryStatus.SENT);
    runRepository.saveAndFlush(olderRun);
    runRepository.saveAndFlush(newerRun);
    em.clear();

    List<AutomationRun> runs =
        runRepository.findByScheduleIdAndOwnerKeyOrderByStartedAtDesc(
            scheduleId, OWNER, PageRequest.of(0, 10));

    assertThat(runs).extracting(AutomationRun::getResultExcerpt).containsExactly("newer", "older");
  }
}
