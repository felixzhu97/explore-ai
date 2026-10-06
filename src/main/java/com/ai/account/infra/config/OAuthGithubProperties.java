package com.ai.account.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.oauth.github} for GitHub OAuth login. */
@ConfigurationProperties(prefix = "app.oauth.github")
@Getter
@Setter
public class OAuthGithubProperties {

  /** When true and credentials are set, GitHub OAuth login is offered. */
  private boolean enabled = false;

  private String clientId = "";

  private String clientSecret = "";

  /**
   * Absolute OAuth callback URL registered in the GitHub OAuth App. When blank, Spring uses {@code
   * {baseUrl}/login/oauth2/code/{registrationId}}.
   */
  private String redirectUri = "";

  /** Tells whether GitHub login is enabled and fully configured. */
  public boolean isReady() {
    return enabled
        && clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }
}
