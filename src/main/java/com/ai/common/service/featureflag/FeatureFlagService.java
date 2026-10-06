package com.ai.common.service.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.domain.vo.ModuleFlag;
import com.ai.common.infra.config.LaunchDarklyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Checks whether application modules are enabled, using configured fallbacks as defaults. */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

  private final FeatureFlagRepository repository;
  private final LaunchDarklyProperties properties;

  public boolean isModuleEnabled(ModuleFlag module) {
    return repository.isEnabled(module.key(), properties.resolveFallback(module.key()));
  }
}
