package com.ai.metrics.domain.model;

/** Request and error counts of invocations, with the derived rates. */
public record InvocationStats(long requests, long errors) {

  public InvocationStats {
    if (requests < 0 || errors < 0 || errors > requests) {
      throw new IllegalArgumentException("errors must be between 0 and requests");
    }
  }

  /** Returns the share of failed requests, 0 when there were none. */
  public double calculateErrorRate() {
    return requests == 0 ? 0.0 : (double) errors / requests;
  }

  /** Returns the share of successful requests, 1 when there were none. */
  public double calculateSuccessRate() {
    return 1.0 - calculateErrorRate();
  }
}
