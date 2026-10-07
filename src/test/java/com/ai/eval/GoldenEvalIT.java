package com.ai.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.eval.domain.model.GoldenEvalCategory;
import com.ai.eval.domain.model.GoldenSuiteReport;
import com.ai.eval.service.GoldenEvalService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Live golden evaluation against Chat and RAG generation + Spring AI evaluators. Enable with {@code
 * GOLDEN_EVAL_IT=true} when model credentials are available.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/testing.html">Evaluation Testing</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOLDEN_EVAL_IT", matches = "true")
@DisplayName("Golden evaluation suites")
class GoldenEvalIT {

  @Autowired private GoldenEvalService goldenEvalService;

  @Test
  @DisplayName("should report pass rate when chat golden suite runs")
  void shouldReportPassRateWhenChatGoldenSuiteRuns() {
    GoldenSuiteReport report =
        goldenEvalService.runSuite(List.of(GoldenEvalCategory.CHAT), List.of());
    logReport("CHAT", report);

    assertThat(report.getTotal()).isGreaterThan(0);
    assertThat(report.getCases()).isNotEmpty();
    assertThat(report.getPassRate()).isBetween(0.0, 1.0);
    assertThat(report.getPassed() + report.getFailed()).isEqualTo(report.getTotal());
  }

  @Test
  @DisplayName("should report pass rate when rag golden suite runs")
  void shouldReportPassRateWhenRagGoldenSuiteRuns() {
    GoldenSuiteReport report =
        goldenEvalService.runSuite(List.of(GoldenEvalCategory.RAG), List.of());
    logReport("RAG", report);

    assertThat(report.getTotal()).isGreaterThan(0);
    assertThat(report.getCases()).allMatch(c -> c.getCategory() == GoldenEvalCategory.RAG);
    assertThat(report.getPassRate()).isBetween(0.0, 1.0);
  }

  private static void logReport(String label, GoldenSuiteReport report) {
    System.out.printf(
        "Golden %s: total=%d passed=%d failed=%d passRate=%.2f%n",
        label, report.getTotal(), report.getPassed(), report.getFailed(), report.getPassRate());
    report
        .getCases()
        .forEach(
            c ->
                System.out.printf(
                    "  [%s] passed=%s relevancy=%s factuality=%s error=%s%n",
                    c.getId(),
                    c.isPassed(),
                    c.isRelevancyPassed(),
                    c.getFactualityPassed(),
                    c.getGenerationError()));
  }
}
