package com.ai.common.infrastructure.featureflag;

import com.ai.common.domain.vo.ModuleFlag;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class LaunchDarklyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(LaunchDarklyEnvironmentPostProcessor.class);
    private static final LDContext SERVER_CONTEXT =
            LDContext.builder("explore-ai-server").build();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> bootstrap = new HashMap<>();
        boolean enabled = environment.getProperty("launchdarkly.enabled", Boolean.class, true);
        String sdkKey = environment.getProperty("launchdarkly.sdk-key", "");

        if (enabled && StringUtils.hasText(sdkKey)) {
            bootstrap.putAll(readFromLaunchDarkly(environment, sdkKey));
        } else {
            bootstrap.putAll(readFromFallback(environment));
        }

        environment.getPropertySources().addFirst(new MapPropertySource("launchdarklyBootstrap", bootstrap));
    }

    private Map<String, Object> readFromLaunchDarkly(ConfigurableEnvironment environment, String sdkKey) {
        Map<String, Object> bootstrap = new HashMap<>();
        LDClient client = new LDClient(sdkKey);
        try {
            LaunchDarklyClientSupport.waitForInitialization(client, Duration.ofSeconds(5));
            for (ModuleFlag flag : ModuleFlag.values()) {
                boolean fallback = readFallback(environment, flag.key());
                boolean value = client.boolVariation(flag.key(), SERVER_CONTEXT, fallback);
                bootstrap.put(flag.bootstrapProperty(), value);
            }
            log.info("LaunchDarkly bootstrap flags loaded for startup conditionals");
        } finally {
            try {
                client.close();
            } catch (IOException closeError) {
                log.warn("Failed to close LaunchDarkly bootstrap client: {}", closeError.getMessage());
            }
        }
        return bootstrap;
    }

    private Map<String, Object> readFromFallback(ConfigurableEnvironment environment) {
        Map<String, Object> bootstrap = new HashMap<>();
        for (ModuleFlag flag : ModuleFlag.values()) {
            bootstrap.put(flag.bootstrapProperty(), readFallback(environment, flag.key()));
        }
        return bootstrap;
    }

    private boolean readFallback(ConfigurableEnvironment environment, String flagKey) {
        return environment.getProperty("launchdarkly.fallback." + flagKey, Boolean.class, false);
    }
}
