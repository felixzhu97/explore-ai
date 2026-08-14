package com.ai.common.infrastructure.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.repository.FeatureFlagRepository;

/** Documentation. */
public class InMemoryFeatureFlagRepository implements FeatureFlagRepository {

  private final LaunchDarklyProperties properties;

  /** Documentation. */
  public InMemoryFeatureFlagRepository(LaunchDarklyProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isEnabled(String flagKey, boolean defaultValue) {
    return properties.getFallback().getOrDefault(flagKey, defaultValue);
  }
}
