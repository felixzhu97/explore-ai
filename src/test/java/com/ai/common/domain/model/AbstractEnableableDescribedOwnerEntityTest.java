package com.ai.common.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ai.base.domain.vo.AbstractUuidId;
import com.ai.common.domain.vo.OwnerKeys;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbstractEnableableDescribedOwnerEntityTest {

  static final class TestId extends AbstractUuidId {

    private TestId(String value) {
      super(value);
    }

    static TestId generate() {
      return new TestId(newUuidString());
    }
  }

  static final class TestEntity extends AbstractEnableableDescribedOwnerEntity<TestId> {

    TestEntity(boolean enabled) {
      super(
          TestId.generate(),
          "client-1",
          "name",
          "desc",
          enabled,
          Instant.now(),
          Instant.now());
    }
  }

  @Test
  @DisplayName("should disable entity when disable is called")
  void shouldDisableEntityWhenDisableIsCalled() {
    TestEntity entity = new TestEntity(true);
    entity.disable();
    assertFalse(entity.isEnabled());
  }

  @Test
  @DisplayName("should enable entity when enable is called")
  void shouldEnableEntityWhenEnableIsCalled() {
    TestEntity entity = new TestEntity(false);
    entity.enable();
    assertTrue(entity.isEnabled());
  }

  @Test
  @DisplayName("should match client partition when belongsToClient is called")
  void shouldMatchClientPartitionWhenBelongsToClientIsCalled() {
    TestEntity entity = new TestEntity(true);
    assertTrue(entity.belongsToClient("client-1"));
    assertFalse(entity.belongsToClient("other"));
  }
}
