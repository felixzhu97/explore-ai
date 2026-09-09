package com.ai.account.service;

import com.ai.common.domain.vo.OwnerKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves the data partition for the current visitor (guest client or signed-in account). */
public interface CurrentOwnerResolver {

  /** Resolves owner from guest Client Identity and/or OAuth / IAM JWT authentication. */
  OwnerKey resolve(String clientId, Authentication authentication);

  /**
   * Ensures an account exists for an IAM access token and returns its owner key.
   *
   * @param jwt validated IAM JWT
   * @return account owner key
   */
  OwnerKey resolveFromJwt(Jwt jwt);
}
