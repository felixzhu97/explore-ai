package com.ai.common.domain.repository;

/** Documentation. */
public interface FeatureFlagRepository {
  /** Documentation. */
  boolean isEnabled(String flagKey, boolean defaultValue);
}
