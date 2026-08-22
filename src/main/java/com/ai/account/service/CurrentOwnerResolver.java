package com.ai.account.service;

import com.ai.common.domain.vo.OwnerKey;
import org.springframework.security.core.Authentication;

/** Resolves the data partition for the current visitor (guest client or signed-in account). */
public interface CurrentOwnerResolver {
  /** Documentation. */
  OwnerKey resolve(String clientId, Authentication authentication);
}
