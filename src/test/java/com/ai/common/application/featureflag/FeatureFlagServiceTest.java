package com.ai.common.application.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.domain.vo.ModuleFlag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureFlagService")
class FeatureFlagServiceTest {

    @Test
    @DisplayName("should return repository value for module flag")
    void should_returnRepositoryValue_when_moduleFlagRequested() {
        LaunchDarklyProperties properties = new LaunchDarklyProperties();
        properties.setFallback(Map.of("module-eval", false));

        FeatureFlagRepository repository = (flagKey, defaultValue) ->
                "module-eval".equals(flagKey);

        FeatureFlagService service = new FeatureFlagService(repository, properties);

        assertThat(service.isModuleEnabled(ModuleFlag.EVAL)).isTrue();
        assertThat(service.isModuleEnabled(ModuleFlag.MCP)).isFalse();
    }
}
