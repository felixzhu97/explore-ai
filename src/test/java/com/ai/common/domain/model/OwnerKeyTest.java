package com.ai.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OwnerKey")
class OwnerKeyTest {

  @Test
  void shouldBuildClientKeyWhenForClient() {
    OwnerKey key = OwnerKey.createClientKey("  abc-123  ");

    assertThat(key.getValue()).isEqualTo("c:abc-123");
    assertThat(key.isClient()).isTrue();
    assertThat(key.isAccount()).isFalse();
  }

  @Test
  void shouldBuildAccountKeyWhenForAccount() {
    OwnerKey key = OwnerKey.createAccountKey("user-9");

    assertThat(key.getValue()).isEqualTo("u:user-9");
    assertThat(key.isAccount()).isTrue();
    assertThat(key.isClient()).isFalse();
  }

  @Test
  void shouldParseRawValueWhenPrefixed() {
    assertThat(OwnerKey.parseKey("c:guest").getValue()).isEqualTo("c:guest");
    assertThat(OwnerKey.parseKey("u:acct").getValue()).isEqualTo("u:acct");
  }

  @Test
  void shouldRejectBlankWhenCreating() {
    assertThatThrownBy(() -> OwnerKey.createClientKey(" "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerKey.createAccountKey(""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerKey.parseKey("x:nope"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should allow merging a guest key into an account key")
  void shouldAllowMergingAGuestKeyIntoAnAccountKey() {
    OwnerKey.createClientKey("guest-1").requireMergeableInto(OwnerKey.createAccountKey("user-1"));
  }

  @Test
  @DisplayName("should reject merging when source is not a guest or target is not an account")
  void shouldRejectMergingWhenSourceIsNotAGuestOrTargetIsNotAnAccount() {
    OwnerKey guest = OwnerKey.createClientKey("guest-1");
    OwnerKey account = OwnerKey.createAccountKey("user-1");

    assertThatThrownBy(() -> account.requireMergeableInto(OwnerKey.createAccountKey("user-2")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> guest.requireMergeableInto(OwnerKey.createClientKey("guest-2")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OwnerKey.UNOWNED.requireMergeableInto(account))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
