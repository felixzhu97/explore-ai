package com.ai.account.domain.model;

import com.ai.common.domain.model.AbstractEntity;
import com.ai.common.domain.model.OwnerKey;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
public class AccountUser extends AbstractEntity<AccountUserId> {

  static final int MAX_DISPLAY_NAME_LENGTH = 255;

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

  @Convert(converter = ContactEmailAttributeConverter.class)
  @Column(length = 320)
  private ContactEmail email;

  @Size(max = MAX_DISPLAY_NAME_LENGTH)
  @Column(length = MAX_DISPLAY_NAME_LENGTH)
  private String displayName;

  @Convert(converter = ClientIdAttributeConverter.class)
  @Column(length = 64)
  private ClientId linkedClientId;

  /** Creates an account for the identity, not yet linked to a browser. */
  public static AccountUser create(
      ExternalIdentity identity, ContactEmail email, String displayName) {
    Objects.requireNonNull(identity, "identity");
    AccountUser user = new AccountUser();
    user.id = AccountUserId.generate();
    user.provider = identity.provider();
    user.subject = identity.subject();
    user.email = email;
    user.displayName = normalizeDisplayName(displayName);
    Instant now = Instant.now();
    user.createdAt = now;
    user.updatedAt = now;
    return user;
  }

  /** Returns the provider and subject this account signs in with. */
  public ExternalIdentity identity() {
    return ExternalIdentity.of(provider, subject);
  }

  /** Links this account to the browser and refreshes the profile from the provider. */
  public void linkBrowser(ClientId clientId, ContactEmail email, String displayName) {
    this.linkedClientId = Objects.requireNonNull(clientId, "clientId");
    applyProfile(email, displayName);
    touchUpdatedAt();
  }

  /** Refreshes the profile on a sign-in that does not involve a browser, such as an IAM token. */
  public void recordSignIn(ContactEmail email, String displayName) {
    if (applyProfile(email, displayName)) {
      touchUpdatedAt();
    }
  }

  /** Clears the browser link so logout returns to guest mode. */
  public void unlinkBrowser() {
    this.linkedClientId = null;
    touchUpdatedAt();
  }

  /** Returns the data partition of this account. */
  public OwnerKey ownerKey() {
    return OwnerKey.forAccount(getId().value());
  }

  /** Returns the guest partition of the linked browser, when one is linked. */
  public Optional<OwnerKey> guestOwnerKey() {
    return Optional.ofNullable(linkedClientId).map(id -> OwnerKey.forClient(id.value()));
  }

  /** Returns the name to show for this account: display name first, then email. */
  public Optional<String> displayLabel() {
    if (displayName != null) {
      return Optional.of(displayName);
    }
    return Optional.ofNullable(email).map(ContactEmail::value);
  }

  /** Keeps stored values the provider did not send, since tokens may omit profile claims. */
  private boolean applyProfile(ContactEmail email, String displayName) {
    String name = normalizeDisplayName(displayName);
    boolean changed = false;
    if (email != null && !email.equals(this.email)) {
      this.email = email;
      changed = true;
    }
    if (name != null && !name.equals(this.displayName)) {
      this.displayName = name;
      changed = true;
    }
    return changed;
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
    return "AccountUser{id=%s, provider=%s}".formatted(getId(), provider);
  }
}
