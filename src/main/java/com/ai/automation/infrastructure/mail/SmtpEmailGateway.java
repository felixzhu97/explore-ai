package com.ai.automation.infrastructure.mail;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.infrastructure.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "smtp")
@EnableConfigurationProperties(MailProperties.class)
public class SmtpEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailGateway.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public SmtpEmailGateway(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

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
        log.info("Sent automation email toFp={}", fingerprint(message.to()));
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

    private static String fingerprint(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(email.indexOf('@') + 1);
    }
}
