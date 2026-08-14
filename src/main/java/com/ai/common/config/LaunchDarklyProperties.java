package com.ai.common.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "launchdarkly")
public class LaunchDarklyProperties {

  private boolean enabled = true;
  private String sdkKey = "";
  private Map<String, Boolean> fallback = new HashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSdkKey() {
    return sdkKey;
  }

  public void setSdkKey(String sdkKey) {
    this.sdkKey = sdkKey;
  }

  public Map<String, Boolean> getFallback() {
    return fallback;
  }

  public void setFallback(Map<String, Boolean> fallback) {
    this.fallback = fallback;
  }

  /** Documentation. */
  public boolean fallbackFor(String flagKey) {
    return fallback.getOrDefault(flagKey, false);
  }
}
