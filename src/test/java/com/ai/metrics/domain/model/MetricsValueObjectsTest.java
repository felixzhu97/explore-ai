package com.ai.metrics.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Metrics value objects")
class MetricsValueObjectsTest {

  @Test
  @DisplayName("should clamp latency to zero when clock goes backwards")
  void shouldClampLatencyToZeroWhenClockGoesBackwards() {
    assertThat(Latency.between(5_000_000L, 1_000_000L).millis()).isZero();
    assertThat(Latency.between(1_000_000L, 4_500_000L).millis()).isEqualTo(3L);
  }

  @Test
  @DisplayName("should collapse control characters and cap length when summarizing errors")
  void shouldCollapseControlCharactersAndCapLengthWhenSummarizingErrors() {
    ErrorSummary summary = ErrorSummary.of(" ", "a\r\n\tb" + "x".repeat(600));

    assertThat(summary.code()).isEqualTo("unknown");
    assertThat(summary.message()).startsWith("a b").hasSize(ErrorSummary.MAX_MESSAGE_LENGTH);
    assertThat(ErrorSummary.of(new IllegalStateException("boom")))
        .isEqualTo(ErrorSummary.of("IllegalStateException", "boom"));
  }

  @Test
  @DisplayName("should reject negative token counts")
  void shouldRejectNegativeTokenCounts() {
    assertThatThrownBy(() -> new TokenUsage(-1, 2)).isInstanceOf(IllegalArgumentException.class);
    assertThat(new TokenUsage(3, null).total()).isEqualTo(3L);
  }

  @Test
  @DisplayName("should report full success when there were no requests")
  void shouldReportFullSuccessWhenThereWereNoRequests() {
    InvocationStats none = new InvocationStats(0, 0);

    assertThat(none.errorRate()).isZero();
    assertThat(none.successRate()).isEqualTo(1.0);
    assertThat(new InvocationStats(4, 1).errorRate()).isEqualTo(0.25);
  }

  @Test
  @DisplayName("should reject more errors than requests")
  void shouldRejectMoreErrorsThanRequests() {
    assertThatThrownBy(() -> new InvocationStats(1, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should default to seven days when range is blank")
  void shouldDefaultToSevenDaysWhenRangeIsBlank() {
    Instant now = Instant.parse("2026-07-08T00:00:00Z");
    MetricsWindow window = MetricsWindow.parse(" ");

    assertThat(window.range()).isEqualTo("7d");
    assertThat(window.span()).isEqualTo(Duration.ofDays(7));
    assertThat(window.from(now)).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
    assertThat(MetricsWindow.parse("30D").range()).isEqualTo("30d");
  }

  @Test
  @DisplayName("should reject unsupported range")
  void shouldRejectUnsupportedRange() {
    assertThatThrownBy(() -> MetricsWindow.parse("90d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("90d");
  }

  @Test
  @DisplayName("should interpolate percentiles when latencies are sorted")
  void shouldInterpolatePercentilesWhenLatenciesAreSorted() {
    LatencyStats stats = LatencyStats.fromSorted(List.of(10L, 20L, 30L, 40L, 100L));

    assertThat(stats.p50Ms()).isEqualTo(30.0);
    assertThat(stats.p95Ms()).isEqualTo(88.0);
    assertThat(LatencyStats.fromSorted(List.of())).isEqualTo(LatencyStats.EMPTY);
  }
}
