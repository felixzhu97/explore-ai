package com.ai.account.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for Explore IAM OIDC login and JWT resource-server validation. */
@ConfigurationProperties(prefix = "app.oauth.explore-iam")
@Getter
@Setter
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

  /** When true and issuer-uri is set, accept IAM JWT Bearer tokens. */
  private boolean resourceServerEnabled = true;

  /** Tells whether Explore IAM login is enabled and fully configured. */
  public boolean isReady() {
    return enabled
        && clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank()
        && issuerUri != null
        && !issuerUri.isBlank();
  }

  /** Resource server needs only a non-blank issuer. */
  public boolean isResourceServerReady() {
    return resourceServerEnabled && issuerUri != null && !issuerUri.isBlank();
  }
}
