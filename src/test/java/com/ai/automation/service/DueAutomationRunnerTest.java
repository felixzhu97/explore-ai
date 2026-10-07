package com.ai.automation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.model.RunStatus;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.domain.repository.PipelineGateway;
import com.ai.automation.infra.config.AutomationProperties;
import com.ai.billing.service.DailyUsageQuotaService;
import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
@DisplayName("DueAutomationRunner")
class DueAutomationRunnerTest {

  private static final String OWNER = "c:client-1";
  private static final String TEMPLATE_ID = "11111111-1111-1111-1111-111111111111";
  private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

  @Mock private AutomationScheduleRepository scheduleRepository;
  @Mock private AutomationRunRepository runRepository;
  @Mock private PipelineGateway pipelineGateway;
  @Mock private EmailGateway emailGateway;
  @Mock private DailyUsageQuotaService dailyUsageQuotaService;

  private final AutomationProperties properties = new AutomationProperties();
  private DueAutomationRunner runner;

  @BeforeEach
  void setUp() {
    runner =
        new DueAutomationRunner(
            scheduleRepository,
            runRepository,
            pipelineGateway,
            emailGateway,
            new AutomationMailFormatter(),
            dailyUsageQuotaService,
            properties);
  }

  @Test
  @DisplayName("should run the template and email the result when a schedule is due")
  void shouldRunTheTemplateAndEmailTheResultWhenAScheduleIsDue() {
    AutomationSchedule schedule =
        AutomationSchedule.createSchedule(
            OWNER,
            "Daily",
            "0 0 9 * * *",
            "UTC",
            TEMPLATE_ID,
            "user@example.com",
            "Do the work",
            CREATED_AT);
    givenClaimed(schedule);
    when(dailyUsageQuotaService.tryConsume(OwnerKey.parseKey(OWNER))).thenReturn(true);
    when(pipelineGateway.runSavedTemplate(OWNER, TEMPLATE_ID, "Do the work", "en"))
        .thenReturn("workflow result");

    int executed = runner.executeDue();

    assertThat(executed).isEqualTo(1);
    ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailGateway).sendEmail(email.capture());
    assertThat(email.getValue().getTo()).isEqualTo("user@example.com");
    assertThat(savedRun().getStatus()).isEqualTo(RunStatus.SUCCESS);
    verify(scheduleRepository).save(schedule);
  }

  @Test
  @DisplayName("should record a skipped run when the daily quota is exhausted")
  void shouldRecordASkippedRunWhenTheDailyQuotaIsExhausted() {
    AutomationSchedule schedule =
        AutomationSchedule.createSchedule(
            OWNER,
            "Daily",
            "0 0 9 * * *",
            "UTC",
            TEMPLATE_ID,
            "user@example.com",
            "Do the work",
            CREATED_AT);
    givenClaimed(schedule);
    when(dailyUsageQuotaService.tryConsume(OwnerKey.parseKey(OWNER))).thenReturn(false);

    runner.executeDue();

    verifyNoInteractions(pipelineGateway, emailGateway);
    assertThat(savedRun().getStatus()).isEqualTo(RunStatus.SKIPPED);
  }

  @Test
  @DisplayName("should disable a one-off schedule after it runs")
  void shouldDisableAOneOffScheduleAfterItRuns() {
    AutomationSchedule schedule =
        AutomationSchedule.createOneOffSchedule(
            OWNER,
            "Once",
            "UTC",
            TEMPLATE_ID,
            "user@example.com",
            "Do once",
            CREATED_AT.plusSeconds(120),
            CREATED_AT);
    givenClaimed(schedule);
    when(dailyUsageQuotaService.tryConsume(OwnerKey.parseKey(OWNER))).thenReturn(true);
    when(pipelineGateway.runSavedTemplate(OWNER, TEMPLATE_ID, "Do once", "en"))
        .thenReturn("once result");

    runner.executeDue();

    verify(scheduleRepository).save(schedule);
    assertThat(schedule.isEnabled()).isFalse();
    assertThat(schedule.getPendingRunAt()).isEmpty();
  }

  private void givenClaimed(AutomationSchedule schedule) {
    when(scheduleRepository.findDue(
            any(Instant.class), eq(Limit.of(properties.getScanBatchSize()))))
        .thenReturn(List.of(schedule));
    when(scheduleRepository.claimSchedule(eq(schedule.getId()), eq(schedule.getNextRunAt()), any()))
        .thenReturn(true);
  }

  private AutomationRun savedRun() {
    ArgumentCaptor<AutomationRun> run = ArgumentCaptor.forClass(AutomationRun.class);
    verify(runRepository).save(run.capture());
    return run.getValue();
  }
}
