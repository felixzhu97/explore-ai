package com.ai.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.skill.domain.model.SkillId;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AbstractEnableableDescribedOwnerEntity")
class AbstractEnableableDescribedOwnerEntityTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  static final class TestEntity extends AbstractEnableableDescribedOwnerEntity<SkillId> {

    TestEntity(String description) {
      super(
          SkillId.generate(),
          "c:client-1",
          "name",
          description,
          true,
          Instant.now(),
          Instant.now());
    }

    void describe(String description) {
      updateDescription(description);
    }
  }

  @BeforeAll
  static void createValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("should accept entity with empty description when description is omitted or blank")
  void shouldAcceptEntityWithEmptyDescriptionWhenDescriptionIsOmittedOrBlank(String description) {
    TestEntity entity = new TestEntity(description);

    assertThat(entity.getDescription()).isEmpty();
    assertThat(validator.validate(entity)).isEmpty();
  }

  @Test
  @DisplayName("should trim description when description has surrounding spaces")
  void shouldTrimDescriptionWhenDescriptionHasSurroundingSpaces() {
    TestEntity entity = new TestEntity("  Summarizes sources  ");

    assertThat(entity.getDescription()).isEqualTo("Summarizes sources");
  }

  @Test
  @DisplayName("should clear description when update is blank")
  void shouldClearDescriptionWhenUpdateIsBlank() {
    TestEntity entity = new TestEntity("Summarizes sources");

    entity.describe(" ");

    assertThat(entity.getDescription()).isEmpty();
    assertThat(validator.validate(entity)).isEmpty();
  }
}
