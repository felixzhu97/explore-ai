package com.ai.automation.service;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.domain.repository.PipelineGateway;
import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.infra.config.AutomationProperties;
import com.ai.billing.service.DailyUsageQuotaService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs due automation schedules under daily quota, emails results and records each run. */
@Service
public class DueAutomationRunner {

  private static final Logger log = LoggerFactory.getLogger(DueAutomationRunner.class);

  private final AutomationScheduleRepository scheduleRepository;
  private final AutomationRunRepository runRepository;
  private final PipelineGateway pipelineGateway;
  private final EmailGateway emailGateway;
  private final AutomationMailFormatter mailFormatter;
  private final CronScheduleCalculator cronCalculator;
  private final DailyUsageQuotaService dailyUsageQuotaService;
  private final AutomationProperties properties;

  public DueAutomationRunner(
      AutomationScheduleRepository scheduleRepository,
      AutomationRunRepository runRepository,
      PipelineGateway pipelineGateway,
      EmailGateway emailGateway,
      AutomationMailFormatter mailFormatter,
      CronScheduleCalculator cronCalculator,
      DailyUsageQuotaService dailyUsageQuotaService,
      AutomationProperties properties) {
    this.scheduleRepository = scheduleRepository;
    this.runRepository = runRepository;
    this.pipelineGateway = pipelineGateway;
    this.emailGateway = emailGateway;
    this.mailFormatter = mailFormatter;
    this.cronCalculator = cronCalculator;
    this.dailyUsageQuotaService = dailyUsageQuotaService;
    this.properties = properties;
  }

  /** Claims and runs a batch of due schedules, returning how many this instance executed. */
  @Transactional
  public int executeDue() {
    Instant now = Instant.now();
    List<AutomationSchedule> due = scheduleRepository.findDue(now, properties.getScanBatchSize());
    int executed = 0;
    for (AutomationSchedule schedule : due) {
      Instant provisional =
          schedule.isOnce()
              ? AutomationSchedule.ONCE_TERMINAL_NEXT
              : cronCalculator.nextRunAt(schedule.getCronExpression(), schedule.getTimezone(), now);
      boolean claimed =
          scheduleRepository.claim(schedule.getId(), schedule.getNextRunAt(), provisional);
      if (!claimed) {
        continue;
      }
      executeOne(schedule, now, provisional);
      executed++;
    }
    return executed;
  }

  private void executeOne(AutomationSchedule schedule, Instant now, Instant provisionalNext) {
    AutomationRun run = AutomationRun.start(schedule.getId(), schedule.getOwnerKeyValue());
    if (!dailyUsageQuotaService.tryConsume(schedule.getOwnerKeyValue())) {
      run.skip("Daily plan quota exceeded");
      runRepository.save(run);
      finishSchedule(schedule, Instant.now(), provisionalNext);
      return;
    }

    try {
      String result =
          pipelineGateway.runSavedTemplate(
              schedule.getOwnerKeyValue(),
              schedule.getPipelineTemplateId().value(),
              schedule.getBrief(),
              "en");
      EmailDeliveryStatus emailStatus = sendResultEmail(schedule, result);
      run.succeed(result, emailStatus);
      runRepository.save(run);
      Instant finished = Instant.now();
      Instant next =
          schedule.isOnce()
              ? AutomationSchedule.ONCE_TERMINAL_NEXT
              : cronCalculator.nextRunAt(
                  schedule.getCronExpression(), schedule.getTimezone(), finished);
      finishSchedule(schedule, finished, next);
    } catch (Exception ex) {
      log.warn("Automation schedule={} failed: {}", schedule.getId().value(), ex.getMessage());
      run.fail(ex.getMessage(), EmailDeliveryStatus.SKIPPED);
      runRepository.save(run);
      Instant finished = Instant.now();
      Instant next =
          schedule.isOnce()
              ? AutomationSchedule.ONCE_TERMINAL_NEXT
              : cronCalculator.nextRunAt(
                  schedule.getCronExpression(), schedule.getTimezone(), finished);
      finishSchedule(schedule, finished, next);
    }
  }

  private void finishSchedule(AutomationSchedule schedule, Instant finished, Instant next) {
    if (schedule.isOnce()) {
      schedule.completeOnce(finished);
    } else {
      schedule.markExecuted(finished, next);
    }
    scheduleRepository.save(schedule);
  }

  private EmailDeliveryStatus sendResultEmail(AutomationSchedule schedule, String result) {
    AutomationMailFormatter.FormattedMail formatted =
        mailFormatter.format(schedule.getName(), schedule.getBrief(), result);
    try {
      emailGateway.send(
          new EmailMessage(
              schedule.getRecipientEmail(),
              "[ExploreAI] " + schedule.getName(),
              formatted.textBody(),
              formatted.htmlBody()));
      return EmailDeliveryStatus.SENT;
    } catch (Exception ex) {
      log.warn("Email send failed for schedule={}: {}", schedule.getId().value(), ex.getMessage());
      return EmailDeliveryStatus.FAILED;
    }
  }
}
