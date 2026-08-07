package com.ai.automation.domain.model;

import com.ai.automation.domain.vo.AutomationActionType;
import com.ai.automation.domain.vo.ScheduleId;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class AutomationSchedule {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_NAME = 120;
    private static final int MAX_BRIEF = 4000;

    private final ScheduleId id;
    private final String clientId;
    private String name;
    private String cronExpression;
    private String timezone;
    private boolean enabled;
    private final AutomationActionType actionType;
    private String workflowTemplateId;
    private String recipientEmail;
    private String brief;
    private Instant nextRunAt;
    private Instant lastRunAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private AutomationSchedule(
            ScheduleId id,
            String clientId,
            String name,
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
        this.id = Objects.requireNonNull(id, "ScheduleId cannot be null");
        this.clientId = requireClientId(clientId);
        this.name = requireName(name);
        this.cronExpression = requireCron(cronExpression);
        this.timezone = requireTimezone(timezone);
        this.enabled = enabled;
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.workflowTemplateId = requireWorkflowTemplateId(workflowTemplateId);
        this.recipientEmail = requireEmail(recipientEmail);
        this.brief = requireBrief(brief);
        this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
        this.lastRunAt = lastRunAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static AutomationSchedule create(
            String clientId,
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
                clientId,
                name,
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

    public static AutomationSchedule restore(
            ScheduleId id,
            String clientId,
            String name,
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
                clientId,
                name,
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

    public void update(
            String name,
            String cronExpression,
            String timezone,
            String workflowTemplateId,
            String recipientEmail,
            String brief,
            Instant nextRunAt) {
        this.name = requireName(name);
        this.cronExpression = requireCron(cronExpression);
        this.timezone = requireTimezone(timezone);
        this.workflowTemplateId = requireWorkflowTemplateId(workflowTemplateId);
        this.recipientEmail = requireEmail(recipientEmail);
        this.brief = requireBrief(brief);
        this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
        this.updatedAt = Instant.now();
    }

    public void enable(Instant nextRunAt) {
        this.enabled = true;
        this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public void markExecuted(Instant finishedAt, Instant nextRunAt) {
        this.lastRunAt = Objects.requireNonNull(finishedAt, "finishedAt");
        this.nextRunAt = Objects.requireNonNull(nextRunAt, "nextRunAt");
        this.updatedAt = Instant.now();
    }

    public ScheduleId getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public String getTimezone() {
        return timezone;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AutomationActionType getActionType() {
        return actionType;
    }

    public String getWorkflowTemplateId() {
        return workflowTemplateId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getBrief() {
        return brief;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("ClientId cannot be null or blank");
        }
        return clientId.trim();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Schedule name cannot be null or blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME) {
            throw new IllegalArgumentException("Schedule name cannot exceed " + MAX_NAME + " characters");
        }
        return trimmed;
    }

    private static String requireCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("Cron expression cannot be null or blank");
        }
        return cronExpression.trim();
    }

    private static String requireTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Timezone cannot be null or blank");
        }
        return timezone.trim();
    }

    private static String requireWorkflowTemplateId(String workflowTemplateId) {
        if (workflowTemplateId == null || workflowTemplateId.isBlank()) {
            throw new IllegalArgumentException("Workflow template id cannot be null or blank");
        }
        return workflowTemplateId.trim();
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
        if (brief == null || brief.isBlank()) {
            throw new IllegalArgumentException("Brief cannot be null or blank");
        }
        String trimmed = brief.trim();
        if (trimmed.length() > MAX_BRIEF) {
            return trimmed.substring(0, MAX_BRIEF);
        }
        return trimmed;
    }
}
