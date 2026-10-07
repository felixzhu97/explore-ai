package com.ai.automation.domain.model;

import com.ai.common.domain.model.AbstractOwnerAwareImmutable;
import com.ai.common.domain.model.DomainStrings;
import com.ai.common.domain.model.OwnerKey;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/** Automation execution run record partitioned by owner_key. */
@Entity
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AutomationRun extends AbstractOwnerAwareImmutable<RunId> {

  public static final int MAX_RESULT_EXCERPT = 16_384;
  private static final String QUOTA_EXCEEDED = "Daily plan quota exceeded";

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "schedule_id", nullable = false))
  private ScheduleId scheduleId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RunStatus status;

  @Column(length = 1000)
  private String errorMessage;

  @Column(columnDefinition = "clob")
  private String resultExcerpt;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EmailDeliveryStatus emailStatus;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Instant startedAt;

  @Column private Instant finishedAt;

  private AutomationRun(ScheduleId scheduleId, String ownerKey, Instant startedAt) {
    super(RunId.generateId(), OwnerKey.parseKey(ownerKey));
    this.scheduleId = Objects.requireNonNull(scheduleId, "scheduleId");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.status = RunStatus.FAILED;
    this.emailStatus = EmailDeliveryStatus.PENDING;
  }

  /** Starts a run for the schedule now, provisionally failed with its email pending. */
  public static AutomationRun startRun(ScheduleId scheduleId, String ownerKey) {
    return startRun(scheduleId, ownerKey, Instant.now());
  }

  /** Starts a run for the schedule at the time, provisionally failed with its email pending. */
  public static AutomationRun startRun(ScheduleId scheduleId, String ownerKey, Instant startedAt) {
    return new AutomationRun(scheduleId, ownerKey, startedAt);
  }

  /** Tells whether the run has finished. */
  public boolean isFinished() {
    return finishedAt != null;
  }

  /** Marks the run successful with a truncated result excerpt and stamps its finish time. */
  public void markSucceeded(String resultExcerpt, EmailDeliveryStatus emailStatus) {
    requireRunning();
    this.status = RunStatus.SUCCESS;
    this.resultExcerpt = DomainStrings.truncate(resultExcerpt, MAX_RESULT_EXCERPT);
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
    this.finishedAt = Instant.now();
    this.errorMessage = null;
  }

  /** Marks the run failed before any email was sent, keeping a truncated error message. */
  public void markFailedBeforeEmail(String errorMessage) {
    finishWithoutEmail(RunStatus.FAILED, errorMessage);
  }

  /** Marks the run skipped because the owner used up the daily plan quota. */
  public void markSkippedForQuota() {
    finishWithoutEmail(RunStatus.SKIPPED, QUOTA_EXCEEDED);
  }

  private void finishWithoutEmail(RunStatus finalStatus, String reason) {
    requireRunning();
    this.status = finalStatus;
    this.errorMessage = truncateMessage(reason);
    this.emailStatus = EmailDeliveryStatus.SKIPPED;
    this.finishedAt = Instant.now();
  }

  private void requireRunning() {
    if (isFinished()) {
      throw new IllegalStateException("Automation run already finished: " + getId().getValue());
    }
  }

  private static String truncateMessage(String value) {
    if (value == null || value.isBlank()) {
      return "unknown error";
    }
    return DomainStrings.truncate(value.trim(), 1000);
  }
}
