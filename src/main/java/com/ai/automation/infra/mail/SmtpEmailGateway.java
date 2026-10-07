package com.ai.automation.infra.mail;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.infra.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Email gateway that sends automation emails over SMTP, as multipart when HTML is present. */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "smtp")
@EnableConfigurationProperties(MailProperties.class)
@RequiredArgsConstructor
public class SmtpEmailGateway implements EmailGateway {

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;

  @Override
  public void send(EmailMessage message) {
    if (message.hasHtmlBody()) {
      sendMultipart(message);
    } else {
      SimpleMailMessage mail = new SimpleMailMessage();
      mail.setFrom(mailProperties.getFrom());
      mail.setTo(message.to());
      mail.setSubject(message.subject());
      mail.setText(message.textBody());
      mailSender.send(mail);
    }
  }

  private void sendMultipart(EmailMessage message) {
    try {
      MimeMessage mime = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
      helper.setFrom(mailProperties.getFrom());
      helper.setTo(message.to());
      helper.setSubject(message.subject());
      helper.setText(message.textBody(), message.htmlBody());
      mailSender.send(mime);
    } catch (MessagingException e) {
      throw new IllegalStateException("Failed to send multipart email via SMTP", e);
    }
  }
}
