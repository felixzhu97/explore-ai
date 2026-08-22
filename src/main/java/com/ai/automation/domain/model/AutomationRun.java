package com.ai.automation.domain.model;

import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.domain.vo.RunId;
import com.ai.automation.domain.vo.RunStatus;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.model.AbstractTimedRunEntity;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.domain.vo.OwnerKeyAttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Automation execution run record partitioned by owner_key. */
@Entity
@Table(name = "automation_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AutomationRun extends AbstractTimedRunEntity<RunId> {

  public static final int MAX_RESULT_EXCERPT = 16_384;

  @Embedded
  @AttributeOverride(
      name = "value",
      column = @Column(name = "schedule_id", nullable = false, length = 36))
  private ScheduleId scheduleId;

  @Convert(converter = OwnerKeyAttributeConverter.class)
  @Column(name = "owner_key", nullable = false, length = 80)
  private OwnerKey ownerKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RunStatus status;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "result_excerpt", columnDefinition = "clob")
  private String resultExcerpt;

  @Enumerated(EnumType.STRING)
  @Column(name = "email_status", nullable = false, length = 20)
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
    super(id, startedAt, finishedAt);
    this.scheduleId = Objects.requireNonNull(scheduleId, "scheduleId");
    this.ownerKey = OwnerKey.parse(ownerKey);
    this.status = Objects.requireNonNull(status, "status");
    this.errorMessage = errorMessage;
    this.resultExcerpt = resultExcerpt;
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
  }

  /** Documentation. */
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

  /** Documentation. */
  public static AutomationRun restore(
      RunId id,
      ScheduleId scheduleId,
      String ownerKey,
      Instant startedAt,
      Instant finishedAt,
      RunStatus status,
      String errorMessage,
      String resultExcerpt,
      EmailDeliveryStatus emailStatus) {
    return new AutomationRun(
        id,
        scheduleId,
        ownerKey,
        startedAt,
        finishedAt,
        status,
        errorMessage,
        resultExcerpt,
        emailStatus);
  }

  /** Documentation. */
  public void succeed(String resultExcerpt, EmailDeliveryStatus emailStatus) {
    this.status = RunStatus.SUCCESS;
    this.resultExcerpt = truncate(resultExcerpt);
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
    markFinished(Instant.now());
    this.errorMessage = null;
  }

  /** Documentation. */
  public void fail(String errorMessage, EmailDeliveryStatus emailStatus) {
    this.status = RunStatus.FAILED;
    this.errorMessage = truncateMessage(errorMessage);
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
    markFinished(Instant.now());
  }

  /** Documentation. */
  public void skip(String reason) {
    this.status = RunStatus.SKIPPED;
    this.errorMessage = truncateMessage(reason);
    this.emailStatus = EmailDeliveryStatus.SKIPPED;
    markFinished(Instant.now());
  }

  /** Returns the persisted owner_key value (c:… or u:…). */
  public String getClientId() {
    return ownerKey.value();
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
