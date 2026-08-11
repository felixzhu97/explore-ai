package com.ai.plugin.infrastructure.catalog;

import com.ai.plugin.domain.repository.PluginCatalogRepository;
import com.ai.plugin.domain.vo.PluginDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Repository
public class ClasspathPluginCatalogRepository implements PluginCatalogRepository {

    private static final String CATALOG_PATH = "plugin-catalog.json";

    private final List<PluginDefinition> definitions;

    public ClasspathPluginCatalogRepository(ObjectMapper objectMapper) {
        this.definitions = load(objectMapper);
    }

    @Override
    public List<PluginDefinition> findAll() {
        return definitions;
    }

    @Override
    public Optional<PluginDefinition> findById(String definitionId) {
        if (definitionId == null || definitionId.isBlank()) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.id().equals(definitionId))
                .findFirst();
    }

    private static List<PluginDefinition> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            List<PluginDefinition> loaded = objectMapper.readValue(in, new TypeReference<>() {
            });
            return List.copyOf(loaded);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load plugin catalog from " + CATALOG_PATH, ex);
        }
    }
}
