package com.ai.eval.domain.model;

import java.util.List;

/** Aggregated golden suite report. */
public record GoldenSuiteReport(
    int total, int passed, int failed, double passRate, List<CaseEvalOutcome> cases) {
  /** Documentation. */
  public GoldenSuiteReport {
    cases = cases == null ? List.of() : List.copyOf(cases);
    if (total < 0) {
      throw new IllegalArgumentException("total must be >= 0");
    }
  }

  /** Documentation. */
  public static GoldenSuiteReport of(List<CaseEvalOutcome> outcomes) {
    List<CaseEvalOutcome> cases = outcomes == null ? List.of() : List.copyOf(outcomes);
    int total = cases.size();
    int passed = (int) cases.stream().filter(CaseEvalOutcome::passed).count();
    int failed = total - passed;
    double passRate = total == 0 ? 0.0 : (double) passed / total;
    return new GoldenSuiteReport(total, passed, failed, passRate, cases);
  }
}
