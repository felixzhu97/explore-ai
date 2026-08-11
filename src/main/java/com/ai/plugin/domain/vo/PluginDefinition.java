package com.ai.plugin.domain.vo;

/**
 * Catalog entry for a Plugin (MCP connection template). Preferred product term: Plugin.
 */
public record PluginDefinition(
        String id,
        String name,
        String description,
        String category,
        String iconKey,
        boolean featured,
        boolean builtin,
        String docsUrl
) {
    public PluginDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plugin definition id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plugin definition name is required");
        }
        description = description == null ? "" : description;
        category = category == null || category.isBlank() ? "developer" : category;
        iconKey = iconKey == null || iconKey.isBlank() ? id : iconKey;
        docsUrl = docsUrl == null ? "" : docsUrl;
    }

    public boolean requiresEndpoint() {
        return !builtin;
    }
}
