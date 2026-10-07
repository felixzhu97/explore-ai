package com.ai.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountUser")
class AccountUserTest {

  private static final ClientId CLIENT = ClientId.parse("11111111-1111-1111-1111-111111111111");
  private static final ClientId OTHER_CLIENT =
      ClientId.parse("22222222-2222-2222-2222-222222222222");

  @Test
  @DisplayName("should create an unlinked account with a normalized identity")
  void shouldCreateAnUnlinkedAccountWithANormalizedIdentity() {
    AccountUser user =
        AccountUser.create(
            ExternalIdentity.of("Google", " sub-1 "),
            ContactEmail.ofNullable(" User@Example.com "),
            null);

    assertThat(user.getId().value()).isNotBlank();
    assertThat(user.identity()).isEqualTo(ExternalIdentity.of("google", "sub-1"));
    assertThat(user.getEmail()).isEqualTo(new ContactEmail("User@Example.com"));
    assertThat(user.getLinkedClientId()).isNull();
    assertThat(user.guestOwnerKey()).isEmpty();
  }

  @Test
  @DisplayName("should link the browser and refresh the profile when signing in on a browser")
  void shouldLinkTheBrowserAndRefreshTheProfileWhenSigningInOnABrowser() {
    AccountUser user = account(new ContactEmail("a@b.com"), null);

    user.linkBrowser(CLIENT, new ContactEmail("c@d.com"), "octocat");

    assertThat(user.getLinkedClientId()).isEqualTo(CLIENT);
    assertThat(user.getEmail()).isEqualTo(new ContactEmail("c@d.com"));
    assertThat(user.getDisplayName()).isEqualTo("octocat");
    assertThat(user.guestOwnerKey()).contains(OwnerKey.forClient(CLIENT.value()));
  }

  @Test
  @DisplayName("should keep the stored profile when the provider sends none")
  void shouldKeepTheStoredProfileWhenTheProviderSendsNone() {
    AccountUser user = account(new ContactEmail("a@b.com"), "octocat");

    user.linkBrowser(OTHER_CLIENT, null, " ");
    user.recordSignIn(null, null);

    assertThat(user.getEmail()).isEqualTo(new ContactEmail("a@b.com"));
    assertThat(user.getDisplayName()).isEqualTo("octocat");
  }

  @Test
  @DisplayName("should update the email when an IAM sign in sends a new one")
  void shouldUpdateTheEmailWhenAnIamSignInSendsANewOne() {
    AccountUser user = account(new ContactEmail("old@example.com"), null);

    user.recordSignIn(new ContactEmail("new@example.com"), null);

    assertThat(user.getEmail()).isEqualTo(new ContactEmail("new@example.com"));
  }

  @Test
  @DisplayName("should clear the guest partition when the browser is unlinked")
  void shouldClearTheGuestPartitionWhenTheBrowserIsUnlinked() {
    AccountUser user = account(null, null);
    user.linkBrowser(CLIENT, null, null);

    user.unlinkBrowser();

    assertThat(user.getLinkedClientId()).isNull();
    assertThat(user.guestOwnerKey()).isEmpty();
  }

  @Test
  @DisplayName("should own data under its account id")
  void shouldOwnDataUnderItsAccountId() {
    AccountUser user = account(null, null);

    assertThat(user.ownerKey()).isEqualTo(OwnerKey.forAccount(user.getId().value()));
  }

  @Test
  @DisplayName("should label the account with the display name before the email")
  void shouldLabelTheAccountWithTheDisplayNameBeforeTheEmail() {
    assertThat(account(new ContactEmail("a@b.com"), "octocat").displayLabel()).contains("octocat");
    assertThat(account(new ContactEmail("a@b.com"), null).displayLabel()).contains("a@b.com");
    assertThat(account(null, null).displayLabel()).isEmpty();
  }

  @Test
  @DisplayName("should cap the display name length when the provider sends a long one")
  void shouldCapTheDisplayNameLengthWhenTheProviderSendsALongOne() {
    AccountUser user = account(null, "x".repeat(300));

    assertThat(user.getDisplayName()).hasSize(AccountUser.MAX_DISPLAY_NAME_LENGTH);
  }

  @Test
  @DisplayName("should not print the email or display name when formatted")
  void shouldNotPrintTheEmailOrDisplayNameWhenFormatted() {
    AccountUser user = account(new ContactEmail("secret@example.com"), "octocat");

    assertThat(user.toString()).doesNotContain("secret@example.com").doesNotContain("octocat");
  }

  private static AccountUser account(ContactEmail email, String displayName) {
    return AccountUser.create(ExternalIdentity.of("github", "42"), email, displayName);
  }
}
