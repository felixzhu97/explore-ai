package com.ai.automation.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.automation")
public class AutomationProperties {

  private boolean scanEnabled = true;
  private int scanBatchSize = 20;
  private int maxSchedulesPerClient = 10;
  private long scanFixedDelayMs = 60_000L;

  public boolean isScanEnabled() {
    return scanEnabled;
  }

  public void setScanEnabled(boolean scanEnabled) {
    this.scanEnabled = scanEnabled;
  }

  public int getScanBatchSize() {
    return scanBatchSize;
  }

  public void setScanBatchSize(int scanBatchSize) {
    this.scanBatchSize = scanBatchSize;
  }

  public int getMaxSchedulesPerClient() {
    return maxSchedulesPerClient;
  }

  public void setMaxSchedulesPerClient(int maxSchedulesPerClient) {
    this.maxSchedulesPerClient = maxSchedulesPerClient;
  }

  public long getScanFixedDelayMs() {
    return scanFixedDelayMs;
  }

  public void setScanFixedDelayMs(long scanFixedDelayMs) {
    this.scanFixedDelayMs = scanFixedDelayMs;
  }
}
