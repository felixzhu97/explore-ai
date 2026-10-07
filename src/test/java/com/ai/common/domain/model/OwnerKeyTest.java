package com.ai.common.domain.model;

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

  @Test
  @DisplayName("should allow merging a guest key into an account key")
  void shouldAllowMergingAGuestKeyIntoAnAccountKey() {
    OwnerKey.forClient("guest-1").requireMergeableInto(OwnerKey.forAccount("user-1"));
  }

  @Test
  @DisplayName("should reject merging when source is not a guest or target is not an account")
  void shouldRejectMergingWhenSourceIsNotAGuestOrTargetIsNotAnAccount() {
    OwnerKey guest = OwnerKey.forClient("guest-1");
    OwnerKey account = OwnerKey.forAccount("user-1");

    assertThatThrownBy(() -> account.requireMergeableInto(OwnerKey.forAccount("user-2")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> guest.requireMergeableInto(OwnerKey.forClient("guest-2")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerKey.UNOWNED.requireMergeableInto(account))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
