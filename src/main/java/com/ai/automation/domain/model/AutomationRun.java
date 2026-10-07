package com.ai.automation.domain.model;

import com.ai.common.domain.model.AbstractOwnerKeyedRunEntity;
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
public class AutomationRun extends AbstractOwnerKeyedRunEntity<RunId> {

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

  private AutomationRun(
      RunId id,
      ScheduleId scheduleId,
      String ownerKey,
      Instant startedAt,
      Instant finishedAt,
      RunStatus status,
      String errorMessage,
      String resultExcerpt,
      EmailDeliveryStatus emailStatus) {
    super(id, OwnerKey.parse(ownerKey), startedAt, finishedAt);
    this.scheduleId = Objects.requireNonNull(scheduleId, "scheduleId");
    this.status = Objects.requireNonNull(status, "status");
    this.errorMessage = errorMessage;
    this.resultExcerpt = resultExcerpt;
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
  }

  /** Starts a run for the schedule now, provisionally failed with its email pending. */
  public static AutomationRun start(ScheduleId scheduleId, String ownerKey) {
    return new AutomationRun(
        RunId.generate(),
        scheduleId,
        ownerKey,
        Instant.now(),
        null,
        RunStatus.FAILED,
        null,
        null,
        EmailDeliveryStatus.PENDING);
  }

  /** Marks the run successful with a truncated result excerpt and stamps its finish time. */
  public void succeed(String resultExcerpt, EmailDeliveryStatus emailStatus) {
    requireRunning();
    this.status = RunStatus.SUCCESS;
    this.resultExcerpt = truncate(resultExcerpt);
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
    markFinished(Instant.now());
    this.errorMessage = null;
  }

  /** Marks the run failed before any email was sent, keeping a truncated error message. */
  public void failBeforeEmail(String errorMessage) {
    finishWithoutEmail(RunStatus.FAILED, errorMessage);
  }

  /** Marks the run skipped because the owner used up the daily plan quota. */
  public void skipForQuota() {
    finishWithoutEmail(RunStatus.SKIPPED, QUOTA_EXCEEDED);
  }

  private void finishWithoutEmail(RunStatus finalStatus, String reason) {
    requireRunning();
    this.status = finalStatus;
    this.errorMessage = truncateMessage(reason);
    this.emailStatus = EmailDeliveryStatus.SKIPPED;
    markFinished(Instant.now());
  }

  private void requireRunning() {
    if (isFinished()) {
      throw new IllegalStateException("Automation run already finished: " + getId().getValue());
    }
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    if (value.length() <= MAX_RESULT_EXCERPT) {
      return value;
    }
    return value.substring(0, MAX_RESULT_EXCERPT);
  }

  private static String truncateMessage(String value) {
    if (value == null || value.isBlank()) {
      return "unknown error";
    }
    String trimmed = value.trim();
    return trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed;
  }
}
