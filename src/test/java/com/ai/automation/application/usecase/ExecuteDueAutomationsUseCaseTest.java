package com.ai.automation.application.usecase;

import com.ai.automation.application.AutomationMailFormatter;
import com.ai.automation.application.AutomationProperties;
import com.ai.automation.application.CronScheduleCalculator;
import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.domain.repository.WorkflowRunner;
import com.ai.automation.domain.vo.RunStatus;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.billing.application.DailyUsageQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteDueAutomationsUseCase")
class ExecuteDueAutomationsUseCaseTest {

    @Mock
    private AutomationScheduleRepository scheduleRepository;
    @Mock
    private AutomationRunRepository runRepository;
    @Mock
    private WorkflowRunner workflowRunner;
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private DailyUsageQuotaService dailyUsageQuotaService;

    private final CronScheduleCalculator cronCalculator = new CronScheduleCalculator();
    private final AutomationProperties properties = new AutomationProperties();
    private ExecuteDueAutomationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecuteDueAutomationsUseCase(
                scheduleRepository,
                runRepository,
                workflowRunner,
                emailGateway,
                new AutomationMailFormatter(),
                cronCalculator,
                dailyUsageQuotaService,
                properties);
    }

    @Test
    void shouldRunWorkflowAndEmailWhenScheduleDue() {
        Instant past = Instant.now().minusSeconds(60);
        AutomationSchedule schedule = AutomationSchedule.create(
                "client-1",
                "Daily",
                "0 0 9 * * *",
                "UTC",
                "11111111-1111-1111-1111-111111111111",
                "user@example.com",
                "Do the work",
                past);
        when(scheduleRepository.findDue(any(), anyInt())).thenReturn(List.of(schedule));
        when(scheduleRepository.claim(eq(schedule.getId()), eq(past), any())).thenReturn(true);
        when(dailyUsageQuotaService.tryConsume("client-1")).thenReturn(true);
        when(workflowRunner.runSavedWorkflow(anyString(), anyString(), anyString(), anyString()))
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
        AutomationSchedule schedule = AutomationSchedule.create(
                "client-1",
                "Daily",
                "0 0 9 * * *",
                "UTC",
                "11111111-1111-1111-1111-111111111111",
                "user@example.com",
                "Do the work",
                past);
        when(scheduleRepository.findDue(any(), anyInt())).thenReturn(List.of(schedule));
        when(scheduleRepository.claim(any(ScheduleId.class), eq(past), any())).thenReturn(true);
        when(dailyUsageQuotaService.tryConsume("client-1")).thenReturn(false);

        useCase.executeDue();

        verify(workflowRunner, never()).runSavedWorkflow(anyString(), anyString(), anyString(), anyString());
        ArgumentCaptor<AutomationRun> runCaptor = ArgumentCaptor.forClass(AutomationRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(RunStatus.SKIPPED);
    }

    @Test
    void shouldDisableOnceScheduleWhenExecuted() {
        Instant past = Instant.now().minusSeconds(60);
        AutomationSchedule schedule = AutomationSchedule.createOnce(
                "client-1",
                "Once",
                "UTC",
                "11111111-1111-1111-1111-111111111111",
                "user@example.com",
                "Do once",
                Instant.now().plusSeconds(120));
        // Force due by restoring with past nextRunAt
        schedule = AutomationSchedule.restore(
                schedule.getId(),
                schedule.getClientId(),
                schedule.getName(),
                schedule.getScheduleKind(),
                schedule.getCronExpression(),
                schedule.getTimezone(),
                true,
                schedule.getActionType(),
                schedule.getWorkflowTemplateId(),
                schedule.getRecipientEmail(),
                schedule.getBrief(),
                past,
                null,
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
        when(scheduleRepository.findDue(any(), anyInt())).thenReturn(List.of(schedule));
        when(scheduleRepository.claim(eq(schedule.getId()), eq(past), eq(AutomationSchedule.ONCE_TERMINAL_NEXT)))
                .thenReturn(true);
        when(dailyUsageQuotaService.tryConsume("client-1")).thenReturn(true);
        when(workflowRunner.runSavedWorkflow(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("once result");

        useCase.executeDue();

        ArgumentCaptor<AutomationSchedule> scheduleCaptor = ArgumentCaptor.forClass(AutomationSchedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().isEnabled()).isFalse();
        assertThat(scheduleCaptor.getValue().getNextRunAt()).isEqualTo(AutomationSchedule.ONCE_TERMINAL_NEXT);
    }
}
