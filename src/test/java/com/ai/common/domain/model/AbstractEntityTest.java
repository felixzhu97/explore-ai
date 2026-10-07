package com.ai.common.domain.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ai.skill.domain.model.SkillId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbstractEntityTest {

  static final class TestEntity extends AbstractEntity<SkillId> {

    TestEntity(SkillId id, Instant createdAt, Instant updatedAt) {
      super(id, createdAt, updatedAt);
    }

    void bump() {
      touchUpdatedAt();
    }
  }

  @Test
  @DisplayName("should advance updatedAt when touchUpdatedAt is called")
  void shouldAdvanceUpdatedAtWhenTouchUpdatedAtIsCalled() throws InterruptedException {
    Instant created = Instant.now();
    TestEntity entity = new TestEntity(SkillId.generate(), created, created);
    Thread.sleep(2);
    entity.bump();
    assertTrue(entity.getUpdatedAt().isAfter(created));
  }
}
