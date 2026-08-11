package com.ai.plugin.web.dto;

import com.ai.plugin.domain.vo.PluginDefinition;

public record PluginDefinitionResponse(
        String id,
        String name,
        String description,
        String category,
        String iconKey,
        boolean featured,
        boolean builtin,
        String docsUrl,
        boolean requiresEndpoint
) {
    public static PluginDefinitionResponse from(PluginDefinition definition) {
        return new PluginDefinitionResponse(
                definition.id(),
                definition.name(),
                definition.description(),
                definition.category(),
                definition.iconKey(),
                definition.featured(),
                definition.builtin(),
                definition.docsUrl(),
                definition.requiresEndpoint());
    }
}
