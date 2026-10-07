package com.ai.common.infra.featureflag;

import com.launchdarkly.sdk.server.LDClient;
import java.time.Duration;

final class LaunchDarklyInitializationWaiter {

  private LaunchDarklyInitializationWaiter() {}

  /** Waits until the client is ready or the timeout passes. */
  static void waitForInitialization(LDClient client, Duration timeout) {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (!client.isInitialized()) {
      if (System.nanoTime() > deadline) {
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
