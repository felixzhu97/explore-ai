package com.ai.automation.infrastructure.mail;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.domain.repository.EmailGateway;
import com.ai.automation.infrastructure.config.MailProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Documentation. */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
@ConditionalOnProperty(
    prefix = "app.mail",
    name = "provider",
    havingValue = "resend",
    matchIfMissing = true)
@EnableConfigurationProperties(MailProperties.class)
public class ResendEmailGateway implements EmailGateway {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailGateway.class);

  private final RestClient restClient;
  private final MailProperties mailProperties;

  /** Documentation. */
  @Autowired
  public ResendEmailGateway(MailProperties mailProperties) {
    String apiKey = mailProperties.getResendApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "app.mail.resend-api-key (APP_MAIL_RESEND_API_KEY) is required"
              + " when app.mail.provider=resend");
    }
    this.mailProperties = mailProperties;
    this.restClient =
        RestClient.builder()
            .baseUrl(trimTrailingSlash(mailProperties.getResendBaseUrl()))
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
  }

  /** Package-visible for unit tests with a stub RestClient. */
  ResendEmailGateway(MailProperties mailProperties, RestClient restClient) {
    this.mailProperties = mailProperties;
    this.restClient = restClient;
  }

  @Override
  public void send(EmailMessage message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("from", mailProperties.getFrom());
    body.put("to", List.of(message.to()));
    body.put("subject", message.subject());
    body.put("text", message.textBody());
    if (message.hasHtmlBody()) {
      body.put("html", message.htmlBody());
    }

    try {
      restClient
          .post()
          .uri("/emails")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();
      log.info("Sent automation email via Resend toFp={}", fingerprint(message.to()));
    } catch (RestClientResponseException e) {
      throw new IllegalStateException(
          "Resend API rejected email: HTTP "
              + e.getStatusCode().value()
              + " "
              + e.getResponseBodyAsString(),
          e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to send email via Resend", e);
    }
  }

  private static String trimTrailingSlash(String url) {
    if (url == null || url.isBlank()) {
      return "https://api.resend.com";
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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
