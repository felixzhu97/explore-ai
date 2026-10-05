package com.ai.account.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared SPA return URL after any OAuth provider finishes (success or failure).
 *
 * <p>Bound from {@code APP_OAUTH_SUCCESS_REDIRECT}.
 */
@ConfigurationProperties(prefix = "app.oauth")
@Getter
@Setter
public class OAuthSpaProperties {

  /** Where to send the browser after OAuth (SPA origin; query {@code login=} is appended). */
  private String successRedirectUrl = "/";
}
