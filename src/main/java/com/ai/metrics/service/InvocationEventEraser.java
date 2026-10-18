package com.ai.metrics.service;

import com.ai.account.domain.event.GuestLinkedToAccountEvent;
import com.ai.account.domain.event.OwnerErasureRequestedEvent;
import com.ai.chat.domain.event.ChatSessionsErasedEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps AI invocation events in step with erased chat sessions and owner partitions. */
@Service
@RequiredArgsConstructor
public class InvocationEventEraser {

  private final AiInvocationEventRepository invocationEventRepository;

  @EventListener
  @Transactional
  void handleChatSessionsErasedEvent(ChatSessionsErasedEvent event) {
    invocationEventRepository.deleteBySessionIds(event.sessionIds());
  }

  @EventListener
  @Transactional
  void handleOwnerErasureRequestedEvent(OwnerErasureRequestedEvent event) {
    invocationEventRepository.deleteByOwnerKey(event.ownerKey());
  }

  @EventListener
  @Transactional
  void handleGuestLinkedToAccountEvent(GuestLinkedToAccountEvent event) {
    invocationEventRepository.reassignOwnerKey(event.fromOwnerKey(), event.toOwnerKey());
  }
}
