package com.ai.automation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.model.RunStatus;
import com.ai.automation.domain.model.ScheduleId;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("DueAutomationRunner")
class DueAutomationRunnerTest {

  @Mock private AutomationScheduleRepository scheduleRepository;
  @Mock private AutomationRunRepository runRepository;
  @Mock private PipelineGateway pipelineGateway;
  @Mock private EmailGateway emailGateway;
  @Mock private DailyUsageQuotaService dailyUsageQuotaService;

  private final AutomationProperties properties = new AutomationProperties();
  private DueAutomationRunner useCase;

  @BeforeEach
  void setUp() {
    useCase =
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
  void shouldRunWorkflowAndEmailWhenScheduleDue() {
    Instant past = Instant.now().minusSeconds(60);
    AutomationSchedule schedule =
        AutomationSchedule.create(
            "c:client-1",
            "Daily",
            "0 0 9 * * *",
            "UTC",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Do the work",
            Instant.now());
    ReflectionTestUtils.setField(schedule, "nextRunAt", past);
    when(scheduleRepository.findDue(any(), any(Limit.class))).thenReturn(List.of(schedule));
    when(scheduleRepository.claim(eq(schedule.getId()), eq(past), any())).thenReturn(true);
    when(dailyUsageQuotaService.tryConsume(OwnerKey.parse("c:client-1"))).thenReturn(true);
    when(pipelineGateway.runSavedTemplate(anyString(), anyString(), anyString(), anyString()))
        .thenReturn("workflow result");

    int executed = useCase.executeDue();

    assertThat(executed).isEqualTo(1);
    verify(emailGateway).send(any(EmailMessage.class));
    ArgumentCaptor<AutomationRun> runCaptor = ArgumentCaptor.forClass(AutomationRun.class);
    verify(runRepository).save(runCaptor.capture());
    assertThat(runCaptor.getValue().getStatus()).isEqualTo(RunStatus.SUCCESS);
    verify(scheduleRepository).save(any(AutomationSchedule.class));
  }

  @Test
  void shouldSkipRunWhenQuotaExceeded() {
    Instant past = Instant.now().minusSeconds(60);
    AutomationSchedule schedule =
        AutomationSchedule.create(
            "c:client-1",
            "Daily",
            "0 0 9 * * *",
            "UTC",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Do the work",
            Instant.now());
    ReflectionTestUtils.setField(schedule, "nextRunAt", past);
    when(scheduleRepository.findDue(any(), any(Limit.class))).thenReturn(List.of(schedule));
    when(scheduleRepository.claim(any(ScheduleId.class), eq(past), any())).thenReturn(true);
    when(dailyUsageQuotaService.tryConsume(OwnerKey.parse("c:client-1"))).thenReturn(false);

    useCase.executeDue();

    verify(pipelineGateway, never())
        .runSavedTemplate(anyString(), anyString(), anyString(), anyString());
    ArgumentCaptor<AutomationRun> runCaptor = ArgumentCaptor.forClass(AutomationRun.class);
    verify(runRepository).save(runCaptor.capture());
    assertThat(runCaptor.getValue().getStatus()).isEqualTo(RunStatus.SKIPPED);
  }

  @Test
  void shouldDisableOnceScheduleWhenExecuted() {
    Instant past = Instant.now().minusSeconds(60);
    AutomationSchedule schedule =
        AutomationSchedule.createOnce(
            "c:client-1",
            "Once",
            "UTC",
            "11111111-1111-1111-1111-111111111111",
            "user@example.com",
            "Do once",
            Instant.now().plusSeconds(120),
            Instant.now());
    ReflectionTestUtils.setField(schedule, "nextRunAt", past);
    when(scheduleRepository.findDue(any(), any(Limit.class))).thenReturn(List.of(schedule));
    when(scheduleRepository.claim(eq(schedule.getId()), eq(past), any())).thenReturn(true);
    when(dailyUsageQuotaService.tryConsume(OwnerKey.parse("c:client-1"))).thenReturn(true);
    when(pipelineGateway.runSavedTemplate(anyString(), anyString(), anyString(), anyString()))
        .thenReturn("once result");

    useCase.executeDue();

    ArgumentCaptor<AutomationSchedule> scheduleCaptor =
        ArgumentCaptor.forClass(AutomationSchedule.class);
    verify(scheduleRepository).save(scheduleCaptor.capture());
    assertThat(scheduleCaptor.getValue().isEnabled()).isFalse();
    assertThat(scheduleCaptor.getValue().pendingRunAt()).isEmpty();
  }
}
