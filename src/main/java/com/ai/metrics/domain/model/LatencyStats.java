package com.ai.metrics.domain.model;

import java.util.List;

/** p50 and p95 latency in milliseconds; both null when nothing was measured. */
public record LatencyStats(Double p50Ms, Double p95Ms) {

  public static final LatencyStats EMPTY = new LatencyStats(null, null);

  /** Computes the percentiles from latencies sorted in ascending order. */
  public static LatencyStats fromSorted(List<Long> sortedMillis) {
    if (sortedMillis.isEmpty()) {
      return EMPTY;
    }
    return new LatencyStats(percentile(sortedMillis, 0.50), percentile(sortedMillis, 0.95));
  }

  /** Linearly interpolated percentile of ascending values; 0 when there are none. */
  public static double percentile(List<Long> sortedValues, double percentile) {
    if (sortedValues.isEmpty()) {
      return 0.0;
    }
    double rank = percentile * (sortedValues.size() - 1);
    int low = (int) Math.floor(rank);
    int high = (int) Math.ceil(rank);
    double weight = rank - low;
    return sortedValues.get(low) * (1 - weight) + sortedValues.get(high) * weight;
  }
}
