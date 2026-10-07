package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.ScheduleId;
import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persists automation schedules and finds and claims those due for execution. */
public interface AutomationScheduleRepository extends Repository<AutomationSchedule, ScheduleId> {

  /** Finds the owner's schedule by id. */
  Optional<AutomationSchedule> findByIdAndOwnerKey(ScheduleId id, OwnerKey ownerKey);

  /** Lists the owner's schedules, newest first. */
  List<AutomationSchedule> findAllByOwnerKeyOrderByCreatedAtDesc(OwnerKey ownerKey);

  /** Counts the owner's schedules. */
  long countByOwnerKey(OwnerKey ownerKey);

  /** Lists every owner's enabled schedules due at or before the time, soonest first. */
  @Query(
      """
      SELECT s FROM AutomationSchedule s
      WHERE s.enabled = true AND s.nextRunAt <= ?1
      ORDER BY s.nextRunAt
      """)
  List<AutomationSchedule> findDue(Instant asOf, Limit limit);

  /** Saves the schedule and returns the stored copy. */
  AutomationSchedule save(AutomationSchedule schedule);

  /** Deletes the owner's schedule by id. */
  @Transactional
  void deleteByIdAndOwnerKey(ScheduleId id, OwnerKey ownerKey);

  /**
   * Moves the next run time only if it still matches, so one worker claims the run. The version
   * stays put because the claimer saves the loaded schedule again when the run finishes.
   */
  @Transactional
  @Modifying
  @Query(
      """
      UPDATE AutomationSchedule s
      SET s.nextRunAt = ?3, s.updatedAt = ?4
      WHERE s.id = ?1 AND s.enabled = true AND s.nextRunAt = ?2
      """)
  int claimNextRun(ScheduleId id, Instant expectedNextRunAt, Instant claimedNextRunAt, Instant now);

  /**
   * Optimistic claim: advances {@code next_run_at} only when it still matches {@code
   * expectedNextRunAt}.
   *
   * @return true if this caller won the claim
   */
  default boolean claimSchedule(
      ScheduleId id, Instant expectedNextRunAt, Instant claimedNextRunAt) {
    return claimNextRun(id, expectedNextRunAt, claimedNextRunAt, Instant.now()) == 1;
  }
}
