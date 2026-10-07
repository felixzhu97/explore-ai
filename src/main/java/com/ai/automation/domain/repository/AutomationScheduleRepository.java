package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.ScheduleId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persists automation schedules and finds and claims those due for execution. */
public interface AutomationScheduleRepository {
  /** Finds the owner's schedule by id. */
  Optional<AutomationSchedule> findByIdAndOwnerKey(ScheduleId id, String ownerKey);

  /** Lists all schedules of the owner. */
  List<AutomationSchedule> findAllByOwnerKey(String ownerKey);

  /** Counts the owner's schedules. */
  int countByOwnerKey(String ownerKey);

  /** Lists enabled schedules due at or before the time. */
  List<AutomationSchedule> findDue(Instant asOf, int limit);

  /** Saves the schedule and returns the stored copy. */
  AutomationSchedule save(AutomationSchedule schedule);

  /** Deletes the owner's schedule by id. */
  void deleteByIdAndOwnerKey(ScheduleId id, String ownerKey);

  /**
   * Optimistic claim: advances {@code next_run_at} only when it still matches {@code
   * expectedNextRunAt}.
   *
   * @return true if this caller won the claim
   */
  boolean claim(ScheduleId id, Instant expectedNextRunAt, Instant provisionalNextRunAt);
}
