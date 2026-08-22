package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.vo.EmailDeliveryStatus;
import com.ai.automation.domain.vo.RunId;
import com.ai.automation.domain.vo.RunStatus;
import com.ai.automation.domain.vo.ScheduleId;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Documentation. */
@Repository
public class JdbcAutomationRunRepository implements AutomationRunRepository {

  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<AutomationRun> rowMapper =
      (rs, rowNum) ->
          AutomationRun.restore(
              RunId.of(rs.getString("id")),
              ScheduleId.of(rs.getString("schedule_id")),
              rs.getString("owner_key"),
              rs.getTimestamp("started_at").toInstant(),
              rs.getTimestamp("finished_at") == null
                  ? null
                  : rs.getTimestamp("finished_at").toInstant(),
              RunStatus.from(rs.getString("status")),
              rs.getString("error_message"),
              rs.getString("result_excerpt"),
              EmailDeliveryStatus.from(rs.getString("email_status")));

  /** Documentation. */
  public JdbcAutomationRunRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public AutomationRun save(AutomationRun run) {
    jdbcTemplate.update(
        """
                MERGE INTO automation_runs (
                    id, schedule_id, owner_key, started_at, finished_at, status,
                    error_message, result_excerpt, email_status
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
        run.getId().value(),
        run.getScheduleId().value(),
        run.getClientId(),
        Timestamp.from(run.getStartedAt()),
        run.getFinishedAt() == null ? null : Timestamp.from(run.getFinishedAt()),
        run.getStatus().value(),
        run.getErrorMessage(),
        run.getResultExcerpt(),
        run.getEmailStatus().value());
    return run;
  }

  @Override
  public List<AutomationRun> findByScheduleIdAndClientId(
      ScheduleId scheduleId, String clientId, int limit) {
    return jdbcTemplate.query(
        """
                SELECT * FROM automation_runs
                WHERE schedule_id = ? AND owner_key = ?
                ORDER BY started_at DESC
                LIMIT ?
                """,
        rowMapper,
        scheduleId.value(),
        clientId,
        limit);
  }
}
