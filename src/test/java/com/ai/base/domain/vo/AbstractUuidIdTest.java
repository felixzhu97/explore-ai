package com.ai.base.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbstractUuidIdTest {

  static final class TestId extends AbstractUuidId {

    private TestId(String value) {
      super(value);
    }

    static TestId of(String value) {
      return new TestId(value);
    }

    static TestId generate() {
      return new TestId(newUuidString());
    }
  }

  @Test
  @DisplayName("should accept valid UUID string when constructing id")
  void shouldAcceptValidUuidWhenConstructingId() {
    String uuid = UUID.randomUUID().toString();
    TestId id = TestId.of(uuid);
    assertEquals(uuid, id.value());
  }

  @Test
  @DisplayName("should reject blank id value when constructing id")
  void shouldRejectBlankIdValueWhenConstructingId() {
    assertThrows(IllegalArgumentException.class, () -> TestId.of(" "));
  }

  @Test
  @DisplayName("should reject malformed uuid when constructing id")
  void shouldRejectMalformedUuidWhenConstructingId() {
    assertThrows(IllegalArgumentException.class, () -> TestId.of("not-a-uuid"));
  }
}
