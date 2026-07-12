package com.ai.common.infrastructure.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;

public class LaunchDarklyFeatureFlagRepository implements FeatureFlagRepository {

    private static final LDContext SERVER_CONTEXT =
            LDContext.builder("explore-ai-server").build();

    private final LDClient client;

    public LaunchDarklyFeatureFlagRepository(LDClient client) {
        this.client = client;
    }

    @Override
    public boolean isEnabled(String flagKey, boolean defaultValue) {
        return client.boolVariation(flagKey, SERVER_CONTEXT, defaultValue);
    }
}
