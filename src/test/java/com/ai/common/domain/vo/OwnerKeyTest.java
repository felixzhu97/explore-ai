package com.ai.common.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OwnerKey")
class OwnerKeyTest {

  @Test
  void shouldBuildClientKeyWhenForClient() {
    OwnerKey key = OwnerKey.forClient("  abc-123  ");

    assertThat(key.value()).isEqualTo("c:abc-123");
    assertThat(key.isClient()).isTrue();
    assertThat(key.isAccount()).isFalse();
  }

  @Test
  void shouldBuildAccountKeyWhenForAccount() {
    OwnerKey key = OwnerKey.forAccount("user-9");

    assertThat(key.value()).isEqualTo("u:user-9");
    assertThat(key.isAccount()).isTrue();
    assertThat(key.isClient()).isFalse();
  }

  @Test
  void shouldParseRawValueWhenPrefixed() {
    assertThat(OwnerKey.parse("c:guest").value()).isEqualTo("c:guest");
    assertThat(OwnerKey.parse("u:acct").value()).isEqualTo("u:acct");
  }

  @Test
  void shouldRejectBlankWhenCreating() {
    assertThatThrownBy(() -> OwnerKey.forClient(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerKey.forAccount("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerKey.parse("x:nope")).isInstanceOf(IllegalArgumentException.class);
  }
}
