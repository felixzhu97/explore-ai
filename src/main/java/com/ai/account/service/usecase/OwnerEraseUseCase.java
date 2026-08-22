package com.ai.account.service.usecase;

import com.ai.common.domain.vo.OwnerKey;

/** Erases durable data for a data partition (guest client or signed-in account). */
public interface OwnerEraseUseCase {
  /** Documentation. */
  void eraseAllForOwner(OwnerKey ownerKey);
}
