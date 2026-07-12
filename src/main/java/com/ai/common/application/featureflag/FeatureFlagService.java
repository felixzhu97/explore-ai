package com.ai.common.application.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.domain.vo.ModuleFlag;
import org.springframework.stereotype.Service;

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
