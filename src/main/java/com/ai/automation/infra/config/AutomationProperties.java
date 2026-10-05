package com.ai.automation.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Automation settings bound from {@code app.automation}: due-scan cadence and schedule limits. */
@ConfigurationProperties(prefix = "app.automation")
@Getter
@Setter
public class AutomationProperties {

  private boolean scanEnabled = true;
  private int scanBatchSize = 20;
  private int maxSchedulesPerClient = 10;
  private long scanFixedDelayMs = 60_000L;
}
