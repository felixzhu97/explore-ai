package com.ai.automation.service;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.domain.repository.PipelineGateway;
import com.ai.automation.domain.service.CronSchedule;
import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.infra.config.AutomationProperties;
import com.ai.billing.service.DailyUsageQuotaService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs due automation schedules under daily quota, emails results and records each run. */
@Service
@RequiredArgsConstructor
public class DueAutomationRunner {

  private static final Logger log = LoggerFactory.getLogger(DueAutomationRunner.class);

  private final AutomationScheduleRepository scheduleRepository;
  private final AutomationRunRepository runRepository;
  private final PipelineGateway pipelineGateway;
  private final EmailGateway emailGateway;
  private final AutomationMailFormatter mailFormatter;
  private final CronSchedule cronSchedule;
  private final DailyUsageQuotaService dailyUsageQuotaService;
  private final AutomationProperties properties;

  /** Claims and runs a batch of due schedules, returning how many this instance executed. */
  @Transactional
  public int executeDue() {
    Instant now = Instant.now();
    List<AutomationSchedule> due = scheduleRepository.findDue(now, properties.getScanBatchSize());
    int executed = 0;
    for (AutomationSchedule schedule : due) {
      Instant provisional = schedule.provisionalNextRunAt(now, cronSchedule);
      if (!scheduleRepository.claim(schedule.getId(), schedule.getNextRunAt(), provisional)) {
        continue;
      }
      executeOne(schedule);
      executed++;
    }
    return executed;
  }

  private void executeOne(AutomationSchedule schedule) {
    AutomationRun run = AutomationRun.start(schedule.getId(), schedule.getOwnerKeyValue());
    if (!dailyUsageQuotaService.tryConsume(schedule.getOwnerKey())) {
      run.skipForQuota();
    } else {
      try {
        String result =
            pipelineGateway.runSavedTemplate(
                schedule.getOwnerKeyValue(),
                schedule.getPipelineTemplateId().value(),
                schedule.getBrief(),
                "en");
        run.succeed(result, sendResultEmail(schedule, result));
      } catch (Exception ex) {
        log.warn("Automation schedule={} failed: {}", schedule.getId().value(), ex.getMessage());
        run.failBeforeEmail(ex.getMessage());
      }
    }
    runRepository.save(run);
    schedule.recordRunFinished(Instant.now(), cronSchedule);
    scheduleRepository.save(schedule);
  }

  private EmailDeliveryStatus sendResultEmail(AutomationSchedule schedule, String result) {
    AutomationMailFormatter.FormattedMail formatted =
        mailFormatter.format(schedule.getName(), schedule.getBrief(), result);
    try {
      emailGateway.send(schedule.resultEmail(formatted.textBody(), formatted.htmlBody()));
      return EmailDeliveryStatus.SENT;
    } catch (Exception ex) {
      log.warn("Email send failed for schedule={}: {}", schedule.getId().value(), ex.getMessage());
      return EmailDeliveryStatus.FAILED;
    }
  }
}
