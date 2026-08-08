package com.ai.account.application.usecase;

import com.ai.common.domain.vo.OwnerKey;

/**
 * Erases durable data for a data partition (guest client or signed-in account).
 */
public interface OwnerEraseUseCase {

    void eraseAllForOwner(OwnerKey ownerKey);
}
