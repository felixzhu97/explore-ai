package com.ai.metrics.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.metrics")
public class MetricsAdminProperties {

    /**
     * When non-blank, {@code /api/metrics/**} requires matching {@code X-Admin-Key}.
     * Leave empty in local/dev so the Metrics UI works without a secret.
     */
    private String adminApiKey = "";

    public String getAdminApiKey() {
        return adminApiKey;
    }

    public void setAdminApiKey(String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    public boolean isAuthEnabled() {
        return adminApiKey != null && !adminApiKey.isBlank();
    }
}
