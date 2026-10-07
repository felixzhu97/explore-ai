package com.ai.eval.domain.model;

import java.util.List;
import lombok.Value;

/** Aggregated golden suite report. */
@Value
public class GoldenSuiteReport {
  int total;
  int passed;
  int failed;
  double passRate;
  List<CaseEvalOutcome> cases;

  public GoldenSuiteReport(
      int total, int passed, int failed, double passRate, List<CaseEvalOutcome> cases) {
    cases = cases == null ? List.of() : List.copyOf(cases);
    if (total < 0) {
      throw new IllegalArgumentException("total must be >= 0");
    }
    this.total = total;
    this.passed = passed;
    this.failed = failed;
    this.passRate = passRate;
    this.cases = cases;
  }

  /** Builds a report counting passed and failed outcomes and computing the pass rate. */
  public static GoldenSuiteReport createReport(List<CaseEvalOutcome> outcomes) {
    List<CaseEvalOutcome> cases = outcomes == null ? List.of() : List.copyOf(outcomes);
    int total = cases.size();
    int passed = (int) cases.stream().filter(CaseEvalOutcome::isPassed).count();
    int failed = total - passed;
    double passRate = total == 0 ? 0.0 : (double) passed / total;
    return new GoldenSuiteReport(total, passed, failed, passRate, cases);
  }
}
