package com.ai.infrastructure.adapter.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper for building JSON Schema objects.
 */
public final class JsonSchemaBuilder {

    private JsonSchemaBuilder() {}

    public static Map<String, Object> objectSchema(List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    public static Map.Entry<String, Object> stringProp(String name, String description, boolean required) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        return Map.entry(name, prop);
    }

    public static Map.Entry<String, Object> integerProp(String name, String description, boolean required) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", "integer");
        prop.put("description", description);
        return Map.entry(name, prop);
    }

    public static Map.Entry<String, Object> arrayProp(String name, String description, String itemType, boolean required) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", "array");
        prop.put("description", description);
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("type", itemType);
        prop.put("items", items);
        return Map.entry(name, prop);
    }

    public static Map<String, Object> toProperties(Map.Entry<String, Object>... entries) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : entries) {
            props.put(e.getKey(), e.getValue());
        }
        return props;
    }

    public static Map<String, Object> emptySchema() {
        return objectSchema(List.of(), Map.of());
    }
}
