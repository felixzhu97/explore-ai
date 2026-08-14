package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.ScheduleId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Documentation. */
public interface AutomationScheduleRepository {
  /** Documentation. */
  AutomationSchedule save(AutomationSchedule schedule);

  /** Documentation. */
  Optional<AutomationSchedule> findByIdAndClientId(ScheduleId id, String clientId);

  /** Documentation. */
  List<AutomationSchedule> findAllByClientId(String clientId);

  /** Documentation. */
  int countByClientId(String clientId);

  /** Documentation. */
  void deleteByIdAndClientId(ScheduleId id, String clientId);

  /** Documentation. */
  List<AutomationSchedule> findDue(Instant asOf, int limit);

  /**
   * Optimistic claim: advances {@code next_run_at} only when it still matches {@code
   * expectedNextRunAt}.
   *
   * @return true if this caller won the claim
   */
  boolean claim(ScheduleId id, Instant expectedNextRunAt, Instant provisionalNextRunAt);
}
