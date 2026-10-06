package com.ai.common.infra.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** LaunchDarkly settings: enabled flag, SDK key, and per-flag fallback values. */
@ConfigurationProperties(prefix = "launchdarkly")
@Getter
@Setter
public class LaunchDarklyProperties {

  private boolean enabled = true;
  private String sdkKey = "";
  private Map<String, Boolean> fallback = new HashMap<>();

  /** Returns the fallback value for the flag, false when not set. */
  public boolean resolveFallback(String flagKey) {
    return fallback.getOrDefault(flagKey, false);
  }
}
