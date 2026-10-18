package com.ai.account.service;

import com.ai.account.domain.event.OwnerErasureRequestedEvent;
import com.ai.common.domain.model.OwnerKey;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erases all data stored under an owner key in a single transaction; each module deletes its own
 * rows when handling {@link OwnerErasureRequestedEvent}.
 */
@Service
@RequiredArgsConstructor
public class OwnerErasureService {

  private final ApplicationEventPublisher eventPublisher;

  /** Erases all data stored for the owner. */
  @Transactional
  public void eraseAllForOwner(OwnerKey ownerKey) {
    Objects.requireNonNull(ownerKey, "ownerKey");
    eventPublisher.publishEvent(new OwnerErasureRequestedEvent(ownerKey));
  }
}
