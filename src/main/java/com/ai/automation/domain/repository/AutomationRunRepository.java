package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.vo.ScheduleId;
import java.util.List;

/** Documentation. */
public interface AutomationRunRepository {
  /** Documentation. */
  AutomationRun save(AutomationRun run);

  /** Documentation. */
  List<AutomationRun> findByScheduleIdAndClientId(
      ScheduleId scheduleId, String clientId, int limit);
}
