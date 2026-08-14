package com.ai.account.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.oauth.google")
public class OAuthGoogleProperties {

  /** When true and credentials are set, Google OAuth login is offered. */
  private boolean enabled = false;

  private String clientId = "";

  private String clientSecret = "";

  /**
   * Absolute OAuth callback URL registered in Google Cloud Console. When blank, Spring uses {@code
   * {baseUrl}/login/oauth2/code/{registrationId}}.
   */
  private String redirectUri = "";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public boolean isReady() {
    return enabled
        && clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }
}
