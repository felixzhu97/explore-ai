package com.ai.common.domain.repository;

/** Evaluates boolean feature flags by key, returning the default when unresolved. */
public interface FeatureFlagRepository {
  /** Tells whether the flag is on, using the default when unknown. */
  boolean isEnabled(String flagKey, boolean defaultValue);
}
