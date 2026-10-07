package com.ai.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Account")
class AccountTest {

  private static final ClientId CLIENT = ClientId.parseId("11111111-1111-1111-1111-111111111111");
  private static final ClientId OTHER_CLIENT =
      ClientId.parseId("22222222-2222-2222-2222-222222222222");

  @Test
  @DisplayName("should create an unlinked account with a normalized identity")
  void shouldCreateAnUnlinkedAccountWithANormalizedIdentity() {
    Account account =
        Account.createAccount(
            ExternalIdentity.createIdentity("Google", " sub-1 "),
            ContactEmail.parseOptionalEmail(" User@Example.com "),
            null);

    assertThat(account.getId().getValue()).isNotNull();
    assertThat(account.getIdentity()).isEqualTo(ExternalIdentity.createIdentity("google", "sub-1"));
    assertThat(account.getEmail()).isEqualTo(new ContactEmail("User@Example.com"));
    assertThat(account.getLinkedClientId()).isNull();
    assertThat(account.findGuestOwnerKey()).isEmpty();
  }

  @Test
  @DisplayName("should link the browser and refresh the profile when signing in on a browser")
  void shouldLinkTheBrowserAndRefreshTheProfileWhenSigningInOnABrowser() {
    Account account = account(new ContactEmail("a@b.com"), null);

    account.linkBrowser(CLIENT, new ContactEmail("c@d.com"), "octocat");

    assertThat(account.getLinkedClientId()).isEqualTo(CLIENT);
    assertThat(account.getEmail()).isEqualTo(new ContactEmail("c@d.com"));
    assertThat(account.getDisplayName()).isEqualTo("octocat");
    assertThat(account.findGuestOwnerKey()).contains(OwnerKey.createClientKey(CLIENT.value()));
  }

  @Test
  @DisplayName("should keep the stored profile when the provider sends none")
  void shouldKeepTheStoredProfileWhenTheProviderSendsNone() {
    Account account = account(new ContactEmail("a@b.com"), "octocat");

    account.linkBrowser(OTHER_CLIENT, null, " ");
    account.recordSignIn(null, null);

    assertThat(account.getEmail()).isEqualTo(new ContactEmail("a@b.com"));
    assertThat(account.getDisplayName()).isEqualTo("octocat");
  }

  @Test
  @DisplayName("should update the email when an IAM sign in sends a new one")
  void shouldUpdateTheEmailWhenAnIamSignInSendsANewOne() {
    Account account = account(new ContactEmail("old@example.com"), null);

    account.recordSignIn(new ContactEmail("new@example.com"), null);

    assertThat(account.getEmail()).isEqualTo(new ContactEmail("new@example.com"));
  }

  @Test
  @DisplayName("should clear the guest partition when the browser is unlinked")
  void shouldClearTheGuestPartitionWhenTheBrowserIsUnlinked() {
    Account account = account(null, null);
    account.linkBrowser(CLIENT, null, null);

    account.unlinkBrowser();

    assertThat(account.getLinkedClientId()).isNull();
    assertThat(account.findGuestOwnerKey()).isEmpty();
  }

  @Test
  @DisplayName("should own data under its account id")
  void shouldOwnDataUnderItsAccountId() {
    Account account = account(null, null);

    assertThat(account.createOwnerKey())
        .isEqualTo(OwnerKey.createAccountKey(account.getId().toString()));
  }

  @Test
  @DisplayName("should label the account with the display name before the email")
  void shouldLabelTheAccountWithTheDisplayNameBeforeTheEmail() {
    assertThat(account(new ContactEmail("a@b.com"), "octocat").findDisplayLabel())
        .contains("octocat");
    assertThat(account(new ContactEmail("a@b.com"), null).findDisplayLabel()).contains("a@b.com");
    assertThat(account(null, null).findDisplayLabel()).isEmpty();
  }

  @Test
  @DisplayName("should cap the display name length when the provider sends a long one")
  void shouldCapTheDisplayNameLengthWhenTheProviderSendsALongOne() {
    Account account = account(null, "x".repeat(300));

    assertThat(account.getDisplayName()).hasSize(Account.MAX_DISPLAY_NAME_LENGTH);
  }

  @Test
  @DisplayName("should not print the email or display name when formatted")
  void shouldNotPrintTheEmailOrDisplayNameWhenFormatted() {
    Account account = account(new ContactEmail("secret@example.com"), "octocat");

    assertThat(account.toString()).doesNotContain("secret@example.com").doesNotContain("octocat");
  }

  private static Account account(ContactEmail email, String displayName) {
    return Account.createAccount(
        ExternalIdentity.createIdentity("github", "42"), email, displayName);
  }
}
