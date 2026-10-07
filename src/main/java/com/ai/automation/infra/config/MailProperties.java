package com.ai.automation.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mail delivery settings bound from {@code app.mail}: toggle, provider, sender and Resend API. */
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

  /** When false, emails are dropped ({@code DisabledEmailGateway}). */
  private boolean enabled = false;

  /** Delivery backend when {@link #enabled} is true: {@code resend} (HTTP API) or {@code smtp}. */
  private String provider = "resend";

  private String from = "onboarding@resend.dev";

  /** Resend API key (Bearer). Required when provider is {@code resend}. */
  private String resendApiKey = "";

  /** Override for tests; production default is {@code https://api.resend.com}. */
  private String resendBaseUrl = "https://api.resend.com";
}
