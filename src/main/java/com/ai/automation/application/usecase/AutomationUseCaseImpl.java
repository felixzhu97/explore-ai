package com.ai.automation.application.usecase;

import com.ai.automation.application.CronScheduleCalculator;
import com.ai.automation.domain.exception.AutomationLimitExceededException;
import com.ai.automation.domain.exception.AutomationScheduleNotFoundException;
import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.automation.application.AutomationProperties;
import com.ai.pipeline.domain.exception.WorkflowTemplateNotFoundException;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@EnableConfigurationProperties(AutomationProperties.class)
public class AutomationUseCaseImpl implements AutomationUseCase {

    private final AutomationScheduleRepository scheduleRepository;
    private final AutomationRunRepository runRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final CronScheduleCalculator cronCalculator;
    private final AutomationProperties properties;

    public AutomationUseCaseImpl(
            AutomationScheduleRepository scheduleRepository,
            AutomationRunRepository runRepository,
            WorkflowTemplateRepository workflowTemplateRepository,
            CronScheduleCalculator cronCalculator,
            AutomationProperties properties) {
        this.scheduleRepository = scheduleRepository;
        this.runRepository = runRepository;
        this.workflowTemplateRepository = workflowTemplateRepository;
        this.cronCalculator = cronCalculator;
        this.properties = properties;
    }

    @Override
    public List<AutomationSchedule> list(String clientId) {
        return scheduleRepository.findAllByClientId(clientId);
    }

    @Override
    @Transactional
    public AutomationSchedule create(
            String clientId,
            String name,
            String cronExpression,
            String timezone,
            String workflowTemplateId,
            String recipientEmail,
            String brief) {
        if (scheduleRepository.countByClientId(clientId) >= properties.getMaxSchedulesPerClient()) {
            throw new AutomationLimitExceededException(
                    "Schedule limit reached (" + properties.getMaxSchedulesPerClient() + ")");
        }
        requireWorkflow(clientId, workflowTemplateId);
        cronCalculator.validate(cronExpression, timezone);
        Instant next = cronCalculator.nextRunAt(cronExpression, timezone, Instant.now());
        AutomationSchedule schedule = AutomationSchedule.create(
                clientId,
                name,
                cronExpression,
                timezone,
                workflowTemplateId,
                recipientEmail,
                brief,
                next);
        return scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public AutomationSchedule update(
            String clientId,
            String scheduleId,
            String name,
            String cronExpression,
            String timezone,
            String workflowTemplateId,
            String recipientEmail,
            String brief) {
        AutomationSchedule schedule = requireOwned(clientId, scheduleId);
        requireWorkflow(clientId, workflowTemplateId);
        cronCalculator.validate(cronExpression, timezone);
        Instant next = cronCalculator.nextRunAt(cronExpression, timezone, Instant.now());
        schedule.update(name, cronExpression, timezone, workflowTemplateId, recipientEmail, brief, next);
        return scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public AutomationSchedule setEnabled(String clientId, String scheduleId, boolean enabled) {
        AutomationSchedule schedule = requireOwned(clientId, scheduleId);
        if (enabled) {
            Instant next = cronCalculator.nextRunAt(
                    schedule.getCronExpression(), schedule.getTimezone(), Instant.now());
            schedule.enable(next);
        } else {
            schedule.disable();
        }
        return scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void delete(String clientId, String scheduleId) {
        requireOwned(clientId, scheduleId);
        scheduleRepository.deleteByIdAndClientId(ScheduleId.of(scheduleId), clientId);
    }

    @Override
    public List<AutomationRun> listRuns(String clientId, String scheduleId, int limit) {
        requireOwned(clientId, scheduleId);
        int capped = Math.min(Math.max(limit, 1), 100);
        return runRepository.findByScheduleIdAndClientId(ScheduleId.of(scheduleId), clientId, capped);
    }

    private AutomationSchedule requireOwned(String clientId, String scheduleId) {
        return scheduleRepository.findByIdAndClientId(ScheduleId.of(scheduleId), clientId)
                .orElseThrow(() -> new AutomationScheduleNotFoundException(scheduleId));
    }

    private void requireWorkflow(String clientId, String workflowTemplateId) {
        workflowTemplateRepository.findByIdAndClientId(WorkflowTemplateId.of(workflowTemplateId), clientId)
                .filter(template -> template.isEnabled())
                .orElseThrow(() -> new WorkflowTemplateNotFoundException(workflowTemplateId));
    }
}
