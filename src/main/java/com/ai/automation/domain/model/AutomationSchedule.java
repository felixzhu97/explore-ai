package com.ai.automation.domain.model;

import com.ai.automation.domain.vo.AutomationActionType;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.automation.domain.vo.ScheduleKind;
import com.ai.common.domain.model.AbstractEnableableNamedOwnerEntity;
import com.ai.common.domain.vo.DomainStrings;
import com.ai.pipeline.domain.vo.PipelineTemplateId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/** Automation schedule aggregate; enabling it re-arms the next run. */
@Entity
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AutomationSchedule extends AbstractEnableableNamedOwnerEntity<ScheduleId> {

  /** Provisional / terminal next_run_at for one-shot schedules after claim or completion. */
  public static final Instant ONCE_TERMINAL_NEXT = Instant.parse("9999-12-31T23:59:59Z");

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final int MAX_BRIEF = 4000;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleKind scheduleKind;

  @Size(max = 80)
  @Column(length = 80)
  private String cronExpression;

  @NotBlank
  @Size(max = 64)
  @Column(nullable = false, length = 64)
  private String timezone;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40, updatable = false)
  private AutomationActionType actionType;

  /** References a PipelineTemplate by id only; the template is a separate aggregate. */
  @NotNull
  @Embedded
  @AttributeOverride(
      name = "value",
      column = @Column(name = "pipeline_template_id", nullable = false))
  private PipelineTemplateId pipelineTemplateId;

  @NotBlank
  @Size(max = 320)
  @Column(nullable = false, length = 320)
  private String recipientEmail;

  @NotBlank
  @Column(nullable = false, columnDefinition = "clob")
  private String brief;

  @NotNull
  @Column(nullable = false)
  private Instant nextRunAt;

  @Column private Instant lastRunAt;

  private AutomationSchedule(
      ScheduleId id,
      String ownerKey,
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      String timezone,
      boolean enabled,
      AutomationActionType actionType,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt,
      Instant lastRunAt,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, enabled, createdAt, updatedAt);
    this.scheduleKind = Objects.requireNonNull(scheduleKind, "scheduleKind");
    this.cronExpression = normalizeCron(scheduleKind, cronExpression);
    this.timezone = DomainStrings.requireNonBlank(timezone, "timezone");
    this.actionType = Objects.requireNonNull(actionType, "actionType");
    this.pipelineTemplateId =
        PipelineTemplateId.of(
            DomainStrings.requireNonBlank(pipelineTemplateId, "pipelineTemplateId"));
    this.recipientEmail = requireEmail(recipientEmail);
    this.brief = requireBrief(brief);
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    this.lastRunAt = lastRunAt;
  }

  /**
   * Creates an enabled cron schedule that runs a saved pipeline template starting at {@code
   * nextRunAt}.
   */
  public static AutomationSchedule create(
      String ownerKey,
      String name,
      String cronExpression,
      String timezone,
      String pipelineTemplateId,
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
        AutomationActionType.RUN_PIPELINE_TEMPLATE,
        pipelineTemplateId,
        recipientEmail,
        brief,
        nextRunAt,
        null,
        now,
        now);
  }

  /**
   * Creates an enabled one-off schedule that runs a saved pipeline template at a future instant.
   */
  public static AutomationSchedule createOnce(
      String ownerKey,
      String name,
      String timezone,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant runAt) {
    Instant now = Instant.now();
    requireFutureRunAt(runAt, now);
    return new AutomationSchedule(
        ScheduleId.generate(),
        ownerKey,
        name,
        ScheduleKind.ONCE,
        null,
        timezone,
        true,
        AutomationActionType.RUN_PIPELINE_TEMPLATE,
        pipelineTemplateId,
        recipientEmail,
        brief,
        runAt,
        null,
        now,
        now);
  }

  /** Returns {@code runAt} when it is set and later than {@code now}. */
  public static Instant requireFutureRunAt(Instant runAt, Instant now) {
    if (runAt == null) {
      throw new IllegalArgumentException("runAt is required for ONCE schedules");
    }
    if (!runAt.isAfter(now)) {
      throw new IllegalArgumentException("One-shot runAt must be in the future");
    }
    return runAt;
  }

  /** Replaces the schedule settings and next run, re-enabling a one-off with a pending run. */
  public void update(
      String name,
      ScheduleKind scheduleKind,
      String cronExpression,
      String timezone,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt) {
    rename(name);
    this.scheduleKind = Objects.requireNonNull(scheduleKind, "scheduleKind");
    this.cronExpression = normalizeCron(scheduleKind, cronExpression);
    this.timezone = DomainStrings.requireNonBlank(timezone, "timezone");
    this.pipelineTemplateId =
        PipelineTemplateId.of(
            DomainStrings.requireNonBlank(pipelineTemplateId, "pipelineTemplateId"));
    this.recipientEmail = requireEmail(recipientEmail);
    this.brief = requireBrief(brief);
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    if (this.scheduleKind == ScheduleKind.ONCE && !ONCE_TERMINAL_NEXT.equals(this.nextRunAt)) {
      this.enabled = true;
    }
    touchUpdatedAt();
  }

  public void enable(Instant nextRunAt) {
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    enable();
  }

  public void markExecuted(Instant finishedAt, Instant nextRunAt) {
    this.lastRunAt = Objects.requireNonNull(finishedAt, "finishedAt");
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
    touchUpdatedAt();
  }

  public void completeOnce(Instant finishedAt) {
    markExecuted(finishedAt, ONCE_TERMINAL_NEXT);
    disable();
  }

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
