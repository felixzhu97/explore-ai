package com.ai.automation.service;

import com.ai.automation.domain.exception.AutomationLimitExceededException;
import com.ai.automation.domain.exception.AutomationScheduleNotFoundException;
import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.service.CronSchedule;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.automation.domain.vo.ScheduleKind;
import com.ai.automation.domain.vo.ScheduleTiming;
import com.ai.automation.infra.config.AutomationProperties;
import com.ai.pipeline.domain.exception.PipelineTemplateNotFoundException;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import com.ai.pipeline.domain.vo.PipelineTemplateId;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages automation schedules, enforcing per-client limits, valid cron and owned workflows. */
@Service
@EnableConfigurationProperties(AutomationProperties.class)
@RequiredArgsConstructor
public class AutomationService {

  private final AutomationScheduleRepository scheduleRepository;
  private final AutomationRunRepository runRepository;
  private final PipelineTemplateRepository pipelineTemplateRepository;
  private final CronSchedule cronSchedule;
  private final AutomationProperties properties;

  /** Lists the owner's schedules. */
  public List<AutomationSchedule> list(String ownerKey) {
    return scheduleRepository.findAllByOwnerKey(ownerKey);
  }

  /** Lists recent runs of the owner's schedule, capped at 100. */
  public List<AutomationRun> listRuns(String ownerKey, String scheduleId, int limit) {
    requireOwned(ownerKey, scheduleId);
    int capped = Math.min(Math.max(limit, 1), 100);
    return runRepository.findByScheduleIdAndOwnerKey(ScheduleId.of(scheduleId), ownerKey, capped);
  }

  /** Creates a schedule for the owner and arms its first run. */
  @Transactional
  public AutomationSchedule create(
      String ownerKey,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      Instant runAt,
      String timezone,
      String pipelineTemplateId,
      String recipientEmail,
      String brief) {
    if (scheduleRepository.countByOwnerKey(ownerKey) >= properties.getMaxSchedulesPerClient()) {
      throw new AutomationLimitExceededException(
          "Schedule limit reached (" + properties.getMaxSchedulesPerClient() + ")");
    }
    requireWorkflow(ownerKey, pipelineTemplateId);
    Instant now = Instant.now();
    AutomationSchedule schedule =
        scheduleKind == ScheduleKind.ONCE
            ? AutomationSchedule.createOnce(
                ownerKey, name, timezone, pipelineTemplateId, recipientEmail, brief, runAt, now)
            : AutomationSchedule.create(
                ownerKey,
                name,
                cronExpression,
                timezone,
                pipelineTemplateId,
                recipientEmail,
                brief,
                now,
                cronSchedule);
    return scheduleRepository.save(schedule);
  }

  /** Replaces the owner's schedule settings and re-arms its next run. */
  @Transactional
  public AutomationSchedule update(
      String ownerKey,
      String scheduleId,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      Instant runAt,
      String timezone,
      String pipelineTemplateId,
      String recipientEmail,
      String brief) {
    AutomationSchedule schedule = requireOwned(ownerKey, scheduleId);
    requireWorkflow(ownerKey, pipelineTemplateId);
    schedule.update(
        name,
        ScheduleTiming.of(scheduleKind, cronExpression, timezone),
        runAt,
        pipelineTemplateId,
        recipientEmail,
        brief,
        Instant.now(),
        cronSchedule);
    return scheduleRepository.save(schedule);
  }

  /** Enables or disables the owner's schedule, re-arming the next run when enabled. */
  @Transactional
  public AutomationSchedule setEnabled(String ownerKey, String scheduleId, boolean enabled) {
    AutomationSchedule schedule = requireOwned(ownerKey, scheduleId);
    if (enabled) {
      schedule.turnOn(Instant.now(), cronSchedule);
    } else {
      schedule.disable();
    }
    return scheduleRepository.save(schedule);
  }

  /** Deletes the owner's schedule. */
  @Transactional
  public void delete(String ownerKey, String scheduleId) {
    requireOwned(ownerKey, scheduleId);
    scheduleRepository.deleteByIdAndOwnerKey(ScheduleId.of(scheduleId), ownerKey);
  }

  private AutomationSchedule requireOwned(String ownerKey, String scheduleId) {
    return scheduleRepository
        .findByIdAndOwnerKey(ScheduleId.of(scheduleId), ownerKey)
        .orElseThrow(() -> new AutomationScheduleNotFoundException(scheduleId));
  }

  private void requireWorkflow(String ownerKey, String pipelineTemplateId) {
    pipelineTemplateRepository
        .findByIdAndOwnerKey(PipelineTemplateId.of(pipelineTemplateId), ownerKey)
        .filter(template -> template.isEnabled())
        .orElseThrow(() -> new PipelineTemplateNotFoundException(pipelineTemplateId));
  }
}
