package com.ai.metrics.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ai.metrics.domain.repository.MetricsQueryRepository.TimePoint;
import com.ai.metrics.domain.vo.AiDomain;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * Runs every metrics query against the Liquibase-migrated schema, so table or column renames fail
 * here instead of at runtime.
 */
@JdbcTest(
    properties = {
      "spring.liquibase.enabled=true",
      "spring.datasource.url=jdbc:h2:mem:metrics-schema;DB_CLOSE_DELAY=-1"
    })
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcMetricsQueryRepository.class)
@Sql("classpath:org/springframework/ai/chat/memory/repository/jdbc/schema-h2.sql")
class JdbcMetricsQueryRepositorySchemaTest {

  private static final Instant TO = Instant.now().plus(1, ChronoUnit.DAYS);
  private static final Instant FROM = TO.minus(30, ChronoUnit.DAYS);

  @Autowired private JdbcMetricsQueryRepository repository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("should run every metrics query when schema comes from liquibase")
  void shouldRunEveryMetricsQueryWhenSchemaComesFromLiquibase() {
    Optional<AiDomain> chat = Optional.of(AiDomain.CHAT);

    assertThatCode(
            () -> {
              repository.countInvocations(chat, FROM, TO);
              repository.countErrors(chat, FROM, TO);
              repository.calculateLatencyPercentiles(chat, FROM, TO);
              repository.sumTokens(chat, FROM, TO);
              repository.countByDomain(FROM, TO);
              repository.countByModel(chat, FROM, TO);
              repository.countByAgentType(FROM, TO);
              repository.listTopTools(chat, FROM, TO, 5);
              repository.countDailyRequests(chat, FROM, TO);
              repository.countDailyErrors(chat, FROM, TO);
              repository.calculateDailyLatencyP95(chat, FROM, TO);
              repository.countDailySessionsCreated(FROM, TO);
              repository.countDailyMessagesCreated(FROM, TO);
              repository.countDailyDocumentsUploaded(FROM, TO);
              repository.getChatInventory(FROM);
              repository.getRagInventory();
            })
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should count uploaded documents when rag document rows exist")
  void shouldCountUploadedDocumentsWhenRagDocumentRowsExist() {
    jdbcTemplate.update(
        "INSERT INTO rag_document (id, title, status) VALUES (?, 'guide', 'READY')",
        UUID.randomUUID());

    List<TimePoint> points = repository.countDailyDocumentsUploaded(FROM, TO);

    assertThat(points).singleElement().extracting(TimePoint::value).isEqualTo(1L);
  }
}
