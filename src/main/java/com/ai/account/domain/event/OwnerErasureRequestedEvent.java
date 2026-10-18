package com.ai.account.domain.event;

import com.ai.common.domain.model.OwnerKey;

/**
 * Owner erasure requested event.
 *
 * @param ownerKey owner partition whose rows must be deleted
 */
public record OwnerErasureRequestedEvent(OwnerKey ownerKey) {}
