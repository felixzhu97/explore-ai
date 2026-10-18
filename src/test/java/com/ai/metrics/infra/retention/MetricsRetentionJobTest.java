package com.ai.metrics.infra.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsRetentionJob")
class MetricsRetentionJobTest {

  @Mock private AiInvocationEventRepository invocationEventRepository;

  private MetricsRetentionProperties properties;
  private MetricsRetentionJob job;

  @BeforeEach
  void setUp() {
    properties = new MetricsRetentionProperties();
    properties.setSessionMaxAge(Duration.ofDays(90));
    job = new MetricsRetentionJob(properties, invocationEventRepository);
  }

  @Test
  @DisplayName("should delete events older than the retention period when enabled")
  void shouldDeleteEventsOlderThanTheRetentionPeriodWhenEnabled() {
    job.purgeExpiredEvents();

    ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
    verify(invocationEventRepository).deleteOlderThan(cutoff.capture());
    assertThat(cutoff.getValue()).isBefore(Instant.now().minus(Duration.ofDays(89)));
  }

  @Test
  @DisplayName("should keep events when retention is disabled")
  void shouldKeepEventsWhenRetentionIsDisabled() {
    properties.setEnabled(false);

    job.purgeExpiredEvents();

    verify(invocationEventRepository, never()).deleteOlderThan(any());
  }
}
