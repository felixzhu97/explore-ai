package com.ai.common.domain.repository;

/** Evaluates boolean feature flags by key, returning the default when unresolved. */
public interface FeatureFlagRepository {
  boolean isEnabled(String flagKey, boolean defaultValue);
}
