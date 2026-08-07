package com.ai.automation.infrastructure.mail;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.infrastructure.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailProperties.getFrom());
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.textBody());
        mailSender.send(mail);
        log.info("Sent automation email toFp={}", fingerprint(message.to()));
    }

    private static String fingerprint(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(email.indexOf('@') + 1);
    }
}
