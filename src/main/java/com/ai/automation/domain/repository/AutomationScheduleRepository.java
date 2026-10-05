package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.ScheduleId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persists automation schedules and finds and claims those due for execution. */
public interface AutomationScheduleRepository {
  Optional<AutomationSchedule> findByIdAndOwnerKey(ScheduleId id, String ownerKey);

  List<AutomationSchedule> findAllByOwnerKey(String ownerKey);

  int countByOwnerKey(String ownerKey);

  List<AutomationSchedule> findDue(Instant asOf, int limit);

  AutomationSchedule save(AutomationSchedule schedule);

  void deleteByIdAndOwnerKey(ScheduleId id, String ownerKey);

  /**
   * Optimistic claim: advances {@code next_run_at} only when it still matches {@code
   * expectedNextRunAt}.
   *
   * @return true if this caller won the claim
   */
  boolean claim(ScheduleId id, Instant expectedNextRunAt, Instant provisionalNextRunAt);
}
