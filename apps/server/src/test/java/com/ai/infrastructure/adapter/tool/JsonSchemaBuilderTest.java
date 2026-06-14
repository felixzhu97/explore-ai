package com.ai.infrastructure.adapter.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsonSchemaBuilder")
class JsonSchemaBuilderTest {

    @Nested
    @DisplayName("objectSchema")
    class ObjectSchema {

        @Test
        @DisplayName("should create object schema with type and properties")
        void shouldCreateObjectSchemaWithTypeAndProperties() {
            Map<String, Object> props = Map.of("name", Map.of("type", "string"));
            Map<String, Object> schema = JsonSchemaBuilder.objectSchema(List.of("name"), props);

            assertThat(schema).containsEntry("type", "object");
            assertThat(schema).containsKey("properties");
            assertThat(schema.get("properties")).isEqualTo(props);
        }

        @Test
        @DisplayName("should include required fields when provided")
        void shouldIncludeRequiredFieldsWhenProvided() {
            Map<String, Object> props = Map.of(
                "query", Map.of("type", "string"),
                "topK", Map.of("type", "integer")
            );
            Map<String, Object> schema = JsonSchemaBuilder.objectSchema(List.of("query", "topK"), props);

            assertThat(schema).containsEntry("required", List.of("query", "topK"));
        }

        @Test
        @DisplayName("should not include required when empty")
        void shouldNotIncludeRequiredWhenEmpty() {
            Map<String, Object> schema = JsonSchemaBuilder.objectSchema(List.of(), Map.of());

            assertThat(schema).doesNotContainKey("required");
        }
    }

    @Nested
    @DisplayName("stringProp")
    class StringProp {

        @Test
        @DisplayName("should create string property with type and description")
        void shouldCreateStringProperty() {
            var entry = JsonSchemaBuilder.stringProp("query", "Search query", true);

            assertThat(entry.getKey()).isEqualTo("query");
            assertThat(entry.getValue()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> prop = (Map<String, Object>) entry.getValue();
            assertThat(prop).containsEntry("type", "string");
            assertThat(prop).containsEntry("description", "Search query");
        }
    }

    @Nested
    @DisplayName("integerProp")
    class IntegerProp {

        @Test
        @DisplayName("should create integer property")
        void shouldCreateIntegerProperty() {
            var entry = JsonSchemaBuilder.integerProp("topK", "Number of results", false);

            assertThat(entry.getKey()).isEqualTo("topK");
            @SuppressWarnings("unchecked")
            Map<String, Object> prop = (Map<String, Object>) entry.getValue();
            assertThat(prop).containsEntry("type", "integer");
        }
    }

    @Nested
    @DisplayName("arrayProp")
    class ArrayProp {

        @Test
        @DisplayName("should create array property with items")
        void shouldCreateArrayProperty() {
            var entry = JsonSchemaBuilder.arrayProp("ids", "List of IDs", "string", true);

            assertThat(entry.getKey()).isEqualTo("ids");
            @SuppressWarnings("unchecked")
            Map<String, Object> prop = (Map<String, Object>) entry.getValue();
            assertThat(prop).containsEntry("type", "array");
            @SuppressWarnings("unchecked")
            Map<String, Object> items = (Map<String, Object>) prop.get("items");
            assertThat(items).containsEntry("type", "string");
        }
    }

    @Nested
    @DisplayName("toProperties")
    class ToProperties {

        @Test
        @DisplayName("should combine entries into properties map")
        void shouldCombineEntriesIntoPropertiesMap() {
            var props = JsonSchemaBuilder.toProperties(
                JsonSchemaBuilder.stringProp("name", "Name", true),
                JsonSchemaBuilder.integerProp("age", "Age", false)
            );

            assertThat(props).containsKey("name");
            assertThat(props).containsKey("age");
            assertThat(props).hasSize(2);
        }
    }

    @Nested
    @DisplayName("emptySchema")
    class EmptySchema {

        @Test
        @DisplayName("should create empty object schema")
        void shouldCreateEmptyObjectSchema() {
            Map<String, Object> schema = JsonSchemaBuilder.emptySchema();

            assertThat(schema).containsEntry("type", "object");
            assertThat(schema.get("properties")).isInstanceOf(Map.class);
            assertThat(((Map<?, ?>) schema.get("properties"))).isEmpty();
        }
    }
}
