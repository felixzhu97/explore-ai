package com.ai.automation.application.usecase;

import com.ai.automation.application.CronScheduleCalculator;
import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.domain.repository.WorkflowRunner;
import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.application.AutomationProperties;
import com.ai.billing.application.DailyUsageQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ExecuteDueAutomationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExecuteDueAutomationsUseCase.class);

    private final AutomationScheduleRepository scheduleRepository;
    private final AutomationRunRepository runRepository;
    private final WorkflowRunner workflowRunner;
    private final EmailGateway emailGateway;
    private final CronScheduleCalculator cronCalculator;
    private final DailyUsageQuotaService dailyUsageQuotaService;
    private final AutomationProperties properties;

    public ExecuteDueAutomationsUseCase(
            AutomationScheduleRepository scheduleRepository,
            AutomationRunRepository runRepository,
            WorkflowRunner workflowRunner,
            EmailGateway emailGateway,
            CronScheduleCalculator cronCalculator,
            DailyUsageQuotaService dailyUsageQuotaService,
            AutomationProperties properties) {
        this.scheduleRepository = scheduleRepository;
        this.runRepository = runRepository;
        this.workflowRunner = workflowRunner;
        this.emailGateway = emailGateway;
        this.cronCalculator = cronCalculator;
        this.dailyUsageQuotaService = dailyUsageQuotaService;
        this.properties = properties;
    }

    @Transactional
    public int executeDue() {
        Instant now = Instant.now();
        List<AutomationSchedule> due = scheduleRepository.findDue(now, properties.getScanBatchSize());
        int executed = 0;
        for (AutomationSchedule schedule : due) {
            Instant provisional = cronCalculator.nextRunAt(
                    schedule.getCronExpression(), schedule.getTimezone(), now);
            boolean claimed = scheduleRepository.claim(
                    schedule.getId(), schedule.getNextRunAt(), provisional);
            if (!claimed) {
                continue;
            }
            executeOne(schedule, now, provisional);
            executed++;
        }
        return executed;
    }

    private void executeOne(AutomationSchedule schedule, Instant now, Instant provisionalNext) {
        AutomationRun run = AutomationRun.start(schedule.getId(), schedule.getClientId());
        if (!dailyUsageQuotaService.tryConsume(schedule.getClientId())) {
            run.skip("Daily plan quota exceeded");
            runRepository.save(run);
            schedule.markExecuted(Instant.now(), provisionalNext);
            scheduleRepository.save(schedule);
            return;
        }

        try {
            String result = workflowRunner.runSavedWorkflow(
                    schedule.getClientId(),
                    schedule.getWorkflowTemplateId(),
                    schedule.getBrief(),
                    "en");
            EmailDeliveryStatus emailStatus = sendResultEmail(schedule, result);
            run.succeed(result, emailStatus);
            runRepository.save(run);
            Instant finished = Instant.now();
            Instant next = cronCalculator.nextRunAt(
                    schedule.getCronExpression(), schedule.getTimezone(), finished);
            schedule.markExecuted(finished, next);
            scheduleRepository.save(schedule);
        } catch (Exception ex) {
            log.warn(
                    "Automation schedule={} failed: {}",
                    schedule.getId().value(),
                    ex.getMessage());
            run.fail(ex.getMessage(), EmailDeliveryStatus.SKIPPED);
            runRepository.save(run);
            Instant finished = Instant.now();
            Instant next = cronCalculator.nextRunAt(
                    schedule.getCronExpression(), schedule.getTimezone(), finished);
            schedule.markExecuted(finished, next);
            scheduleRepository.save(schedule);
        }
    }

    private EmailDeliveryStatus sendResultEmail(AutomationSchedule schedule, String result) {
        String excerpt = result == null ? "" : result;
        if (excerpt.length() > AutomationRun.MAX_RESULT_EXCERPT) {
            excerpt = excerpt.substring(0, AutomationRun.MAX_RESULT_EXCERPT);
        }
        try {
            emailGateway.send(new EmailMessage(
                    schedule.getRecipientEmail(),
                    "[ExploreAI] " + schedule.getName(),
                    """
                    Automation: %s

                    Task brief:
                    %s

                    Result:
                    %s
                    """.formatted(schedule.getName(), schedule.getBrief(), excerpt)));
            return EmailDeliveryStatus.SENT;
        } catch (Exception ex) {
            log.warn("Email send failed for schedule={}: {}", schedule.getId().value(), ex.getMessage());
            return EmailDeliveryStatus.FAILED;
        }
    }
}
