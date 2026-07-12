package com.ai.common.infrastructure.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import com.launchdarkly.sdk.server.LDClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "launchdarkly.enabled", havingValue = "true", matchIfMissing = true)
public class LaunchDarklyConfig {

    private static final Logger log = LoggerFactory.getLogger(LaunchDarklyConfig.class);

    private LDClient ldClient;

    @Bean
    @ConditionalOnExpression("'${launchdarkly.sdk-key:}'.length() > 0")
    public LDClient ldClient(LaunchDarklyProperties properties) {
        ldClient = new LDClient(properties.getSdkKey());
        LaunchDarklyClientSupport.waitForInitialization(ldClient, Duration.ofSeconds(5));
        return ldClient;
    }

    @PreDestroy
    public void shutdown() throws IOException {
        if (ldClient != null) {
            ldClient.close();
        }
    }
}
