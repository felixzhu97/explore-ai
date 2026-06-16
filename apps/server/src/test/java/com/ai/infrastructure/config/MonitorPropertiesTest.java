package com.ai.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MonitorProperties configuration binding.
 */
@DisplayName("MonitorProperties")
class MonitorPropertiesTest {

    @Test
    @DisplayName("should have default values")
    void shouldHaveDefaultValues() {
        MonitorProperties props = new MonitorProperties();
        assertThat(props.getCpuSampleSeconds()).isEqualTo(1);
        assertThat(props.getJvmRefreshMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("should set and get cpu-sample-seconds")
    void shouldSetAndGetCpuSampleSeconds() {
        MonitorProperties props = new MonitorProperties();
        props.setCpuSampleSeconds(2);
        assertThat(props.getCpuSampleSeconds()).isEqualTo(2);
    }

    @Test
    @DisplayName("should set and get jvm-refresh-ms")
    void shouldSetAndGetJvmRefreshMs() {
        MonitorProperties props = new MonitorProperties();
        props.setJvmRefreshMs(10000);
        assertThat(props.getJvmRefreshMs()).isEqualTo(10000);
    }
}
