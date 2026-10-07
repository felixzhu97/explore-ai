package com.ai.automation.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.EmailDeliveryStatus;
import com.ai.automation.domain.model.RunStatus;
import com.ai.automation.domain.model.ScheduleId;
import com.ai.common.domain.model.OwnerKey;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

class AutomationRunRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parse("c:44444444-4444-4444-4444-444444444444");
  private static final OwnerKey OTHER = OwnerKey.parse("c:55555555-5555-5555-5555-555555555555");

  @Autowired private AutomationRunRepository repository;

  @Test
  @DisplayName("should insert a new run as the same managed instance")
  void shouldInsertANewRunAsTheSameManagedInstance() {
    AutomationRun run = AutomationRun.start(ScheduleId.generate(), OWNER.value());
    run.skipForQuota();

    AutomationRun saved = repository.save(run);

    assertThat(saved).isSameAs(run);
    assertThat(em.getEntityManager().contains(run)).isTrue();
  }

  @Test
  @DisplayName("should list the owner's newest runs of the schedule up to the limit")
  void shouldListTheOwnersNewestRunsOfTheScheduleUpToTheLimit() {
    ScheduleId scheduleId = ScheduleId.generate();
    save(scheduleId, OWNER, "2026-01-01T10:00:00Z", "oldest");
    save(scheduleId, OWNER, "2026-01-03T10:00:00Z", "newest");
    save(scheduleId, OWNER, "2026-01-02T10:00:00Z", "middle");
    save(scheduleId, OTHER, "2026-01-04T10:00:00Z", "foreign");
    save(ScheduleId.generate(), OWNER, "2026-01-05T10:00:00Z", "unrelated");
    flushAndClear();

    assertThat(
            repository.findAllByScheduleIdAndOwnerKeyOrderByStartedAtDesc(
                scheduleId, OWNER, Limit.of(2)))
        .extracting(AutomationRun::getResultExcerpt)
        .containsExactly("newest", "middle");
  }

  @Test
  @DisplayName("should reload the schedule id and outcome of a finished run")
  void shouldReloadTheScheduleIdAndOutcomeOfAFinishedRun() {
    ScheduleId scheduleId = ScheduleId.generate();
    save(scheduleId, OWNER, "2026-01-01T10:00:00Z", "result excerpt");
    flushAndClear();

    AutomationRun reloaded =
        repository
            .findAllByScheduleIdAndOwnerKeyOrderByStartedAtDesc(scheduleId, OWNER, Limit.of(1))
            .getFirst();

    assertThat(reloaded.getScheduleId()).isEqualTo(scheduleId);
    assertThat(reloaded.getStatus()).isEqualTo(RunStatus.SUCCESS);
    assertThat(reloaded.getEmailStatus()).isEqualTo(EmailDeliveryStatus.SENT);
  }

  private void save(ScheduleId scheduleId, OwnerKey owner, String startedAt, String result) {
    AutomationRun run = AutomationRun.start(scheduleId, owner.value(), Instant.parse(startedAt));
    run.succeed(result, EmailDeliveryStatus.SENT);
    repository.save(run);
  }
}
