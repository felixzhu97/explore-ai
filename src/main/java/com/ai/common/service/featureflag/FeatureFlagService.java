package com.ai.common.service.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.domain.vo.ModuleFlag;
import org.springframework.stereotype.Service;

/** Documentation. */
@Service
public class FeatureFlagService {

  private final FeatureFlagRepository repository;
  private final LaunchDarklyProperties properties;

  /** Documentation. */
  public FeatureFlagService(FeatureFlagRepository repository, LaunchDarklyProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  /** Documentation. */
  public boolean isModuleEnabled(ModuleFlag module) {
    return repository.isEnabled(module.key(), properties.fallbackFor(module.key()));
  }
}
