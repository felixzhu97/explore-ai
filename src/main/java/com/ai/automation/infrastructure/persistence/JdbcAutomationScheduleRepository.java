package com.ai.automation.infrastructure.persistence;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.vo.AutomationActionType;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.automation.domain.vo.ScheduleKind;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAutomationScheduleRepository implements AutomationScheduleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AutomationSchedule> rowMapper = (rs, rowNum) -> AutomationSchedule.restore(
            ScheduleId.of(rs.getString("id")),
            rs.getString("client_id"),
            rs.getString("name"),
            ScheduleKind.from(rs.getString("schedule_kind")),
            rs.getString("cron_expression"),
            rs.getString("timezone"),
            rs.getBoolean("enabled"),
            AutomationActionType.from(rs.getString("action_type")),
            rs.getString("workflow_template_id"),
            rs.getString("recipient_email"),
            rs.getString("brief"),
            rs.getTimestamp("next_run_at").toInstant(),
            rs.getTimestamp("last_run_at") == null ? null : rs.getTimestamp("last_run_at").toInstant(),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());

    public JdbcAutomationScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public AutomationSchedule save(AutomationSchedule schedule) {
        jdbcTemplate.update(
                """
                MERGE INTO automation_schedules (
                    id, client_id, name, schedule_kind, cron_expression, timezone, enabled, action_type,
                    workflow_template_id, recipient_email, brief, next_run_at, last_run_at,
                    created_at, updated_at
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                schedule.getId().value(),
                schedule.getClientId(),
                schedule.getName(),
                schedule.getScheduleKind().value(),
                schedule.getCronExpression(),
                schedule.getTimezone(),
                schedule.isEnabled(),
                schedule.getActionType().value(),
                schedule.getWorkflowTemplateId(),
                schedule.getRecipientEmail(),
                schedule.getBrief(),
                Timestamp.from(schedule.getNextRunAt()),
                schedule.getLastRunAt() == null ? null : Timestamp.from(schedule.getLastRunAt()),
                Timestamp.from(schedule.getCreatedAt()),
                Timestamp.from(schedule.getUpdatedAt()));
        return schedule;
    }

    @Override
    public Optional<AutomationSchedule> findByIdAndClientId(ScheduleId id, String clientId) {
        List<AutomationSchedule> rows = jdbcTemplate.query(
                """
                SELECT * FROM automation_schedules WHERE id = ? AND client_id = ?
                """,
                rowMapper,
                id.value(),
                clientId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public List<AutomationSchedule> findAllByClientId(String clientId) {
        return jdbcTemplate.query(
                """
                SELECT * FROM automation_schedules
                WHERE client_id = ?
                ORDER BY created_at DESC
                """,
                rowMapper,
                clientId);
    }

    @Override
    public int countByClientId(String clientId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM automation_schedules WHERE client_id = ?",
                Integer.class,
                clientId);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional
    public void deleteByIdAndClientId(ScheduleId id, String clientId) {
        jdbcTemplate.update(
                "DELETE FROM automation_schedules WHERE id = ? AND client_id = ?",
                id.value(),
                clientId);
    }

    @Override
    public List<AutomationSchedule> findDue(Instant asOf, int limit) {
        return jdbcTemplate.query(
                """
                SELECT * FROM automation_schedules
                WHERE enabled = TRUE AND next_run_at <= ?
                ORDER BY next_run_at ASC
                LIMIT ?
                """,
                rowMapper,
                Timestamp.from(asOf),
                limit);
    }

    @Override
    @Transactional
    public boolean claim(ScheduleId id, Instant expectedNextRunAt, Instant provisionalNextRunAt) {
        int updated = jdbcTemplate.update(
                """
                UPDATE automation_schedules
                SET next_run_at = ?, updated_at = ?
                WHERE id = ? AND enabled = TRUE AND next_run_at = ?
                """,
                Timestamp.from(provisionalNextRunAt),
                Timestamp.from(Instant.now()),
                id.value(),
                Timestamp.from(expectedNextRunAt));
        return updated == 1;
    }
}
