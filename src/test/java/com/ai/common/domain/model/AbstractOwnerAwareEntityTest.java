package com.ai.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.skill.domain.model.SkillId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractOwnerAwareEntity")
class AbstractOwnerAwareEntityTest {

  static final class TestEntity extends AbstractOwnerAwareEntity<SkillId> {

    TestEntity(String ownerKey) {
      super(SkillId.generate(), OwnerKey.parse(ownerKey));
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
}
