package com.ai.account.infra.persistence;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Documentation. */
@Repository
public class JdbcAccountUserRepository implements AccountUserRepository {

  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<AccountUser> rowMapper =
      (rs, rowNum) ->
          AccountUser.restore(
              rs.getString("id"),
              rs.getString("provider"),
              rs.getString("subject"),
              rs.getString("email"),
              rs.getString("linked_client_id"),
              rs.getTimestamp("created_at").toInstant(),
              rs.getTimestamp("updated_at").toInstant());

  /** Documentation. */
  public JdbcAccountUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public AccountUser save(AccountUser user) {
    jdbcTemplate.update(
        """
                MERGE INTO account_users (
                    id, provider, subject, email, linked_client_id, created_at, updated_at
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
        user.getId(),
        user.getProvider(),
        user.getSubject(),
        user.getEmail(),
        user.getLinkedClientId(),
        Timestamp.from(user.getCreatedAt()),
        Timestamp.from(user.getUpdatedAt()));
    return user;
  }

  @Override
  public Optional<AccountUser> findByProviderAndSubject(String provider, String subject) {
    List<AccountUser> rows =
        jdbcTemplate.query(
            """
                SELECT * FROM account_users WHERE provider = ? AND subject = ?
                """,
            rowMapper,
            provider,
            subject);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  @Override
  public Optional<AccountUser> findByLinkedClientId(String linkedClientId) {
    if (linkedClientId == null || linkedClientId.isBlank()) {
      return Optional.empty();
    }
    List<AccountUser> rows =
        jdbcTemplate.query(
            """
                SELECT * FROM account_users WHERE linked_client_id = ?
                """,
            rowMapper,
            linkedClientId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }
}
