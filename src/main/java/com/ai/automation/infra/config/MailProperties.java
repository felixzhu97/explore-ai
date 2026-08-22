package com.ai.automation.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

  /** When false, emails are logged only ({@code LoggingEmailGateway}). */
  private boolean enabled = false;

  /** Delivery backend when {@link #enabled} is true: {@code resend} (HTTP API) or {@code smtp}. */
  private String provider = "resend";

  private String from = "onboarding@resend.dev";

  /** Resend API key (Bearer). Required when provider is {@code resend}. */
  private String resendApiKey = "";

  /** Override for tests; production default is {@code https://api.resend.com}. */
  private String resendBaseUrl = "https://api.resend.com";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getResendApiKey() {
    return resendApiKey;
  }

  public void setResendApiKey(String resendApiKey) {
    this.resendApiKey = resendApiKey;
  }

  public String getResendBaseUrl() {
    return resendBaseUrl;
  }

  public void setResendBaseUrl(String resendBaseUrl) {
    this.resendBaseUrl = resendBaseUrl;
  }
}
