package com.ai.tools.domain.vo;

public record ToolCatalogEntry(String name, String description, ToolSource source) {

    public ToolCatalogEntry {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must not be blank");
        }
        if (description == null) {
            description = "";
        }
        if (source == null) {
            throw new IllegalArgumentException("Tool source must not be null");
        }
    }

    public static ToolCatalogEntry of(String name, String description, ToolSource source) {
        return new ToolCatalogEntry(name, description, source);
    }
}
