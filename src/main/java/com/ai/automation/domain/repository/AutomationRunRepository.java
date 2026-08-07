package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.vo.ScheduleId;

import java.util.List;

public interface AutomationRunRepository {

    AutomationRun save(AutomationRun run);

    List<AutomationRun> findByScheduleIdAndClientId(ScheduleId scheduleId, String clientId, int limit);
}
