package com.ai.common.infra.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.ai.common.domain.repository.FeatureFlagRepository;
import com.launchdarkly.sdk.server.LDClient;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentation. */
@Configuration
@ConditionalOnProperty(name = "launchdarkly.enabled", havingValue = "true", matchIfMissing = true)
public class LaunchDarklyConfig {

  private static final Logger log = LoggerFactory.getLogger(LaunchDarklyConfig.class);

  private LDClient ldClient;

  /** Documentation. */
  @Bean
  @ConditionalOnExpression("'${launchdarkly.sdk-key:}'.length() > 0")
  public LDClient ldClient(LaunchDarklyProperties properties) {
    ldClient = new LDClient(properties.getSdkKey());
    LaunchDarklyClientSupport.waitForInitialization(ldClient, Duration.ofSeconds(5));
    return ldClient;
  }

  /** Documentation. */
  @Bean
  @ConditionalOnBean(LDClient.class)
  public FeatureFlagRepository launchDarklyFeatureFlagRepository(LDClient client) {
    return new LaunchDarklyFeatureFlagRepository(client);
  }

  /** Documentation. */
  @PreDestroy
  public void shutdown() throws IOException {
    if (ldClient != null) {
      ldClient.close();
    }
  }
}
