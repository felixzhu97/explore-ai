package com.ai.common.infra.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.infra.config.LaunchDarklyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LaunchDarklyProperties.class)
public class FeatureFlagFallbackConfig {
  /** Uses configured fallback flags when LaunchDarkly is off. */
  @Bean
  @ConditionalOnMissingBean(FeatureFlagRepository.class)
  @ConditionalOnExpression("'${launchdarkly.sdk-key:}'.length() == 0")
  public FeatureFlagRepository inMemoryFeatureFlagRepository(LaunchDarklyProperties properties) {
    return new InMemoryFeatureFlagRepository(properties);
  }
}
