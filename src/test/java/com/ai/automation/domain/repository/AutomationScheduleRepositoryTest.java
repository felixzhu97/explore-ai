package com.ai.automation.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.common.domain.model.OwnerKey;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

class AutomationScheduleRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parseKey("c:44444444-4444-4444-4444-444444444444");
  private static final OwnerKey OTHER = OwnerKey.parseKey("c:55555555-5555-5555-5555-555555555555");
  private static final String WORKFLOW_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
  private static final Instant JAN_1 = Instant.parse("2026-01-01T00:00:00Z");

  @Autowired private AutomationScheduleRepository repository;

  @Test
  @DisplayName("should let only one caller claim the next run")
  void shouldLetOnlyOneCallerClaimTheNextRun() {
    AutomationSchedule schedule = save(OWNER, "Claimed", "0 0 9 * * *", JAN_1);
    flushAndClear();
    Instant nextRun = schedule.getNextRunAt();
    Instant claimedNextRunAt = nextRun.plusSeconds(86_400);

    boolean first = repository.claimSchedule(schedule.getId(), nextRun, claimedNextRunAt);
    boolean second = repository.claimSchedule(schedule.getId(), nextRun, claimedNextRunAt);
    flushAndClear();

    AutomationSchedule reloaded = repository.findByIdAndOwnerKey(schedule.getId(), OWNER).get();
    assertThat(first).isTrue();
    assertThat(second).isFalse();
    assertThat(reloaded.getNextRunAt()).isEqualTo(claimedNextRunAt);
  }

  @Test
  @DisplayName("should save the finished run of a schedule claimed in the same transaction")
  void shouldSaveTheFinishedRunOfAScheduleClaimedInTheSameTransaction() {
    save(OWNER, "Daily", "0 0 9 * * *", JAN_1);
    flushAndClear();
    final Instant now = Instant.parse("2026-01-02T00:00:00Z");
    AutomationSchedule due = repository.findDue(now, Limit.of(1)).getFirst();

    assertThat(
            repository.claimSchedule(
                due.getId(), due.getNextRunAt(), due.calculateClaimedNextRunAt(now)))
        .isTrue();
    due.completeRun(now);
    repository.save(due);
    flushAndClear();

    assertThat(repository.findByIdAndOwnerKey(due.getId(), OWNER).get().getLastRunAt())
        .isEqualTo(now);
  }

  @Test
  @DisplayName("should find enabled due schedules of every owner soonest first up to the limit")
  void shouldFindEnabledDueSchedulesOfEveryOwnerSoonestFirstUpToTheLimit() {
    save(OWNER, "Ten", "0 0 10 * * *", JAN_1);
    save(OTHER, "Nine", "0 0 9 * * *", JAN_1);
    save(OWNER, "Eleven", "0 0 11 * * *", JAN_1);
    save(OWNER, "Off", "0 0 8 * * *", JAN_1).disable();
    save(OWNER, "Later", "0 0 9 * * *", Instant.parse("2099-01-01T00:00:00Z"));
    flushAndClear();

    assertThat(repository.findDue(Instant.parse("2026-01-02T00:00:00Z"), Limit.of(2)))
        .extracting(AutomationSchedule::getName)
        .containsExactly("Nine", "Ten");
  }

  @Test
  @DisplayName("should list and count only the owner's schedules newest first")
  void shouldListAndCountOnlyTheOwnersSchedulesNewestFirst() {
    save(OWNER, "First", "0 0 8 * * *", JAN_1);
    em.flush();
    save(OWNER, "Second", "0 0 9 * * *", JAN_1);
    save(OTHER, "Other", "0 0 9 * * *", JAN_1);
    flushAndClear();

    assertThat(repository.findAllByOwnerKeyOrderByCreatedAtDesc(OWNER))
        .extracting(AutomationSchedule::getName)
        .containsExactly("Second", "First");
    assertThat(repository.countByOwnerKey(OWNER)).isEqualTo(2);
  }

  private AutomationSchedule save(OwnerKey owner, String name, String cron, Instant now) {
    return repository.save(
        AutomationSchedule.createSchedule(
            owner.value(), name, cron, "UTC", WORKFLOW_ID, "user@example.com", "Brief", now));
  }
}
