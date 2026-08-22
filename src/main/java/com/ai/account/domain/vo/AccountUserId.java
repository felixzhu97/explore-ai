package com.ai.account.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for AccountUser. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class AccountUserId extends AbstractUuidId {

  /** Documentation. */
  public AccountUserId(String value) {
    super(value);
  }

  /** Documentation. */
  public static AccountUserId of(String value) {
    return new AccountUserId(value);
  }

  /** Documentation. */
  public static AccountUserId generate() {
    return new AccountUserId(newUuidString());
  }
}
