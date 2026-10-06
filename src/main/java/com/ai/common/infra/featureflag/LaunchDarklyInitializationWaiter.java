package com.ai.common.infra.featureflag;

import com.launchdarkly.sdk.server.LDClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LaunchDarklyInitializationWaiter {

  private static final Logger log = LoggerFactory.getLogger(LaunchDarklyInitializationWaiter.class);

  private LaunchDarklyInitializationWaiter() {}

  /** Waits until the client is ready or the timeout passes. */
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
