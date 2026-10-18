package com.ai.account.domain.event;

import com.ai.common.domain.model.OwnerKey;

/**
 * Guest linked to account event.
 *
 * @param fromOwnerKey guest owner partition of the browser
 * @param toOwnerKey account owner partition that now owns the guest rows
 */
public record GuestLinkedToAccountEvent(OwnerKey fromOwnerKey, OwnerKey toOwnerKey) {}
