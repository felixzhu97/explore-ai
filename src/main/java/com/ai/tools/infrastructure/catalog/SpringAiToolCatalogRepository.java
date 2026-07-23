package com.ai.tools.infrastructure.catalog;

import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.tools.domain.repository.ToolCatalogRepository;
import com.ai.tools.domain.vo.ToolCatalogEntry;
import com.ai.tools.domain.vo.ToolSource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lists the same ToolCallbacks Chat uses for tool-calling (local @Tool + MCP).
 */
@Repository
public class SpringAiToolCatalogRepository implements ToolCatalogRepository {

    private final ToolCallback[] localToolCallbacks;
    private final ObjectProvider<ToolCallback[]> mcpToolCallbacks;

    public SpringAiToolCatalogRepository(
            WeatherTool weatherTools,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            ObjectProvider<ToolCallback[]> mcpToolCallbacks) {
        this.localToolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools, documentSearchTool, webSearchTool)
                .build()
                .getToolCallbacks();
        this.mcpToolCallbacks = mcpToolCallbacks;
    }

    @Override
    public List<ToolCatalogEntry> listCatalog() {
        List<ToolCatalogEntry> entries = new ArrayList<>();
        Set<String> names = new HashSet<>();

        for (ToolCallback callback : localToolCallbacks) {
            var def = callback.getToolDefinition();
            if (names.add(def.name())) {
                entries.add(ToolCatalogEntry.of(def.name(), def.description(), ToolSource.LOCAL));
            }
        }

        ToolCallback[] mcp = mcpToolCallbacks.getIfAvailable();
        if (mcp != null) {
            for (ToolCallback callback : mcp) {
                var def = callback.getToolDefinition();
                if (names.add(def.name())) {
                    entries.add(ToolCatalogEntry.of(def.name(), def.description(), ToolSource.MCP));
                }
            }
        }

        return List.copyOf(entries);
    }
}
