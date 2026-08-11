package com.ai.plugin.infrastructure.persistence;

import com.ai.plugin.domain.model.PluginInstallation;
import com.ai.plugin.domain.repository.PluginInstallationRepository;
import com.ai.plugin.domain.vo.PluginHealthStatus;
import com.ai.plugin.domain.vo.PluginInstallationId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPluginInstallationRepository implements PluginInstallationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PluginInstallation> rowMapper = (rs, rowNum) -> PluginInstallation.restore(
            PluginInstallationId.of(rs.getString("id")),
            rs.getString("owner_key"),
            rs.getString("definition_id"),
            rs.getString("display_name"),
            rs.getString("endpoint"),
            rs.getString("auth_token"),
            rs.getBoolean("enabled"),
            PluginHealthStatus.from(rs.getString("health_status")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));

    public JdbcPluginInstallationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PluginInstallation save(PluginInstallation installation) {
        jdbcTemplate.update("""
                MERGE INTO plugin_installations (
                  id, owner_key, definition_id, display_name, endpoint, auth_token,
                  enabled, health_status, created_at, updated_at
                )
                KEY (id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                installation.getId().value(),
                installation.getOwnerKey(),
                installation.getDefinitionId(),
                installation.getDisplayName(),
                installation.getEndpoint(),
                installation.getAuthToken(),
                installation.isEnabled(),
                installation.getHealthStatus().name(),
                Timestamp.from(installation.getCreatedAt()),
                Timestamp.from(installation.getUpdatedAt()));
        return installation;
    }

    @Override
    public Optional<PluginInstallation> findByIdAndOwnerKey(String id, String ownerKey) {
        List<PluginInstallation> rows = jdbcTemplate.query("""
                SELECT id, owner_key, definition_id, display_name, endpoint, auth_token,
                       enabled, health_status, created_at, updated_at
                FROM plugin_installations
                WHERE id = ? AND owner_key = ?
                """, rowMapper, id, ownerKey);
        return rows.stream().findFirst();
    }

    @Override
    public List<PluginInstallation> findAllByOwnerKey(String ownerKey) {
        return jdbcTemplate.query("""
                SELECT id, owner_key, definition_id, display_name, endpoint, auth_token,
                       enabled, health_status, created_at, updated_at
                FROM plugin_installations
                WHERE owner_key = ?
                ORDER BY created_at ASC
                """, rowMapper, ownerKey);
    }

    @Override
    public Optional<PluginInstallation> findByOwnerKeyAndDefinitionId(String ownerKey, String definitionId) {
        List<PluginInstallation> rows = jdbcTemplate.query("""
                SELECT id, owner_key, definition_id, display_name, endpoint, auth_token,
                       enabled, health_status, created_at, updated_at
                FROM plugin_installations
                WHERE owner_key = ? AND definition_id = ?
                """, rowMapper, ownerKey, definitionId);
        return rows.stream().findFirst();
    }

    @Override
    public void deleteByIdAndOwnerKey(String id, String ownerKey) {
        jdbcTemplate.update(
                "DELETE FROM plugin_installations WHERE id = ? AND owner_key = ?",
                id,
                ownerKey);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}
