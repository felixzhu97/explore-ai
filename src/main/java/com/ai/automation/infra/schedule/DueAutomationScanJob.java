package com.ai.automation.infra.schedule;

import com.ai.automation.infra.config.AutomationProperties;
import com.ai.automation.service.DueAutomationRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job that periodically executes automation schedules whose next run is due. */
@Component
@EnableConfigurationProperties(AutomationProperties.class)
@RequiredArgsConstructor
public class DueAutomationScanJob {

  private final DueAutomationRunner dueAutomationRunner;
  private final AutomationProperties properties;

  /** Runs the schedules that are due, when scanning is enabled. */
  @Scheduled(fixedDelayString = "${app.automation.scan-fixed-delay-ms:60000}")
  public void scanDueSchedules() {
    if (!properties.isScanEnabled()) {
      return;
    }
    dueAutomationRunner.executeDue();
  }
}
