package com.ai.metrics.domain.model;

import java.util.List;
import lombok.Value;

/** p50 and p95 latency in milliseconds; both null when nothing was measured. */
@Value
public class LatencyStats {
  Double p50Ms;
  Double p95Ms;

  public static final LatencyStats EMPTY = new LatencyStats(null, null);

  /** Computes the percentiles from latencies sorted in ascending order. */
  public static LatencyStats calculateStats(List<Long> sortedMillis) {
    if (sortedMillis.isEmpty()) {
      return EMPTY;
    }
    return new LatencyStats(
        calculatePercentile(sortedMillis, 0.50), calculatePercentile(sortedMillis, 0.95));
  }

  /** Linearly interpolated percentile of ascending values; 0 when there are none. */
  public static double calculatePercentile(List<Long> sortedValues, double percentile) {
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
