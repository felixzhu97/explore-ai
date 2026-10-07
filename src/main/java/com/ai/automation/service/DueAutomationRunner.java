package com.ai.automation.service;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.EmailDeliveryStatus;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.domain.repository.PipelineGateway;
import com.ai.automation.infra.config.AutomationProperties;
import com.ai.billing.service.DailyUsageQuotaService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs due automation schedules under daily quota, emails results and records each run. */
@Service
@RequiredArgsConstructor
public class DueAutomationRunner {

  private final AutomationScheduleRepository scheduleRepository;
  private final AutomationRunRepository runRepository;
  private final PipelineGateway pipelineGateway;
  private final EmailGateway emailGateway;
  private final AutomationMailFormatter mailFormatter;
  private final DailyUsageQuotaService dailyUsageQuotaService;
  private final AutomationProperties properties;

  /** Claims and runs a batch of due schedules, returning how many this instance executed. */
  @Transactional
  public int executeDue() {
    Instant now = Instant.now();
    List<AutomationSchedule> due =
        scheduleRepository.findDue(now, Limit.of(properties.getScanBatchSize()));
    int executed = 0;
    for (AutomationSchedule schedule : due) {
      Instant provisional = schedule.calculateClaimedNextRunAt(now);
      if (!scheduleRepository.claimSchedule(
          schedule.getId(), schedule.getNextRunAt(), provisional)) {
        continue;
      }
      executeOne(schedule);
      executed++;
    }
    return executed;
  }

  private void executeOne(AutomationSchedule schedule) {
    AutomationRun run = AutomationRun.startRun(schedule.getId(), schedule.getOwnerKeyValue());
    if (!dailyUsageQuotaService.tryConsume(schedule.getOwnerKey())) {
      run.markSkippedForQuota();
    } else {
      try {
        String result =
            pipelineGateway.runSavedTemplate(
                schedule.getOwnerKeyValue(),
                schedule.getPipelineTemplateId().toString(),
                schedule.getBrief(),
                "en");
        run.markSucceeded(result, sendResultEmail(schedule, result));
      } catch (Exception ex) {
        run.markFailedBeforeEmail(ex.getMessage());
      }
    }
    runRepository.save(run);
    schedule.completeRun(Instant.now());
    scheduleRepository.save(schedule);
  }

  private EmailDeliveryStatus sendResultEmail(AutomationSchedule schedule, String result) {
    AutomationMailFormatter.FormattedMail formatted =
        mailFormatter.formatMail(schedule.getName(), schedule.getBrief(), result);
    try {
      emailGateway.sendEmail(
          schedule.composeResultEmail(formatted.textBody(), formatted.htmlBody()));
      return EmailDeliveryStatus.SENT;
    } catch (Exception ex) {
      return EmailDeliveryStatus.FAILED;
    }
  }
}
