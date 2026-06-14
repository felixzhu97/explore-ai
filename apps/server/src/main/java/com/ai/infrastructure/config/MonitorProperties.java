package com.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * System monitoring configuration properties.
 * Binds configuration from application.yml under 'monitor' prefix.
 */
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    private int cpuSampleSeconds = 1;
    private long jvmRefreshMs = 5000;

    public int getCpuSampleSeconds() {
        return cpuSampleSeconds;
    }

    public void setCpuSampleSeconds(int cpuSampleSeconds) {
        this.cpuSampleSeconds = cpuSampleSeconds;
    }

    public long getJvmRefreshMs() {
        return jvmRefreshMs;
    }

    public void setJvmRefreshMs(long jvmRefreshMs) {
        this.jvmRefreshMs = jvmRefreshMs;
    }
}
