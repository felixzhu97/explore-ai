package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.vo.ScheduleId;
import java.util.List;

/** Documentation. */
public interface AutomationRunRepository {
  /** Inserts a new run; each run is written once, after it finishes. */
  AutomationRun save(AutomationRun run);

  /** Documentation. */
  List<AutomationRun> findByScheduleIdAndClientId(
      ScheduleId scheduleId, String clientId, int limit);
}
