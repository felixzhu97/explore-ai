package com.ai.common.service.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.domain.vo.ModuleFlag;
import com.ai.common.infra.config.LaunchDarklyProperties;
import org.springframework.stereotype.Service;

/** Checks whether application modules are enabled, using configured fallbacks as defaults. */
@Service
public class FeatureFlagService {

  private final FeatureFlagRepository repository;
  private final LaunchDarklyProperties properties;

  public FeatureFlagService(FeatureFlagRepository repository, LaunchDarklyProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  public boolean isModuleEnabled(ModuleFlag module) {
    return repository.isEnabled(module.key(), properties.fallbackFor(module.key()));
  }
}
