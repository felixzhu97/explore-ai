package com.ai.common.domain;

public interface FeatureFlagRepository {

    boolean isEnabled(String flagKey, boolean defaultValue);
}
