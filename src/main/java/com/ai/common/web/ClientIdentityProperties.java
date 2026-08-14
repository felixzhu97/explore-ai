package com.ai.common.web;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.client-identity")
public class ClientIdentityProperties {

  /** Cookie name. Prefer {@code __Host-ea_cid} when Secure is enabled (HTTPS). */
  private String cookieName = "ea_cid";

  private boolean secure = false;

  private String sameSite = "Lax";

  private Duration maxAge = Duration.ofDays(365);

  public String getCookieName() {
    return cookieName;
  }

  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public String getSameSite() {
    return sameSite;
  }

  public void setSameSite(String sameSite) {
    this.sameSite = sameSite;
  }

  public Duration getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }
}
