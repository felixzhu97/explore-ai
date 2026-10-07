package com.ai.automation.infra.mail;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.infra.config.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Fallback email gateway that drops every message while mail delivery is disabled. */
@Component
@ConditionalOnProperty(
    prefix = "app.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
@EnableConfigurationProperties(MailProperties.class)
public class DisabledEmailGateway implements EmailGateway {

  @Override
  public void send(EmailMessage message) {}
}
