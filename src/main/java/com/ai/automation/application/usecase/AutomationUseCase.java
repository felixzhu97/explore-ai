package com.ai.automation.application.usecase;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;

import java.util.List;

public interface AutomationUseCase {

    List<AutomationSchedule> list(String clientId);

    AutomationSchedule create(
            String clientId,
            String name,
            String cronExpression,
            String timezone,
            String workflowTemplateId,
            String recipientEmail,
            String brief);

    AutomationSchedule update(
            String clientId,
            String scheduleId,
            String name,
            String cronExpression,
            String timezone,
            String workflowTemplateId,
            String recipientEmail,
            String brief);

    AutomationSchedule setEnabled(String clientId, String scheduleId, boolean enabled);

    void delete(String clientId, String scheduleId);

    List<AutomationRun> listRuns(String clientId, String scheduleId, int limit);
}
