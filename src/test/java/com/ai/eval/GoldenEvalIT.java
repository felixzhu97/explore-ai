package com.ai.eval;

import com.ai.eval.application.usecase.GoldenEvalUseCase;
import com.ai.eval.domain.model.GoldenSuiteReport;
import com.ai.eval.domain.vo.GoldenEvalDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live golden evaluation against Chat and RAG generation + Spring AI evaluators.
 * Enable with {@code GOLDEN_EVAL_IT=true} when model credentials are available.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/testing.html">Evaluation Testing</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "GOLDEN_EVAL_IT", matches = "true")
@DisplayName("GoldenEvalIT")
class GoldenEvalIT {

    @Autowired
    private GoldenEvalUseCase goldenEvalUseCase;

    @Test
    @DisplayName("should_reportPassRate_when_chatGoldenSuiteRuns")
    void should_reportPassRate_when_chatGoldenSuiteRuns() {
        GoldenSuiteReport report = goldenEvalUseCase.run(List.of(GoldenEvalDomain.CHAT), List.of());
        logReport("CHAT", report);

        assertThat(report.total()).isGreaterThan(0);
        assertThat(report.cases()).isNotEmpty();
        assertThat(report.passRate()).isBetween(0.0, 1.0);
        assertThat(report.passed() + report.failed()).isEqualTo(report.total());
    }

    @Test
    @DisplayName("should_reportPassRate_when_ragGoldenSuiteRuns")
    void should_reportPassRate_when_ragGoldenSuiteRuns() {
        GoldenSuiteReport report = goldenEvalUseCase.run(List.of(GoldenEvalDomain.RAG), List.of());
        logReport("RAG", report);

        assertThat(report.total()).isGreaterThan(0);
        assertThat(report.cases()).allMatch(c -> c.domain() == GoldenEvalDomain.RAG);
        assertThat(report.passRate()).isBetween(0.0, 1.0);
    }

    private static void logReport(String label, GoldenSuiteReport report) {
        System.out.printf(
                "Golden %s: total=%d passed=%d failed=%d passRate=%.2f%n",
                label, report.total(), report.passed(), report.failed(), report.passRate());
        report.cases().forEach(c -> System.out.printf(
                "  [%s] passed=%s relevancy=%s factuality=%s error=%s%n",
                c.id(),
                c.passed(),
                c.relevancyPass(),
                c.factualityPass(),
                c.generationError()));
    }
}
