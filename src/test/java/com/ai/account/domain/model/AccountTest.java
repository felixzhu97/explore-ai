package com.ai.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
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
    assertThat(account.findGuestOwnerKey()).contains(OwnerKey.createClientKey(CLIENT.getValue()));
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

  @Test
  @DisplayName("should create an active provisioned account with the IAM identity")
  void shouldCreateAnActiveProvisionedAccountWithTheIamIdentity() {
    Account account = provisioned();

    assertThat(account.getIdentity()).isEqualTo(ExternalIdentity.createIamIdentity("iam-user-1"));
    assertThat(account.getUserName()).isEqualTo("alice");
    assertThat(account.getDisplayName()).isEqualTo("Alice");
    assertThat(account.isActive()).isTrue();
  }

  @Test
  @DisplayName("should reject a provisioned account without a user name")
  void shouldRejectAProvisionedAccountWithoutAUserName() {
    assertThatThrownBy(
            () ->
                Account.createProvisionedAccount(
                    ExternalIdentity.createIamIdentity("iam-user-1"), " ", null, null))
        .isInstanceOfSatisfying(
            DomainException.class, e -> assertThat(e.getCode()).isEqualTo("USER_NAME_REQUIRED"));
  }

  @Test
  @DisplayName("should clear values left out when the profile is replaced")
  void shouldClearValuesLeftOutWhenTheProfileIsReplaced() {
    Account account = provisioned();

    account.replaceProfile(" alice2 ", null, null);

    assertThat(account.getUserName()).isEqualTo("alice2");
    assertThat(account.getEmail()).isNull();
    assertThat(account.getDisplayName()).isNull();
  }

  @Test
  @DisplayName("should refuse sign-in and unlink the browser when deactivated")
  void shouldRefuseSignInAndUnlinkTheBrowserWhenDeactivated() {
    Account account = provisioned();
    account.linkBrowser(CLIENT, null, null);

    account.deactivateAccount();

    assertThat(account.isActive()).isFalse();
    assertThat(account.getLinkedClientId()).isNull();
    assertThatThrownBy(() -> account.recordSignIn(null, null))
        .isInstanceOfSatisfying(
            DomainException.class,
            e -> {
              assertThat(e.getKind()).isEqualTo(DomainException.Kind.FORBIDDEN);
              assertThat(e.getCode()).isEqualTo("ACCOUNT_INACTIVE");
            });
    assertThatThrownBy(() -> account.linkBrowser(CLIENT, null, null))
        .isInstanceOf(DomainException.class);
  }

  @Test
  @DisplayName("should allow sign-in again when reactivated")
  void shouldAllowSignInAgainWhenReactivated() {
    Account account = provisioned();
    account.deactivateAccount();

    account.reactivateAccount();
    account.recordSignIn(new ContactEmail("alice@example.com"), null);

    assertThat(account.isActive()).isTrue();
    assertThat(account.getEmail()).isEqualTo(new ContactEmail("alice@example.com"));
  }

  private static Account provisioned() {
    return Account.createProvisionedAccount(
        ExternalIdentity.createIamIdentity("iam-user-1"), "alice", null, "Alice");
  }

  private static Account account(ContactEmail email, String displayName) {
    return Account.createAccount(
        ExternalIdentity.createIdentity("github", "42"), email, displayName);
  }
}
