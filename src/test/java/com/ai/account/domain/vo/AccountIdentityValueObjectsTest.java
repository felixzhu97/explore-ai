package com.ai.account.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Account identity value objects")
class AccountIdentityValueObjectsTest {

  @Test
  @DisplayName("should normalize the identity the same way for saving and lookup")
  void shouldNormalizeTheIdentityTheSameWayForSavingAndLookup() {
    assertThat(ExternalIdentity.of(" GitHub ", " 42 "))
        .isEqualTo(ExternalIdentity.of("github", "42"));
  }

  @Test
  @DisplayName("should use the explore iam provider when the identity comes from an IAM token")
  void shouldUseTheExploreIamProviderWhenTheIdentityComesFromAnIamToken() {
    assertThat(ExternalIdentity.iam("sub").provider()).isEqualTo("explore-iam");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "unknown", "UNKNOWN"})
  @DisplayName("should reject the provider when it is missing or unknown")
  void shouldRejectTheProviderWhenItIsMissingOrUnknown(String provider) {
    assertThatThrownBy(() -> ExternalIdentity.of(provider, "sub"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should reject the subject when it is blank")
  void shouldRejectTheSubjectWhenItIsBlank() {
    assertThatThrownBy(() -> ExternalIdentity.iam(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should keep a trimmed email when the value is an email address")
  void shouldKeepATrimmedEmailWhenTheValueIsAnEmailAddress() {
    assertThat(ContactEmail.ofNullable(" a@b.com ")).isEqualTo(new ContactEmail("a@b.com"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "octocat", "two words@x.com", "a@b@c"})
  @DisplayName("should drop the email when the value is not an email address")
  void shouldDropTheEmailWhenTheValueIsNotAnEmailAddress(String raw) {
    assertThat(ContactEmail.ofNullable(raw)).isNull();
  }

  @Test
  @DisplayName("should reject a login handle when constructing an email")
  void shouldRejectALoginHandleWhenConstructingAnEmail() {
    assertThatThrownBy(() -> new ContactEmail("octocat"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should not print the address when the email is formatted")
  void shouldNotPrintTheAddressWhenTheEmailIsFormatted() {
    assertThat(new ContactEmail("secret@example.com").toString()).doesNotContain("secret");
  }

  @Test
  @DisplayName("should accept a client id when it is a UUID")
  void shouldAcceptAClientIdWhenItIsAUuid() {
    assertThat(ClientId.parse(" 55555555-5555-5555-5555-555555555555 ").value())
        .isEqualTo("55555555-5555-5555-5555-555555555555");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"cid-1", "ip:127.0.0.1", "55555555-5555-5555-5555-55555555555"})
  @DisplayName("should reject a client id when it is not a UUID")
  void shouldRejectAClientIdWhenItIsNotAUuid(String raw) {
    assertThat(ClientId.isValid(raw)).isFalse();
    assertThatThrownBy(() -> ClientId.parse(raw)).isInstanceOf(IllegalArgumentException.class);
  }
}
