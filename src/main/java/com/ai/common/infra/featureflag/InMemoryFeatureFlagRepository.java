package com.ai.common.infra.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.infra.config.LaunchDarklyProperties;
import lombok.RequiredArgsConstructor;

/** Feature flag repository backed by the configured LaunchDarkly fallback values. */
@RequiredArgsConstructor
public class InMemoryFeatureFlagRepository implements FeatureFlagRepository {

  private final LaunchDarklyProperties properties;

  @Override
  public boolean isEnabled(String flagKey, boolean defaultValue) {
    return properties.getFallback().getOrDefault(flagKey, defaultValue);
  }
}
