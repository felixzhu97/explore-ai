package com.ai.common.infrastructure.featureflag;

import com.launchdarkly.sdk.server.LDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

final class LaunchDarklyClientSupport {

    private static final Logger log = LoggerFactory.getLogger(LaunchDarklyClientSupport.class);

    private LaunchDarklyClientSupport() {
    }

    static void waitForInitialization(LDClient client, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!client.isInitialized()) {
            if (System.nanoTime() > deadline) {
                log.warn("LaunchDarkly client failed to initialize within {}", timeout);
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
