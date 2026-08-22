package com.ai.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountUser")
class AccountUserTest {

  @Test
  void shouldCreateUserWhenValidInputs() {
    AccountUser user = AccountUser.create("Google", "sub-1", " User@Example.com ", "cid-1");

    assertThat(user.getId().value()).isNotBlank();
    assertThat(user.getProvider()).isEqualTo("google");
    assertThat(user.getSubject()).isEqualTo("sub-1");
    assertThat(user.getEmail()).isEqualTo("User@Example.com");
    assertThat(user.getLinkedClientId()).isEqualTo("cid-1");
  }

  @Test
  void shouldUpdateLinkWhenLinkSessionCalled() {
    AccountUser user = AccountUser.create("google", "sub-1", "a@b.com", "cid-1");

    user.linkSession("c@d.com", "cid-2");

    assertThat(user.getEmail()).isEqualTo("c@d.com");
    assertThat(user.getLinkedClientId()).isEqualTo("cid-2");
  }

  @Test
  void shouldRejectBlankProviderWhenCreate() {
    assertThatThrownBy(() -> AccountUser.create(" ", "sub", "a@b.com", "cid"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
