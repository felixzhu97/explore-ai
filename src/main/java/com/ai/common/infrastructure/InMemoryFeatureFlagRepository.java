package com.ai.common.infrastructure;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.FeatureFlagRepository;

public class InMemoryFeatureFlagRepository implements FeatureFlagRepository {

    private final LaunchDarklyProperties properties;

    public InMemoryFeatureFlagRepository(LaunchDarklyProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isEnabled(String flagKey, boolean defaultValue) {
        return properties.getFallback().getOrDefault(flagKey, defaultValue);
    }
}
