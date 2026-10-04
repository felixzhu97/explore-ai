package com.ai.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.vo.AbstractUuidId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractEnableableNamedOwnerEntity")
class AbstractEnableableNamedOwnerEntityTest {

  static final class TestId extends AbstractUuidId {

    private TestId(String value) {
      super(value);
    }

    static TestId generate() {
      return new TestId(newUuidString());
    }
  }

  static final class TestEntity extends AbstractEnableableNamedOwnerEntity<TestId> {

    TestEntity(boolean enabled) {
      super(TestId.generate(), "c:client-1", "name", enabled, Instant.now(), Instant.now());
    }
  }

  @Test
  @DisplayName("should disable entity when disable is called")
  void shouldDisableEntityWhenDisableIsCalled() {
    TestEntity entity = new TestEntity(true);
    entity.disable();
    assertThat(entity.isEnabled()).isFalse();
  }

  @Test
  @DisplayName("should enable entity when enable is called")
  void shouldEnableEntityWhenEnableIsCalled() {
    TestEntity entity = new TestEntity(false);
    entity.enable();
    assertThat(entity.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("should match owner key when belongsTo is called with its value")
  void shouldMatchOwnerKeyWhenBelongsToIsCalledWithItsValue() {
    TestEntity entity = new TestEntity(true);
    assertThat(entity.belongsTo("c:client-1")).isTrue();
    assertThat(entity.belongsTo("c:other")).isFalse();
  }
}
