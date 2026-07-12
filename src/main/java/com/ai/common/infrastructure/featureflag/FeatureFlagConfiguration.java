package com.ai.common.infrastructure.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.repository.FeatureFlagRepository;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LaunchDarklyProperties.class)
public class FeatureFlagConfiguration {

    @Bean
    @ConditionalOnBean(LDClient.class)
    public FeatureFlagRepository launchDarklyFeatureFlagRepository(LDClient client) {
        return new LaunchDarklyFeatureFlagRepository(client);
    }

    @Bean
    @ConditionalOnMissingBean(FeatureFlagRepository.class)
    public FeatureFlagRepository inMemoryFeatureFlagRepository(LaunchDarklyProperties properties) {
        return new InMemoryFeatureFlagRepository(properties);
    }
}
