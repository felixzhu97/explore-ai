package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.EmailMessage;

/** Documentation. */
public interface EmailGateway {
  /** Documentation. */
  void send(EmailMessage message);
}
