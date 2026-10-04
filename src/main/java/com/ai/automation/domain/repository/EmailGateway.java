package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.EmailMessage;

/** Outbound gateway that delivers automation result emails. */
public interface EmailGateway {
  void send(EmailMessage message);
}
