package com.ai.automation.service.usecase;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.ScheduleKind;
import java.time.Instant;
import java.util.List;

/** Manages a client's automation schedules and lists their run history. */
public interface AutomationUseCase {
  List<AutomationSchedule> list(String clientId);

  AutomationSchedule create(
      String clientId,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      Instant runAt,
      String timezone,
      String workflowTemplateId,
      String recipientEmail,
      String brief);

  AutomationSchedule update(
      String clientId,
      String scheduleId,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      Instant runAt,
      String timezone,
      String workflowTemplateId,
      String recipientEmail,
      String brief);

  AutomationSchedule setEnabled(String clientId, String scheduleId, boolean enabled);

  void delete(String clientId, String scheduleId);

  List<AutomationRun> listRuns(String clientId, String scheduleId, int limit);
}
