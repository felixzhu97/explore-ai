package com.ai.common.service.featureflag;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.ModuleFlag;
import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.infra.config.LaunchDarklyProperties;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FeatureFlagService")
class FeatureFlagServiceTest {

  @Test
  @DisplayName("should return repository value for module flag")
  void shouldReturnRepositoryValueWhenModuleFlagRequested() {
    LaunchDarklyProperties properties = new LaunchDarklyProperties();
    properties.setFallback(Map.of("module-eval", false));

    FeatureFlagRepository repository = (flagKey, defaultValue) -> "module-eval".equals(flagKey);

    FeatureFlagService service = new FeatureFlagService(repository, properties);

    assertThat(service.isModuleEnabled(ModuleFlag.EVAL)).isTrue();
    assertThat(service.isModuleEnabled(ModuleFlag.MCP)).isFalse();
  }
}
