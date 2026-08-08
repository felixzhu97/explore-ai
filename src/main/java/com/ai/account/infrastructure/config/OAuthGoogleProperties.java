package com.ai.account.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth.google")
public class OAuthGoogleProperties {

    /** When true and credentials are set, Google OAuth login is offered. */
    private boolean enabled = false;

    private String clientId = "";
    private String clientSecret = "";

    /** Where to send the browser after OAuth (SPA origin; query {@code login=} is appended). */
    private String successRedirectUrl = "http://127.0.0.1:4200/";

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

    public String getSuccessRedirectUrl() {
        return successRedirectUrl;
    }

    public void setSuccessRedirectUrl(String successRedirectUrl) {
        this.successRedirectUrl = successRedirectUrl;
    }
}
