package com.ai.account.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.oauth.google} for Google OAuth login. */
@ConfigurationProperties(prefix = "app.oauth.google")
@Getter
@Setter
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

  /** Tells whether Google login is enabled and fully configured. */
  public boolean isReady() {
    return enabled
        && clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }
}
