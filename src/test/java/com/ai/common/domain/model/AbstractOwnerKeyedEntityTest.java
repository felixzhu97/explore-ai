package com.ai.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractOwnerKeyedEntity")
class AbstractOwnerKeyedEntityTest {

  static final class TestId extends AbstractUuidId {

    private TestId(String value) {
      super(value);
    }

    static TestId generate() {
      return new TestId(generateUuidString());
    }
  }

  static final class TestEntity extends AbstractOwnerKeyedEntity<TestId> {

    TestEntity(String ownerKey) {
      super(TestId.generate(), ownerKey, Instant.now(), Instant.now());
    }
  }

  @Test
  @DisplayName("should match the owner when the key value has surrounding spaces")
  void shouldMatchTheOwnerWhenTheKeyValueHasSurroundingSpaces() {
    TestEntity entity = new TestEntity("c:guest-1");

    assertThat(entity.belongsTo(" c:guest-1 ")).isTrue();
    assertThat(entity.belongsTo("c:guest-2")).isFalse();
  }

  @Test
  @DisplayName("should not match the owner when the key value is invalid")
  void shouldNotMatchTheOwnerWhenTheKeyValueIsInvalid() {
    TestEntity entity = new TestEntity("c:guest-1");

    assertThat(entity.belongsTo("guest-1")).isFalse();
    assertThat(entity.belongsTo((String) null)).isFalse();
  }

  @Test
  @DisplayName("should move a guest row to the signed-in account")
  void shouldMoveAGuestRowToTheSignedInAccount() {
    TestEntity entity = new TestEntity("c:guest-1");

    entity.transferTo(OwnerKey.forAccount("user-1"));

    assertThat(entity.getOwnerKeyValue()).isEqualTo("u:user-1");
  }

  @Test
  @DisplayName("should refuse to move an account row to another owner")
  void shouldRefuseToMoveAnAccountRowToAnotherOwner() {
    TestEntity entity = new TestEntity("u:user-1");

    assertThatThrownBy(() -> entity.transferTo(OwnerKey.forAccount("user-2")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(entity.getOwnerKeyValue()).isEqualTo("u:user-1");
  }
}
