package com.ai.automation.infrastructure.mail;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.infrastructure.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Documentation. */
@Component
@ConditionalOnProperty(
    prefix = "app.mail",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
@EnableConfigurationProperties(MailProperties.class)
public class LoggingEmailGateway implements EmailGateway {

  private static final Logger log = LoggerFactory.getLogger(LoggingEmailGateway.class);

  @Override
  public void send(EmailMessage message) {
    log.info(
        "Mail disabled — would send subject='{}' toFp={} bodyChars={}",
        message.subject(),
        fingerprint(message.to()),
        message.textBody() == null ? 0 : message.textBody().length());
  }

  private static String fingerprint(String email) {
    if (email == null || email.length() < 3) {
      return "***";
    }
    int at = email.indexOf('@');
    if (at < 1) {
      return "***";
    }
    return email.charAt(0) + "***@" + email.substring(at + 1);
  }
}
