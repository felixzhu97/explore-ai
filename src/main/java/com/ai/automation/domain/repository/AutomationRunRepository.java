package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.RunId;
import com.ai.automation.domain.model.ScheduleId;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.domain.repository.OwnerPartitionedRepository;
import java.util.List;
import org.springframework.data.domain.Limit;

/** Persists automation run records and lists a schedule's recent runs for its owner. */
public interface AutomationRunRepository extends OwnerPartitionedRepository<AutomationRun, RunId> {

  /** Lists the newest runs of the owner's schedule. */
  List<AutomationRun> findAllByScheduleIdAndOwnerKeyOrderByStartedAtDesc(
      ScheduleId scheduleId, OwnerKey ownerKey, Limit limit);

  /** Inserts a new run; each run is written once, after it finishes. */
  AutomationRun save(AutomationRun run);
}
