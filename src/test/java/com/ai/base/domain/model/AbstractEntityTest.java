package com.ai.base.domain.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ai.base.domain.vo.AbstractUuidId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbstractEntityTest {

  static final class TestEntityId extends AbstractUuidId {

    private TestEntityId(String value) {
      super(value);
    }

    static TestEntityId generate() {
      return new TestEntityId(newUuidString());
    }
  }

  static final class TestEntity extends AbstractEntity<TestEntityId> {

    TestEntity(TestEntityId id, Instant createdAt, Instant updatedAt) {
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
    TestEntity entity = new TestEntity(TestEntityId.generate(), created, created);
    Thread.sleep(2);
    entity.bump();
    assertTrue(entity.getUpdatedAt().isAfter(created));
  }
}
