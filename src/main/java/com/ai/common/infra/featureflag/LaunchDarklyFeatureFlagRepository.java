package com.ai.common.infra.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import lombok.RequiredArgsConstructor;

/** Feature flag repository that evaluates boolean flags via the LaunchDarkly server SDK. */
@RequiredArgsConstructor
public class LaunchDarklyFeatureFlagRepository implements FeatureFlagRepository {

  private static final LDContext SERVER_CONTEXT = LDContext.builder("explore-ai-server").build();

  private final LDClient client;

  @Override
  public boolean isEnabled(String flagKey, boolean defaultValue) {
    return client.boolVariation(flagKey, SERVER_CONTEXT, defaultValue);
  }
}
