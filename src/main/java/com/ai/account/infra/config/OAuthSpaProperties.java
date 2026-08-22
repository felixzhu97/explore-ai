package com.ai.account.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared SPA return URL after any OAuth provider finishes (success or failure).
 *
 * <p>Bound from {@code APP_OAUTH_SUCCESS_REDIRECT}.
 */
@ConfigurationProperties(prefix = "app.oauth")
public class OAuthSpaProperties {

  /** Where to send the browser after OAuth (SPA origin; query {@code login=} is appended). */
  private String successRedirectUrl = "/";

  public String getSuccessRedirectUrl() {
    return successRedirectUrl;
  }

  public void setSuccessRedirectUrl(String successRedirectUrl) {
    this.successRedirectUrl = successRedirectUrl;
  }
}
