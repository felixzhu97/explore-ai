package com.ai.automation.domain.model;

import com.ai.common.domain.model.AbstractEnableableNamedOwnerEntity;
import com.ai.common.domain.model.DomainStrings;
import com.ai.pipeline.domain.model.PipelineTemplateId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/** Automation schedule aggregate; it keeps its next run in step with its timing. */
@Entity
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AutomationSchedule extends AbstractEnableableNamedOwnerEntity<ScheduleId> {

  /** next_run_at for a one-off schedule that has been claimed or has finished. */
  private static final Instant ONCE_TERMINAL_NEXT = Instant.parse("9999-12-31T23:59:59Z");

  private static final String SUBJECT_PREFIX = "[ExploreAI] ";
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final int MAX_BRIEF = 4000;

  @NotNull @Valid @Embedded private ScheduleTiming timing;

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
      String ownerKey,
      String name,
      ScheduleTiming timing,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant nextRunAt,
      Instant now) {
    super(ScheduleId.generate(), ownerKey, name, true, now, now);
    this.timing = Objects.requireNonNull(timing, "timing");
    this.actionType = AutomationActionType.RUN_PIPELINE_TEMPLATE;
    this.pipelineTemplateId = requireTemplateId(pipelineTemplateId);
    this.recipientEmail = requireEmail(recipientEmail);
    this.brief = requireBrief(brief);
    this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
  }

  /** Creates an enabled cron schedule whose first run is the next fire time after {@code now}. */
  public static AutomationSchedule create(
      String ownerKey,
      String name,
      String cronExpression,
      String timezone,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant now) {
    ScheduleTiming timing = ScheduleTiming.cron(cronExpression, timezone);
    return new AutomationSchedule(
        ownerKey,
        name,
        timing,
        pipelineTemplateId,
        recipientEmail,
        brief,
        timing.nextRunAfter(now),
        now);
  }

  /** Creates an enabled one-off schedule that runs at {@code runAt}, which must be later. */
  public static AutomationSchedule createOnce(
      String ownerKey,
      String name,
      String timezone,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant runAt,
      Instant now) {
    return new AutomationSchedule(
        ownerKey,
        name,
        ScheduleTiming.once(timezone),
        pipelineTemplateId,
        recipientEmail,
        brief,
        requireFutureRunAt(runAt, now),
        now);
  }

  /**
   * Replaces the settings and re-arms the next run. A one-off schedule turns back on because it now
   * has a run to come.
   */
  public void update(
      String name,
      ScheduleTiming timing,
      Instant runAt,
      String pipelineTemplateId,
      String recipientEmail,
      String brief,
      Instant now) {
    rename(name);
    this.timing = Objects.requireNonNull(timing, "timing");
    this.pipelineTemplateId = requireTemplateId(pipelineTemplateId);
    this.recipientEmail = requireEmail(recipientEmail);
    this.brief = requireBrief(brief);
    if (timing.isOnce()) {
      this.nextRunAt = requireFutureRunAt(runAt, now);
      this.enabled = true;
    } else {
      this.nextRunAt = timing.nextRunAfter(now);
    }
    touchUpdatedAt();
  }

  /**
   * Turns the schedule on and re-arms its next run.
   *
   * @throws IllegalArgumentException when a one-off schedule has no run left
   */
  public void turnOn(Instant now) {
    if (isOnce()) {
      if (!hasPendingRun(now)) {
        throw new IllegalArgumentException(
            "One-shot schedule already completed; set a new runAt before enabling");
      }
    } else {
      this.nextRunAt = timing.nextRunAfter(now);
    }
    this.enabled = true;
    touchUpdatedAt();
  }

  /** Not supported: use {@link #turnOn} so the next run is re-armed. */
  @Override
  public void enable() {
    throw new UnsupportedOperationException("Use turnOn so the next run is re-armed");
  }

  /** Tells whether a run is still to come after {@code now}; a cron schedule always has one. */
  public boolean hasPendingRun(Instant now) {
    return !isOnce() || pendingRunAt().filter(runAt -> runAt.isAfter(now)).isPresent();
  }

  /** Returns when a one-off schedule will run, or empty once it has been claimed or finished. */
  public Optional<Instant> pendingRunAt() {
    return isOnce() && !ONCE_TERMINAL_NEXT.equals(nextRunAt)
        ? Optional.of(nextRunAt)
        : Optional.empty();
  }

  /** Returns the next_run_at written while a run is claimed, so other scanners skip it. */
  public Instant provisionalNextRunAt(Instant now) {
    return isOnce() ? ONCE_TERMINAL_NEXT : timing.nextRunAfter(now);
  }

  /**
   * Records a finished run; a one-off schedule turns off, a cron schedule moves to its next run.
   */
  public void recordRunFinished(Instant finishedAt) {
    this.lastRunAt = Objects.requireNonNull(finishedAt, "finishedAt");
    if (isOnce()) {
      this.nextRunAt = ONCE_TERMINAL_NEXT;
      this.enabled = false;
    } else {
      this.nextRunAt = timing.nextRunAfter(finishedAt);
    }
    touchUpdatedAt();
  }

  /** Builds the email that delivers a run's result to the recipient. */
  public EmailMessage resultEmail(String textBody, String htmlBody) {
    return new EmailMessage(recipientEmail, SUBJECT_PREFIX + getName(), textBody, htmlBody);
  }

  /** Tells whether the schedule runs only once. */
  public boolean isOnce() {
    return timing.isOnce();
  }

  private static Instant requireFutureRunAt(Instant runAt, Instant now) {
    if (runAt == null) {
      throw new IllegalArgumentException("runAt is required for ONCE schedules");
    }
    if (!runAt.isAfter(now)) {
      throw new IllegalArgumentException("One-shot runAt must be in the future");
    }
    return runAt;
  }

  private static PipelineTemplateId requireTemplateId(String pipelineTemplateId) {
    return PipelineTemplateId.of(
        DomainStrings.requireNonBlank(pipelineTemplateId, "pipelineTemplateId"));
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
