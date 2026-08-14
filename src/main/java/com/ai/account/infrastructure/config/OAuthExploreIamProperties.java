package com.ai.account.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.oauth.explore-iam")
public class OAuthExploreIamProperties {

  /** When true and credentials + issuer are set, Explore IAM OIDC login is offered. */
  private boolean enabled = false;

  private String clientId = "";

  private String clientSecret = "";

  /** Issuer URL of Explore IAM (e.g. http://localhost:9100). */
  private String issuerUri = "";

  /**
   * Absolute OAuth callback URL. When blank, Spring uses {@code
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

  public String getIssuerUri() {
    return issuerUri;
  }

  public void setIssuerUri(String issuerUri) {
    this.issuerUri = issuerUri;
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
        && !clientSecret.isBlank()
        && issuerUri != null
        && !issuerUri.isBlank();
  }
}
