package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.ScheduleId;
import java.util.List;

/** Persists automation run records and lists a schedule's recent runs for its owner. */
public interface AutomationRunRepository {
  /** Lists the newest runs of the owner's schedule. */
  List<AutomationRun> findByScheduleIdAndOwnerKey(
      ScheduleId scheduleId, String ownerKey, int limit);

  /** Inserts a new run; each run is written once, after it finishes. */
  AutomationRun save(AutomationRun run);
}
