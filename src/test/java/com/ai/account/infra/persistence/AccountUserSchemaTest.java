package com.ai.account.infra.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest(
    properties = {
      "spring.liquibase.enabled=true",
      "spring.datasource.url=jdbc:h2:mem:account-schema;DB_CLOSE_DELAY=-1"
    })
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("account_user schema")
class AccountUserSchemaTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("should reject a second account linked to the same browser")
  void shouldRejectASecondAccountLinkedToTheSameBrowser() {
    insert("google", "sub-a", "cid-shared");

    assertThatThrownBy(() -> insert("github", "sub-b", "cid-shared"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("should allow many accounts without a linked browser")
  void shouldAllowManyAccountsWithoutALinkedBrowser() {
    insert("google", "sub-c", null);

    assertThatCode(() -> insert("github", "sub-d", null)).doesNotThrowAnyException();
  }

  private void insert(String provider, String subject, String linkedClientId) {
    jdbcTemplate.update(
        "INSERT INTO account_user (id, provider, subject, linked_client_id) VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        provider,
        subject,
        linkedClientId);
  }
}
