package com.ai.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.billing")
public class BillingProperties {

    private boolean quotaEnabled = true;
    /** free | pro */
    private String plan = "free";
    private int freeDailyRequests = 50;
    private int proDailyRequests = 2000;

    public boolean isQuotaEnabled() {
        return quotaEnabled;
    }

    public void setQuotaEnabled(boolean quotaEnabled) {
        this.quotaEnabled = quotaEnabled;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public int getFreeDailyRequests() {
        return freeDailyRequests;
    }

    public void setFreeDailyRequests(int freeDailyRequests) {
        this.freeDailyRequests = freeDailyRequests;
    }

    public int getProDailyRequests() {
        return proDailyRequests;
    }

    public void setProDailyRequests(int proDailyRequests) {
        this.proDailyRequests = proDailyRequests;
    }

    public int dailyLimit() {
        if ("pro".equalsIgnoreCase(plan)) {
            return proDailyRequests;
        }
        return freeDailyRequests;
    }
}
