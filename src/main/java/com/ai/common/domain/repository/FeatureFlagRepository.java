package com.ai.common.domain.repository;

public interface FeatureFlagRepository {

    boolean isEnabled(String flagKey, boolean defaultValue);
}
