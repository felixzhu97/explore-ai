package com.ai.metrics.domain.vo;

/** Elapsed milliseconds of an invocation, never negative. */
public record Latency(long millis) {

  private static final long NANOS_PER_MILLI = 1_000_000L;

  public Latency {
    millis = Math.max(0L, millis);
  }

  /** Wraps a measured duration in milliseconds. */
  public static Latency ofMillis(long millis) {
    return new Latency(millis);
  }

  /** Measures from a {@link System#nanoTime()} reading until now. */
  public static Latency since(long startNanos) {
    return between(startNanos, System.nanoTime());
  }

  /** Measures between two {@link System#nanoTime()} readings. */
  public static Latency between(long startNanos, long endNanos) {
    return new Latency((endNanos - startNanos) / NANOS_PER_MILLI);
  }
}
