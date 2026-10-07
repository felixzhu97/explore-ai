package com.ai.common.infra.featureflag;

import com.ai.common.domain.repository.FeatureFlagRepository;
import com.ai.common.infra.config.LaunchDarklyProperties;
import com.launchdarkly.sdk.server.LDClient;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "launchdarkly.enabled", havingValue = "true", matchIfMissing = true)
public class LaunchDarklyConfig {

  private LDClient ldClient;

  /** Creates the LaunchDarkly client and waits for it to start. */
  @Bean
  @ConditionalOnExpression("'${launchdarkly.sdk-key:}'.length() > 0")
  public LDClient ldClient(LaunchDarklyProperties properties) {
    ldClient = new LDClient(properties.getSdkKey());
    LaunchDarklyInitializationWaiter.waitForInitialization(ldClient, Duration.ofSeconds(5));
    return ldClient;
  }

  /** Reads feature flags from LaunchDarkly. */
  @Bean
  @ConditionalOnBean(LDClient.class)
  public FeatureFlagRepository launchDarklyFeatureFlagRepository(LDClient client) {
    return new LaunchDarklyFeatureFlagRepository(client);
  }

  /** Closes the LaunchDarkly client. */
  @PreDestroy
  public void shutdownClient() throws IOException {
    if (ldClient != null) {
      ldClient.close();
    }
  }
}
