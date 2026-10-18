package com.ai.account.domain.model;

import com.ai.common.domain.model.AbstractEntity;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

/** Signed-in OAuth / IAM identity, optionally linked to one browser Client Identity. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Account extends AbstractEntity<AccountId> {

  static final int MAX_DISPLAY_NAME_LENGTH = 255;
  static final int MAX_USER_NAME_LENGTH = 256;

  @NaturalId
  @NotBlank
  @Size(max = 32)
  @Column(nullable = false, length = 32)
  private String provider;

  @NaturalId
  @NotBlank
  @Size(max = 255)
  @Column(nullable = false, length = 255)
  private String subject;

  @Size(max = MAX_USER_NAME_LENGTH)
  @Column(length = MAX_USER_NAME_LENGTH)
  private String userName;

  @Convert(converter = ContactEmailAttributeConverter.class)
  @Column(length = 320)
  private ContactEmail email;

  @Size(max = MAX_DISPLAY_NAME_LENGTH)
  @Column(length = MAX_DISPLAY_NAME_LENGTH)
  private String displayName;

  @Convert(converter = ClientIdAttributeConverter.class)
  @Column(length = 64)
  private ClientId linkedClientId;

  @Column(nullable = false)
  private boolean active = true;

  private Account(AccountId id) {
    super(id);
  }

  /** Creates an account for the identity, not yet linked to a browser. */
  public static Account createAccount(
      ExternalIdentity identity, ContactEmail email, String displayName) {
    Objects.requireNonNull(identity, "identity");
    Account account = new Account(AccountId.generateId());
    account.provider = identity.getProvider();
    account.subject = identity.getSubject();
    account.email = email;
    account.displayName = normalizeDisplayName(displayName);
    return account;
  }

  /**
   * Creates an account that Explore IAM provisioned over SCIM before the person signs in. The
   * identity is the one the person will sign in with, so the first sign-in reuses this account.
   */
  public static Account createProvisionedAccount(
      ExternalIdentity identity, String userName, ContactEmail email, String displayName) {
    Account account = createAccount(identity, email, displayName);
    account.userName = requireUserName(userName);
    return account;
  }

  /** Returns the provider and subject this account signs in with. */
  public ExternalIdentity getIdentity() {
    return ExternalIdentity.createIdentity(provider, subject);
  }

  /** Links this account to the browser and refreshes the profile from the provider. */
  public void linkBrowser(ClientId clientId, ContactEmail email, String displayName) {
    requireActive();
    this.linkedClientId = Objects.requireNonNull(clientId, "clientId");
    applyProfile(email, displayName);
  }

  /** Refreshes the profile on a sign-in that does not involve a browser, such as an IAM token. */
  public void recordSignIn(ContactEmail email, String displayName) {
    requireActive();
    applyProfile(email, displayName);
  }

  /**
   * Replaces the provisioned profile with the values Explore IAM sent; values left out are cleared,
   * as SCIM replace requires.
   */
  public void replaceProfile(String userName, ContactEmail email, String displayName) {
    this.userName = requireUserName(userName);
    this.email = email;
    this.displayName = normalizeDisplayName(displayName);
  }

  /** Stops sign-in and data access; the data stays until the account is deleted. */
  public void deactivateAccount() {
    this.active = false;
    this.linkedClientId = null;
  }

  /** Allows sign-in and data access again. */
  public void reactivateAccount() {
    this.active = true;
  }

  /** Fails when the account was deactivated. */
  public void requireActive() {
    if (!active) {
      throw DomainException.createForbiddenError("ACCOUNT_INACTIVE", "Account is deactivated");
    }
  }

  /** Clears the browser link so logout returns to guest mode. */
  public void unlinkBrowser() {
    this.linkedClientId = null;
  }

  /** Returns the data partition of this account. */
  public OwnerKey createOwnerKey() {
    return OwnerKey.createAccountKey(getId().toString());
  }

  /** Returns the guest partition of the linked browser, when one is linked. */
  public Optional<OwnerKey> findGuestOwnerKey() {
    return Optional.ofNullable(linkedClientId).map(id -> OwnerKey.createClientKey(id.getValue()));
  }

  /** Returns the name to show for this account: display name first, then email. */
  public Optional<String> findDisplayLabel() {
    if (displayName != null) {
      return Optional.of(displayName);
    }
    return Optional.ofNullable(email).map(ContactEmail::getValue);
  }

  /** Keeps stored values the provider did not send, since tokens may omit profile claims. */
  private void applyProfile(ContactEmail email, String displayName) {
    if (email != null) {
      this.email = email;
    }
    String name = normalizeDisplayName(displayName);
    if (name != null) {
      this.displayName = name;
    }
  }

  private static String requireUserName(String userName) {
    if (userName == null || userName.isBlank()) {
      throw DomainException.createInvalidError("USER_NAME_REQUIRED", "userName is required");
    }
    String trimmed = userName.trim();
    if (trimmed.length() > MAX_USER_NAME_LENGTH) {
      throw DomainException.createInvalidError("USER_NAME_TOO_LONG", "userName is too long");
    }
    return trimmed;
  }

  private static String normalizeDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return null;
    }
    String trimmed = displayName.trim();
    if (trimmed.length() <= MAX_DISPLAY_NAME_LENGTH) {
      return trimmed;
    }
    int end = MAX_DISPLAY_NAME_LENGTH;
    if (Character.isHighSurrogate(trimmed.charAt(end - 1))) {
      end--;
    }
    return trimmed.substring(0, end);
  }

  @Override
  public String toString() {
    return "Account{id=%s, provider=%s}".formatted(getId(), provider);
  }
}
