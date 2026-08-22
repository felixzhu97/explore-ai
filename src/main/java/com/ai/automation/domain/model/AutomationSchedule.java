package com.ai.automation.domain.model;

import com.ai.automation.domain.vo.AutomationActionType;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.automation.domain.vo.ScheduleKind;
import com.ai.common.domain.model.AbstractNamedOwnerEntity;
import com.ai.common.domain.vo.DomainStrings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Automation schedule aggregate with custom enable semantics. */
@Entity
@Table(name = "automation_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AutomationSchedule extends AbstractNamedOwnerEntity<ScheduleId> {

  /** Provisional / terminal next_run_at for one-shot schedules after claim or completion. */
  public static final Instant ONCE_TERMINAL_NEXT = Instant.parse("9999-12-31T23:59:59Z");

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final int MAX_BRIEF = 4000;

  @Enumerated(EnumType.STRING)
  @Column(name = "schedule_kind", nullable = false, length = 20)
  private ScheduleKind scheduleKind;

  @Column(name = "cron_expression", length = 80)
  private String cronExpression;

  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 40, updatable = false)
  private AutomationActionType actionType;

  @Column(name = "workflow_template_id", nullable = false, length = 36)
  private String workflowTemplateId;

  @Column(name = "recipient_email", nullable = false, length = 320)
  private String recipientEmail;

  @Column(name = "brief", nullable = false, columnDefinition = "clob")
  private String brief;

  @Column(name = "next_run_at", nullable = false)
  private Instant nextRunAt;

  @Column(name = "last_run_at")
  private Instant lastRunAt;

  private AutomationSchedule(
      ScheduleId id,
      String ownerKey,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      String timezone,
      boolean enabled,
      AutomationActionType actionType,
      String workflowTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt,
      Instant lastRunAt,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, createdAt, updatedAt);
    this.scheduleKind = Objects.requireNonNull(scheduleKind, "scheduleKind");
    this.cronExpression = normalizeCron(scheduleKind, cronExpression);
    this.timezone = DomainStrings.requireNonBlank(timezone, "timezone");
    this.enabled = enabled;
    this.actionType = Objects.requireNonNull(actionType, "actionType");
    this.workflowTemplateId =
        DomainStrings.requireNonBlank(workflowTemplateId, "workflowTemplateId");
    this.recipientEmail = requireEmail(recipientEmail);
    this.brief = requireBrief(brief);
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    this.lastRunAt = lastRunAt;
  }

  /** Documentation. */
  public static AutomationSchedule create(
      String ownerKey,
      String name,
      String cronExpression,
      String timezone,
      String workflowTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt) {
    Instant now = Instant.now();
    return new AutomationSchedule(
        ScheduleId.generate(),
        ownerKey,
        name,
        ScheduleKind.CRON,
        cronExpression,
        timezone,
        true,
        AutomationActionType.RUN_SAVED_WORKFLOW,
        workflowTemplateId,
        recipientEmail,
        brief,
        nextRunAt,
        null,
        now,
        now);
  }

  /** Documentation. */
  public static AutomationSchedule createOnce(
      String ownerKey,
      String name,
      String timezone,
      String workflowTemplateId,
      String recipientEmail,
      String brief,
      Instant runAt) {
    Instant now = Instant.now();
    Objects.requireNonNull(runAt, "runAt");
    if (!runAt.isAfter(now)) {
      throw new IllegalArgumentException("One-shot runAt must be in the future");
    }
    return new AutomationSchedule(
        ScheduleId.generate(),
        ownerKey,
        name,
        ScheduleKind.ONCE,
        null,
        timezone,
        true,
        AutomationActionType.RUN_SAVED_WORKFLOW,
        workflowTemplateId,
        recipientEmail,
        brief,
        runAt,
        null,
        now,
        now);
  }

  /** Documentation. */
  public static AutomationSchedule restore(
      ScheduleId id,
      String ownerKey,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      String timezone,
      boolean enabled,
      AutomationActionType actionType,
      String workflowTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt,
      Instant lastRunAt,
      Instant createdAt,
      Instant updatedAt) {
    return new AutomationSchedule(
        id,
        ownerKey,
        name,
        scheduleKind,
        cronExpression,
        timezone,
        enabled,
        actionType,
        workflowTemplateId,
        recipientEmail,
        brief,
        nextRunAt,
        lastRunAt,
        createdAt,
        updatedAt);
  }

  /** Documentation. */
  public void update(
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      String timezone,
      String workflowTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt) {
    rename(name);
    this.scheduleKind = Objects.requireNonNull(scheduleKind, "scheduleKind");
    this.cronExpression = normalizeCron(scheduleKind, cronExpression);
    this.timezone = DomainStrings.requireNonBlank(timezone, "timezone");
    this.workflowTemplateId =
        DomainStrings.requireNonBlank(workflowTemplateId, "workflowTemplateId");
    this.recipientEmail = requireEmail(recipientEmail);
    this.brief = requireBrief(brief);
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    if (this.scheduleKind == ScheduleKind.ONCE && !ONCE_TERMINAL_NEXT.equals(this.nextRunAt)) {
      this.enabled = true;
    }
    touchUpdatedAt();
  }

  /** Documentation. */
  public void enable(Instant nextRunAt) {
    this.enabled = true;
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    touchUpdatedAt();
  }

  /** Documentation. */
  public void disable() {
    this.enabled = false;
    touchUpdatedAt();
  }

  /** Documentation. */
  public void markExecuted(Instant finishedAt, Instant nextRunAt) {
    this.lastRunAt = Objects.requireNonNull(finishedAt, "finishedAt");
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    touchUpdatedAt();
  }

  /** Documentation. */
  public void completeOnce(Instant finishedAt) {
    markExecuted(finishedAt, ONCE_TERMINAL_NEXT);
    disable();
  }

  /** Documentation. */
  public boolean isOnce() {
    return scheduleKind == ScheduleKind.ONCE;
  }

  private static String normalizeCron(ScheduleKind kind, String cronExpression) {
    if (kind == ScheduleKind.ONCE) {
      return null;
    }
    return DomainStrings.requireNonBlank(cronExpression, "cronExpression");
  }

  private static String requireEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Recipient email cannot be null or blank");
    }
    String trimmed = email.trim().toLowerCase(Locale.ROOT);
    if (trimmed.length() > 320 || !EMAIL_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Recipient email is invalid");
    }
    return trimmed;
  }

  private static String requireBrief(String brief) {
    return DomainStrings.truncate(DomainStrings.requireNonBlank(brief, "brief"), MAX_BRIEF);
  }
}
