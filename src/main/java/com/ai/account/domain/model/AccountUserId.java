package com.ai.account.domain.model;

import com.ai.common.domain.model.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for AccountUser. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class AccountUserId extends AbstractUuidId {

  public AccountUserId(String value) {
    super(value);
  }

  /** Wraps an existing account id. */
  public static AccountUserId of(String value) {
    return new AccountUserId(value);
  }

  /** Creates a new random account id. */
  public static AccountUserId generate() {
    return new AccountUserId(generateUuidString());
  }
}
