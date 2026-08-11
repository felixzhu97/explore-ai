package com.ai.plugin.domain.vo;

import java.util.UUID;

public record PluginInstallationId(String value) {

    public PluginInstallationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PluginInstallationId cannot be null or blank");
        }
    }

    public static PluginInstallationId of(String value) {
        return new PluginInstallationId(value);
    }

    public static PluginInstallationId generate() {
        return new PluginInstallationId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
