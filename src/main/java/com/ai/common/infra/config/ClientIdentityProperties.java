package com.ai.common.infra.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Client identity cookie settings: name, Secure flag, SameSite policy, and max age. */
@ConfigurationProperties(prefix = "app.client-identity")
@Getter
@Setter
public class ClientIdentityProperties {

  /** Cookie name. Prefer {@code __Host-ea_cid} when Secure is enabled (HTTPS). */
  private String cookieName = "ea_cid";

  private boolean secure = false;

  private String sameSite = "Lax";

  private Duration maxAge = Duration.ofDays(365);
}
