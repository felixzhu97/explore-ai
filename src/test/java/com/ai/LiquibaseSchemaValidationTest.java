package com.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:liquibase-schema-validation;DB_CLOSE_DELAY=-1",
      "spring.liquibase.enabled=true",
      "spring.liquibase.change-log=classpath:db/changelog-master.xml",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
class LiquibaseSchemaValidationTest extends AbstractDataJpaTest {

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("should validate every entity mapping when schema comes from liquibase")
  void shouldValidateEveryEntityMappingWhenSchemaComesFromLiquibase() {
    assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(9);
  }
}
