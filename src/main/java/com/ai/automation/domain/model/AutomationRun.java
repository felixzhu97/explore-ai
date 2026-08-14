package com.ai.automation.domain.model;

import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.domain.vo.RunId;
import com.ai.automation.domain.vo.RunStatus;
import com.ai.automation.domain.vo.ScheduleId;
import java.time.Instant;
import java.util.Objects;

/** Documentation. */
public class AutomationRun {

  public static final int MAX_RESULT_EXCERPT = 16_384;

  private final RunId id;
  private final ScheduleId scheduleId;
  private final String clientId;
  private final Instant startedAt;
  private Instant finishedAt;
  private RunStatus status;
  private String errorMessage;
  private String resultExcerpt;
  private EmailDeliveryStatus emailStatus;

  private AutomationRun(
      RunId id,
      ScheduleId scheduleId,
      String clientId,
      Instant startedAt,
      Instant finishedAt,
      RunStatus status,
      String errorMessage,
      String resultExcerpt,
      EmailDeliveryStatus emailStatus) {
    this.id = Objects.requireNonNull(id, "RunId");
    this.scheduleId = Objects.requireNonNull(scheduleId, "ScheduleId");
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.finishedAt = finishedAt;
    this.status = Objects.requireNonNull(status, "status");
    this.errorMessage = errorMessage;
    this.resultExcerpt = resultExcerpt;
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
  }

  /** Documentation. */
  public static AutomationRun start(ScheduleId scheduleId, String clientId) {
    return new AutomationRun(
        RunId.generate(),
        scheduleId,
        clientId,
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
      String clientId,
      Instant startedAt,
      Instant finishedAt,
      RunStatus status,
      String errorMessage,
      String resultExcerpt,
      EmailDeliveryStatus emailStatus) {
    return new AutomationRun(
        id,
        scheduleId,
        clientId,
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
    this.finishedAt = Instant.now();
    this.errorMessage = null;
  }

  /** Documentation. */
  public void fail(String errorMessage, EmailDeliveryStatus emailStatus) {
    this.status = RunStatus.FAILED;
    this.errorMessage = truncateMessage(errorMessage);
    this.emailStatus = Objects.requireNonNull(emailStatus, "emailStatus");
    this.finishedAt = Instant.now();
  }

  /** Documentation. */
  public void skip(String reason) {
    this.status = RunStatus.SKIPPED;
    this.errorMessage = truncateMessage(reason);
    this.emailStatus = EmailDeliveryStatus.SKIPPED;
    this.finishedAt = Instant.now();
  }

  public RunId getId() {
    return id;
  }

  public ScheduleId getScheduleId() {
    return scheduleId;
  }

  public String getClientId() {
    return clientId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public RunStatus getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getResultExcerpt() {
    return resultExcerpt;
  }

  public EmailDeliveryStatus getEmailStatus() {
    return emailStatus;
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
