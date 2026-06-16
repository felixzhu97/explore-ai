package com.ai.application.tool;

import java.util.List;
import java.util.Map;

/**
 * Immutable tool definition record.
 * Protocol-agnostic representation of a tool's metadata.
 */
public final class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final boolean composite;
    private final String category;

    private ToolDefinition(String name, String description, Map<String, Object> inputSchema,
                           boolean composite, String category) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.composite = composite;
        this.category = category;
    }

    public static ToolDefinition atomic(String name, String description,
                                        Map<String, Object> inputSchema, String category) {
        return new ToolDefinition(name, description, inputSchema, false, category);
    }

    public static ToolDefinition composite(String name, String description,
                                          Map<String, Object> inputSchema, String category) {
        return new ToolDefinition(name, description, inputSchema, true, category);
    }

    public String name() { return name; }
    public String description() { return description; }
    public Map<String, Object> inputSchema() { return inputSchema; }
    public boolean composite() { return composite; }
    public String category() { return category; }

    @Override
    public String toString() {
        return "ToolDefinition{name='" + name + "', composite=" + composite + ", category='" + category + "'}";
    }
}
